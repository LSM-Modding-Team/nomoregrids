package net.nicomar.nomoregrids.network;

import java.io.IOException;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.nicomar.nomoregrids.world.*;

/** Native chunk tracking supplies snapshots; live edits use deltas and divergence requests a fresh snapshot. */
public final class CellReplication {
    private static final Map<ServerLevel,net.nicomar.nomoregrids.diagnostic.TrafficMeter> TRAFFIC=Collections.synchronizedMap(new WeakHashMap<>());
    private static net.nicomar.nomoregrids.diagnostic.TrafficMeter meter(ServerLevel level){return TRAFFIC.computeIfAbsent(level,k->new net.nicomar.nomoregrids.diagnostic.TrafficMeter());}
    public static net.nicomar.nomoregrids.diagnostic.TrafficMeter.Snapshot traffic(ServerLevel level){return meter(level).snapshot();}
    private CellReplication(){}
    public static void register(){NeoForge.EVENT_BUS.addListener(CellReplication::sent);}
    private static void sent(net.neoforged.neoforge.event.level.ChunkWatchEvent.Sent event){
        for(var be:event.getChunk().getBlockEntities().values())if(be instanceof CompositeCellEntity cell&&cell.valid())snapshot(event.getPlayer(),cell);
    }
    public static void request(ServerPlayer player,BlockPos pos){
        // getPlayers does not load a chunk and proves the requesting connection actually tracks it.
        if(!player.level().getChunkSource().chunkMap.getPlayers(new net.minecraft.world.level.ChunkPos(Math.floorDiv(pos.getX(),16),Math.floorDiv(pos.getZ(),16)),false).contains(player))return;
        if(player.level().isLoaded(pos)&&player.level().getBlockEntity(pos) instanceof CompositeCellEntity cell&&cell.valid())snapshot(player,cell);
    }
    public static void snapshot(ServerPlayer player,CompositeCellEntity cell){try{if(cell.getLevel() instanceof ServerLevel level)send(level,cell.getBlockPos(),false,cell.view().encode(),player);}catch(IOException ex){throw new IllegalStateException("Cannot encode public cell",ex);}}
    public static void changed(ServerLevel level,BlockPos pos,CellView before,CellView after){
        try{if(before.equals(after))return;byte[] full=after.encode(),delta;try{delta=CellDelta.encode(before,after);}catch(IOException oversized){delta=full;}send(level,pos,delta.length<full.length,delta.length<full.length?delta:full,null);}
        catch(IOException ex){throw new IllegalStateException("Cannot encode public cell",ex);}
    }
    private static void send(ServerLevel level,BlockPos pos,boolean delta,byte[] bytes,ServerPlayer player){
        var recipients=player==null?level.getChunkSource().chunkMap.getPlayers(new net.minecraft.world.level.ChunkPos(Math.floorDiv(pos.getX(),16),Math.floorDiv(pos.getZ(),16)),false).size():1;
        var metrics=meter(level);metrics.prepared(bytes.length,delta);
        for(var fragment:SnapshotAssembler.split(UUID.randomUUID(),bytes)){
            var packet=new CellFragmentPayload(level.dimension().identifier().toString(),pos,delta,fragment);
            if(player!=null)PacketDistributor.sendToPlayer(player,packet);else PacketDistributor.sendToPlayersTrackingChunk(level,new net.minecraft.world.level.ChunkPos(Math.floorDiv(pos.getX(),16),Math.floorDiv(pos.getZ(),16)),packet);
            metrics.enqueued(Math.min(SnapshotAssembler.CHUNK,bytes.length-fragment.index()*SnapshotAssembler.CHUNK),recipients);
        }
    }
}
