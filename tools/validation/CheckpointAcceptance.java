package validation;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.operation.OperationPlan;
import net.nicomar.nomoregrids.persistence.SectionStore;
import net.nicomar.nomoregrids.platform.*;

/** Drives real maintenance across ticks; moves keep committing after the worker has captured its prefix. */
final class CheckpointAcceptance {
    private static final Map<ServerLevel,Run> ACTIVE=new IdentityHashMap<>();
    private static final class Run {
        final WorldService service;final PieceRecord initial;final UUID firstOperation;
        int moves,ticks;boolean observedWorker,tailWritten;
        Run(WorldService service,PieceRecord piece,UUID operation){this.service=service;initial=piece;firstOperation=operation;}
    }
    static void register(){
        NeoForge.EVENT_BUS.addListener(CheckpointAcceptance::commands);
        NeoForge.EVENT_BUS.addListener(CheckpointAcceptance::tick);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent e)->{for(var row:List.copyOf(ACTIVE.entrySet()))if(row.getKey().getServer()==e.getServer())finish(row.getKey(),row.getValue(),new IllegalStateException("Stopped before checkpoint case completed"));});
    }
    private static void commands(RegisterCommandsEvent event){event.getDispatcher().register(Commands.literal("nmg_checkpoint_test").requires(s->s.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_OWNER)).then(Commands.argument("anchor",BlockPosArgument.blockPos()).executes(ctx->{
        try{var level=ctx.getSource().getLevel();ServerBenchmarks.checkProfile(level);var service=NmgRuntime.require(level);if(ACTIVE.containsKey(level)||!service.snapshot().records().isEmpty()||!service.effects().isEmpty()||service.journalBytes()!=0)throw new IllegalStateException("Needs a fresh validation authority");var at=BlockPosArgument.getLoadedBlockPos(ctx,"anchor");var p=NativeStates.capture(Blocks.STONE.defaultBlockState(),level,at,UUID.randomUUID());p=BenchmarkScenes.move(p,p.origin16().add(new GridPoint(8,0,0)),0);
            for(var c:p.worldOccupation().cells(8)){BenchmarkScenes.requirePosition(level,WorldService.pos(c));if(!level.getBlockState(WorldService.pos(c)).isAir())throw new IllegalStateException("Loaded clear air required");}
            var operation=UUID.randomUUID();service.executePrepared(new OperationPlan(operation,OperationPlan.WORLD_ACTOR,OperationPlan.Kind.SYSTEM,List.of(new OperationPlan.Change(p.pieceId(),Optional.empty(),Optional.of(p))),BinaryData.EMPTY,BinaryData.EMPTY));ACTIVE.put(level,new Run(service,p,operation));ctx.getSource().sendSuccess(()->Component.literal("D08 queued: actual moves, checkpoint worker and concurrent tail; up to 600 ticks"),false);return 1;
        }catch(Exception ex){ctx.getSource().sendFailure(Component.literal(ex.toString()));return 0;}
    })));}
    private static void tick(ServerTickEvent.Post event){for(var row:List.copyOf(ACTIVE.entrySet()))if(row.getKey().getServer()==event.getServer()){
        var level=row.getKey();var run=row.getValue();try{
            if(++run.ticks>600||run.service.quarantined())throw new IllegalStateException("Checkpoint timeout/quarantine");
            boolean running=run.service.compactionStatus().running();if(running)run.observedWorker=true;
            if(run.moves<272){
                for(int n=0;n<16&&run.moves<272;n++){
                    var before=run.service.snapshot().records().get(run.initial.pieceId());var next=BenchmarkScenes.move(before,run.initial.origin16().add(new GridPoint((run.moves&1)==0?1:0,0,0)),before.revision()+1);
                    run.service.executeWorld(List.of(new OperationPlan.Change(before.pieceId(),Optional.of(before),Optional.of(next))),List.of());run.moves++;
                    if(running)run.tailWritten=true;
                }
                continue;
            }
            if(!run.observedWorker||!run.tailWritten)throw new IllegalStateException("Did not append a real movement while checkpoint was pending");
            if(running)continue;
            if(!run.service.hasOperation(run.firstOperation,OperationPlan.WORLD_ACTOR))throw new IllegalStateException("Archived deduplication stamp missing");
            var p=run.service.snapshot().records().get(run.initial.pieceId());if(p==null||p.revision()!=272)throw new IllegalStateException("Canonical movement tail lost");
            for(var c:p.worldOccupation().cells(8))if(!(level.getBlockEntity(WorldService.pos(c)) instanceof net.nicomar.nomoregrids.world.CompositeCellEntity cell)||!cell.view().equals(run.service.cellView(c,run.service.snapshot())))throw new IllegalStateException("Replica diverged after checkpoint");
            run.service.removeWorld(Set.of(p.pieceId()),false);finish(level,run,null);
        }catch(Exception ex){finish(level,run,ex);}
    }}
    private static void finish(ServerLevel level,Run run,Exception failure){
        ACTIVE.remove(level);var json=new JsonObject();json.addProperty("schema",1);json.addProperty("kind","world-checkpoint-case");json.addProperty("case","D08");json.addProperty("passed",failure==null);json.addProperty("moves",run.moves);json.addProperty("observedWorker",run.observedWorker);json.addProperty("tailWritten",run.tailWritten);json.addProperty("error",failure==null?null:failure.toString());
        try{SectionStore.atomic(level.getServer().getWorldPath(LevelResource.ROOT).resolve("nomoregrids/acceptance/checkpoint-"+UUID.randomUUID()+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(json).getBytes(StandardCharsets.UTF_8));}catch(Exception ex){net.nicomar.nomoregrids.NoMoreGrids.LOGGER.error("Cannot persist checkpoint case result",ex);}
    }
}
