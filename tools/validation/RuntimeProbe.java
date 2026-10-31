package validation;

import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.nicomar.nomoregrids.compat.paintableblocks.PaintableBridge;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.operation.OperationPlan;
import net.nicomar.nomoregrids.platform.*;
import net.nicomar.nomoregrids.world.CompositeCellEntity;

/** Test-only mod, compiled manually outside main. Uses an actual disposable ServerLevel. */
@Mod("nmg_validation_probe")
public final class RuntimeProbe {
    public RuntimeProbe(){NeoForge.EVENT_BUS.addListener(RuntimeProbe::register);}
    private static void register(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("nmg_validation_probe")
            .requires(s->s.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_OWNER))
            .then(Commands.literal("seed").executes(ctx->{
                try{
                    var level=ctx.getSource().getLevel();var service=NmgRuntime.require(level);
                    require(service.snapshot().records().isEmpty(),"Probe needs an empty NMG authority");
                    var at=new BlockPos(40,100,84);
                    var stone=NativeStates.capture(Blocks.STONE.defaultBlockState(),level,at,UUID.randomUUID());
                    stone=at(stone,stone.origin16().add(new GridPoint(8,0,0)),0);
                    service.executeWorld(List.of(new OperationPlan.Change(stone.pieceId(),Optional.empty(),Optional.of(stone))),List.of());
                    verify(level,stone);
                    var moved=at(stone,stone.origin16().add(new GridPoint(0,16,0)),1);
                    service.executeWorld(List.of(new OperationPlan.Change(stone.pieceId(),Optional.of(stone),Optional.of(moved))),List.of());
                    verify(level,moved);
                    for(var cell:stone.worldOccupation().cells(8))require(level.getBlockState(WorldService.pos(cell)).isAir(),"Old cell survived move");
                    var bridge=PaintableBridge.available().orElseThrow(()->new IllegalStateException("PB bridge unavailable"));
                    var pb=bridge.create(level,new BlockPos(44,101,84),1);
                    pb=at(pb,pb.origin16().add(new GridPoint(8,0,0)),0);
                    service.executeWorld(List.of(new OperationPlan.Change(pb.pieceId(),Optional.empty(),Optional.of(pb))),List.of());
                    verify(level,pb);
                    String result="PROBE SEEDED stone="+moved.pieceId()+" pb="+pb.pieceId()+" cells=4 stoneRevision=1";
                    ctx.getSource().sendSuccess(()->Component.literal(result),false);return 1;
                }catch(Exception ex){throw new IllegalStateException("Runtime seed failed",ex);}
            }))
            .then(Commands.literal("unrelated_water").executes(ctx->{
                var level=ctx.getSource().getLevel();var at=new BlockPos(40,110,104);
                require(level.isLoaded(at),"Water fixture chunk unloaded");
                require(level.setBlock(at,Blocks.WATER.defaultBlockState(),3),"Could not set native water fixture");
                ctx.getSource().sendSuccess(()->Component.literal("PROBE NATIVE WATER placed away from NMG"),false);return 1;
            }))
            .then(Commands.literal("dry_verify").executes(ctx->{
                var service=NmgRuntime.existing(ctx.getSource().getLevel());
                require(service!=null&&!service.quarantined(),"Authority not available");
                require(service.fluids().snapshot().isEmpty(),"Unrelated native fluids were claimed by NMG");
                ctx.getSource().sendSuccess(()->Component.literal("PROBE DRY VERIFIED cavityEffects=0"),false);return 1;
            }))
            .then(Commands.literal("wet_check").executes(ctx->{
                try{
                    var service=NmgRuntime.require(ctx.getSource().getLevel());var cell=new CellPos(40,101,84);
                    service.fluids().incoming(cell,net.minecraft.world.level.material.Fluids.WATER.defaultFluidState());
                    var water=service.fluids().at(cell).orElseThrow();
                    require(water.amount(net.nicomar.nomoregrids.simulation.fluid.CavityCell.Kind.WATER)==2048,"Expected half a cell of water");
                    require(water.source()==net.nicomar.nomoregrids.simulation.fluid.CavityCell.Kind.EMPTY,"A partial cavity cannot be a bucket source");
                    ctx.getSource().sendSuccess(()->Component.literal("PROBE WET VERIFIED water=2048 source=EMPTY"),false);return 1;
                }catch(Exception ex){throw new IllegalStateException("Runtime water check failed",ex);}
            }))
            .then(Commands.literal("verify").executes(ctx->{
                var level=ctx.getSource().getLevel();var service=NmgRuntime.existing(level);
                require(service!=null&&!service.quarantined(),"Authority not available");
                require(service.snapshot().records().size()==2,"Expected two recovered probe pieces");
                for(var p:service.snapshot().records().values())verify(level,p);
                ctx.getSource().sendSuccess(()->Component.literal("PROBE VERIFIED pieces=2 cells=4 quarantined=false"),false);return 1;
            })));
    }
    private static PieceRecord at(PieceRecord p,GridPoint origin,long revision){
        return new PieceRecord(p.pieceId(),revision,p.adapterId(),p.adapterVersion(),p.sourceBlockId(),p.sourceState(),origin,
            p.orientation(),p.pivot16(),p.geometry(),p.geometryPayload(),p.materialPayload(),p.itemPayload(),p.groupId(),p.flags());
    }
    private static void verify(ServerLevel level,PieceRecord p){
        var service=NmgRuntime.existing(level);
        require(p.equals(service.snapshot().records().get(p.pieceId())),"Canonical record changed");
        require(p.worldOccupation().cells(8).size()==2,"Expected a piece spanning two cells");
        for(var cell:p.worldOccupation().cells(8)){
            var pos=WorldService.pos(cell);require(level.isLoaded(pos),"Probe chunk unloaded");
            require(level.getBlockEntity(pos) instanceof CompositeCellEntity,"Missing cell entity");
            var entity=(CompositeCellEntity)level.getBlockEntity(pos);
            require(entity.valid()&&entity.view().equals(service.cellView(cell,service.snapshot())),"Replica differs from authority");
        }
    }
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
}
