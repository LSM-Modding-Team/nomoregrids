package net.nicomar.nomoregrids.network;

import java.util.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.nicomar.nomoregrids.core.GridPoint;

/** Server-owned session preferences. Geometry/actions are independently revalidated by the server. */
public final class NmgNetworking {
    private static final Map<UUID,ModePayload> MODES=new HashMap<>();
    private static final Map<UUID,IntentLimiter> LIMITERS=new HashMap<>();
    private static final Set<UUID> READY=new HashSet<>();
    private static final Set<UUID> INVALID_PREFERENCES=new HashSet<>();
    private NmgNetworking(){}
    public static void register(IEventBus bus){bus.addListener(NmgNetworking::payloads);}
    private static void payloads(RegisterPayloadHandlersEvent event){event.registrar("2").playToServer(ModePayload.TYPE,ModePayload.CODEC,(packet,context)->context.enqueueWork(()->{
        UUID id=context.player().getUUID();if(READY.contains(id)&&LIMITERS.computeIfAbsent(id,key->new IntentLimiter()).allow(System.nanoTime()))MODES.put(id,packet);
    })).playToServer(ActionPayload.TYPE,ActionPayload.CODEC,(packet,context)->context.enqueueWork(()->{
        if(context.player() instanceof net.minecraft.server.level.ServerPlayer player)net.nicomar.nomoregrids.platform.NmgRuntime.action(player,packet);
    })).playToServer(SessionAckPayload.TYPE,SessionAckPayload.CODEC,(packet,context)->context.enqueueWork(()->{UUID id=context.player().getUUID();if(READY.contains(id)||!LIMITERS.computeIfAbsent(id,key->new IntentLimiter()).allow(System.nanoTime()))return;if(packet.fingerprint().equals(SessionPayload.fingerprint()))READY.add(context.player().getUUID());else context.disconnect(net.minecraft.network.chat.Component.translatable("message.nomoregrids.profile_mismatch"));})).playToClient(ActionResultPayload.TYPE,ActionResultPayload.CODEC).playToClient(SessionPayload.TYPE,SessionPayload.CODEC);}
    public static ModePayload modes(UUID player){return MODES.getOrDefault(player,new ModePayload(false,false,false,false,false,0,GridPoint.ZERO,0));}
    public static boolean ready(UUID player){return READY.contains(player);}
    public static void disconnected(UUID player){READY.remove(player);MODES.remove(player);LIMITERS.remove(player);INVALID_PREFERENCES.remove(player);}
    private static java.nio.file.Path preferenceFile(net.minecraft.server.level.ServerPlayer player){return player.level().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("nomoregrids/preferences").resolve(player.getUUID()+".json");}
    public static void joined(net.minecraft.server.level.ServerPlayer player){
        READY.remove(player.getUUID());ModePayload mode=modes(player.getUUID());java.nio.file.Path file=preferenceFile(player);
        try{if(java.nio.file.Files.exists(file)){
            if(java.nio.file.Files.size(file)>8192)throw new java.io.IOException("Preference size");var json=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(file)).getAsJsonObject();
            if(json.get("schema").getAsInt()!=1)throw new java.io.IOException("Preference schema");
            mode=new ModePayload(json.get("precise").getAsBoolean(),json.get("expanded").getAsBoolean(),json.get("adjusted").getAsBoolean(),false,false,0,GridPoint.ZERO,0);
        }}catch(java.io.IOException|RuntimeException ex){INVALID_PREFERENCES.add(player.getUUID());net.nicomar.nomoregrids.NoMoreGrids.LOGGER.warn("Cannot read NMG mode preferences; original retained",ex);}
        MODES.put(player.getUUID(),mode);net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,new SessionPayload(1,SessionPayload.localHash(),SessionPayload.localRules(),mode.precise(),mode.expanded(),mode.adjusted()));
    }
    public static void persist(net.minecraft.server.level.ServerPlayer player){
        if(INVALID_PREFERENCES.contains(player.getUUID()))return;
        var mode=modes(player.getUUID());var json=new com.google.gson.JsonObject();json.addProperty("schema",1);json.addProperty("precise",mode.precise());json.addProperty("expanded",mode.expanded());json.addProperty("adjusted",mode.adjusted());
        try{net.nicomar.nomoregrids.persistence.SectionStore.atomic(preferenceFile(player),json.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(java.io.IOException ex){net.nicomar.nomoregrids.NoMoreGrids.LOGGER.warn("Cannot persist NMG mode preferences",ex);}
    }
}
