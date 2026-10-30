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
    private static Object connection,world;
    private ClientCellReplication(){}
    public static void reset(){ASSEMBLER.clear();HEADERS.clear();connection=null;world=null;}
    public static void accept(CellFragmentPayload packet){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        if(connection!=mc.getConnection()||world!=mc.level){reset();connection=mc.getConnection();world=mc.level;}
        if(!mc.level.dimension().identifier().toString().equals(packet.dimension()))return;
        long now=System.nanoTime();HEADERS.values().removeIf(h->now-h.started>=10_000_000_000L);
        var f=packet.fragment();var old=HEADERS.get(f.message());var header=new Header(packet.dimension(),packet.position(),packet.delta(),now);
        if(old!=null&&(!old.dimension.equals(header.dimension)||!old.pos.equals(header.pos)||old.delta!=header.delta)){reset();return;}
        if(old==null){if(HEADERS.size()>=SnapshotAssembler.IN_FLIGHT)return;HEADERS.put(f.message(),header);}
        try{
            var assembled=ASSEMBLER.accept(f,now);if(assembled.isEmpty())return;HEADERS.remove(f.message());
            if(!mc.level.isLoaded(packet.position())||!(mc.level.getBlockEntity(packet.position()) instanceof CompositeCellEntity cell))return;
            var next=packet.delta()?CellDelta.apply(cell.view(),assembled.orElseThrow()):CellView.decode(assembled.orElseThrow());cell.install(next);
        }catch(IOException|RuntimeException ex){ClientPacketDistributor.sendToServer(new CellResyncPayload(packet.position()));}
    }
}
