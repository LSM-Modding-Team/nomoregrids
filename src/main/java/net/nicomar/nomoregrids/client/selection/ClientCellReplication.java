package net.nicomar.nomoregrids.client.selection;

import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.nicomar.nomoregrids.network.*;
import net.nicomar.nomoregrids.world.*;

/** One bounded assembler for the current connection; no cross-dimension or cross-cell assembly. */
public final class ClientCellReplication {
    private static final SnapshotAssembler ASSEMBLER=new SnapshotAssembler();
    private record Header(String dimension,BlockPos pos,boolean delta,long started){}
    private static final Map<UUID,Header> HEADERS=new HashMap<>();
    private record Retry(long created,long sent){}
    private static final Map<BlockPos,Retry> RESYNC=new LinkedHashMap<>();
    private static long lastRequest;
    private static Object connection,world;
    private ClientCellReplication(){}
    public static void reset(){ASSEMBLER.clear();HEADERS.clear();RESYNC.clear();lastRequest=0;connection=null;world=null;}
    private static void resync(BlockPos pos){if(RESYNC.size()<4096||RESYNC.containsKey(pos))RESYNC.putIfAbsent(pos.immutable(),new Retry(System.nanoTime(),0));}
    /** Wait until vanilla has installed the block entity before asking for its current snapshot. */
    public static void tick(){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.getConnection()==null)return;
        if(world!=null&&(world!=mc.level||connection!=mc.getConnection())){reset();return;}
        long now=System.nanoTime();HEADERS.values().removeIf(h->{if(now-h.started<10_000_000_000L)return false;resync(h.pos);return true;});
        if(now-lastRequest<100_000_000L)return;
        var entries=RESYNC.entrySet().iterator();while(entries.hasNext()){
            var entry=entries.next();var pos=entry.getKey();
            var retry=entry.getValue();if(!mc.level.isLoaded(pos)){if(now-retry.created>60_000_000_000L)entries.remove();continue;}
            if(!(mc.level.getBlockEntity(pos) instanceof CompositeCellEntity)){if(now-retry.created>60_000_000_000L)entries.remove();continue;}
            if(retry.sent!=0&&now-retry.sent<1_000_000_000L)continue;
            ClientPacketDistributor.sendToServer(new CellResyncPayload(pos));entry.setValue(new Retry(retry.created,now));lastRequest=now;break;
        }
    }
    public static void accept(CellFragmentPayload packet){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        if(connection!=mc.getConnection()||world!=mc.level){reset();connection=mc.getConnection();world=mc.level;}
        if(!mc.level.dimension().identifier().toString().equals(packet.dimension()))return;
        long now=System.nanoTime();HEADERS.values().removeIf(h->{if(now-h.started<10_000_000_000L)return false;resync(h.pos);return true;});
        var f=packet.fragment();var old=HEADERS.get(f.message());var header=new Header(packet.dimension(),packet.position(),packet.delta(),now);
        if(old!=null&&(!old.dimension.equals(header.dimension)||!old.pos.equals(header.pos)||old.delta!=header.delta)){for(var pending:HEADERS.values())resync(pending.pos);resync(packet.position());ASSEMBLER.clear();HEADERS.clear();return;}
        if(old==null){if(HEADERS.size()>=SnapshotAssembler.IN_FLIGHT){resync(packet.position());return;}HEADERS.put(f.message(),header);}
        try{
            var assembled=ASSEMBLER.accept(f,now);if(assembled.isEmpty())return;HEADERS.remove(f.message());
            if(!mc.level.isLoaded(packet.position())){resync(packet.position());return;}
            if(!(mc.level.getBlockEntity(packet.position()) instanceof CompositeCellEntity cell)){resync(packet.position());return;}
            var next=packet.delta()?CellDelta.apply(cell.view(),assembled.orElseThrow()):CellView.decode(assembled.orElseThrow());cell.install(next);
            if(!packet.delta())RESYNC.remove(packet.position());
        }catch(IOException|RuntimeException ex){resync(packet.position());}
    }
}
