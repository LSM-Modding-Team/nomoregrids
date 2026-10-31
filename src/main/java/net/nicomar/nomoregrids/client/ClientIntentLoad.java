package net.nicomar.nomoregrids.client;

import com.google.gson.*;
import com.mojang.brigadier.arguments.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.nicomar.nomoregrids.core.GridPoint;
import net.nicomar.nomoregrids.network.*;
import net.nicomar.nomoregrids.persistence.SectionStore;
import net.nicomar.nomoregrids.platform.ComponentSelection;
import net.nicomar.nomoregrids.world.CompositeCellEntity;

/** Explicit loopback-only load generator using real PROBE packets, which cannot consume or mutate. */
public final class ClientIntentLoad {
    private static final class Run {
        final String label;final Object world,connection;final int ticks,perTick;
        final LinkedHashMap<UUID,Boolean> pending=new LinkedHashMap<>();
        int elapsed,sent,replies,acceptedCurrent,rejectedCurrent,acceptedStale,rejectedStale,evicted;
        Run(String label,int ticks,int perTick){this.label=label;this.ticks=ticks;this.perTick=perTick;var mc=Minecraft.getInstance();world=mc.level;connection=mc.getConnection();}
    }
    private static Run active;
    private ClientIntentLoad(){}
    public static void register(){if(!Boolean.getBoolean("nomoregrids.validation"))return;NeoForge.EVENT_BUS.addListener(ClientIntentLoad::commands);NeoForge.EVENT_BUS.addListener(ClientIntentLoad::tick);}
    private static void commands(RegisterClientCommandsEvent event){
        event.getDispatcher().register(Commands.literal("nmg_load").then(Commands.literal("start").then(Commands.argument("label",StringArgumentType.word()).then(Commands.argument("ticks",IntegerArgumentType.integer(20,1200)).then(Commands.argument("perTick",IntegerArgumentType.integer(1,20)).executes(ctx->{
            var mc=Minecraft.getInstance();var server=mc.getCurrentServer();
            if(active!=null||mc.player==null||mc.level==null||server==null||!Set.of("127.0.0.1","localhost","[::1]").contains(server.ip.replaceFirst(":\\d+$",""))){ctx.getSource().sendFailure(Component.literal("A connected loopback validation session and no active load are required"));return 0;}
            active=new Run(StringArgumentType.getString(ctx,"label"),IntegerArgumentType.getInteger(ctx,"ticks"),IntegerArgumentType.getInteger(ctx,"perTick"));return 1;
        }))))).then(Commands.literal("stop").executes(ctx->{if(active==null)return 0;finish(true);return 1;})));
    }
    private static void tick(ClientTickEvent.Post event){
        var run=active;if(run==null)return;var mc=Minecraft.getInstance();
        if(mc.level!=run.world||mc.getConnection()!=run.connection||mc.player==null){finish(true);return;}
        if(run.elapsed++>=run.ticks){if(run.pending.isEmpty()||run.elapsed>=run.ticks+100)finish(false);return;}
        var mode=net.nicomar.nomoregrids.client.input.ConstructionControls.currentIntent();
        Optional<UUID> piece=Optional.empty();long revision=-1;
        if(mc.hitResult instanceof BlockHitResult hit&&mc.level.getBlockEntity(hit.getBlockPos()) instanceof CompositeCellEntity cell){var found=ComponentSelection.select(mc.player,cell.view(),mode.candidate());if(found.isPresent()){piece=Optional.of(found.orElseThrow().pieceId());revision=found.orElseThrow().revision();}}
        for(int i=0;i<run.perTick;i++){
            boolean stale=(run.sent&1)==1;UUID id=UUID.randomUUID();
            if(run.pending.size()==2048){var first=run.pending.keySet().iterator();first.next();first.remove();run.evicted++;}
            run.pending.put(id,stale);run.sent++;
            ClientPacketDistributor.sendToServer(new ActionPayload(id,ActionPayload.Action.PROBE,false,mode,stale?Optional.of(UUID.randomUUID()):piece,stale?Long.MAX_VALUE:revision));
        }
    }
    public static void result(ActionResultPayload result){
        var run=active;if(run==null)return;var stale=run.pending.remove(result.operation());if(stale==null)return;run.replies++;
        if(stale){if(result.accepted())run.acceptedStale++;else run.rejectedStale++;}
        else if(result.accepted())run.acceptedCurrent++;else run.rejectedCurrent++;
    }
    private static void finish(boolean aborted){
        var run=active;active=null;if(run==null)return;var json=new JsonObject();json.addProperty("schema",1);json.addProperty("kind","client-intent-load");json.addProperty("label",run.label);json.addProperty("aborted",aborted);json.addProperty("ticks",run.ticks);json.addProperty("perTick",run.perTick);json.addProperty("sent",run.sent);json.addProperty("replies",run.replies);json.addProperty("acceptedCurrent",run.acceptedCurrent);json.addProperty("rejectedCurrent",run.rejectedCurrent);json.addProperty("acceptedStale",run.acceptedStale);json.addProperty("rejectedStale",run.rejectedStale);json.addProperty("unanswered",run.pending.size()+run.evicted);json.addProperty("rateLimitDropsMeasured",false);json.addProperty("mutatingActionsSent",false);
        var mc=Minecraft.getInstance();var path=mc.gameDirectory.toPath().resolve("nomoregrids/profiles/load-"+UUID.randomUUID()+".json");
        try{SectionStore.atomic(path,new GsonBuilder().setPrettyPrinting().create().toJson(json).getBytes(StandardCharsets.UTF_8));if(mc.player!=null)mc.player.sendSystemMessage(Component.literal("NMG load report: "+path));}
        catch(Exception ex){net.nicomar.nomoregrids.NoMoreGrids.LOGGER.error("Cannot save load report",ex);}
    }
}
