package validation;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.nicomar.nomoregrids.compat.paintableblocks.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.Rotation;
import net.nicomar.nomoregrids.persistence.SectionStore;
import net.nicomar.nomoregrids.platform.*;

/** Actual world fixture recipes. Every scene is validated against real section byte budgets before mutation. */
final class BenchmarkScenes {
    static final Set<String> NAMES=Set.of("baseline","wall10000","dense4096","pb256","boundary8","fluids256","fluids320","native_wall10000","native_dense4096","native_pb256","native_boundary8","native_fluids256","native_fluids320");
    record Plan(String scene,UUID run,BlockPos anchor,List<PieceRecord> pieces,List<BlockPos> enclosure,Set<net.minecraft.world.level.ChunkPos> loadedChunks){Plan{pieces=List.copyOf(pieces);enclosure=List.copyOf(enclosure);loadedChunks=Set.copyOf(loadedChunks);}int chunks(){return loadedChunks.size();}}
    static Plan create(ServerLevel level,String scene,UUID run,BlockPos anchor)throws Exception{
        if(!NAMES.contains(scene))throw new IllegalArgumentException("Unknown scene: "+scene);
        if((anchor.getX()&15)!=0||(anchor.getY()&15)!=0||(anchor.getZ()&15)!=0)throw new IllegalArgumentException("Anchor must be section aligned");
        String geometry=scene.startsWith("native_")?scene.substring(7):scene;
        int count=switch(geometry){case "wall10000"->10000;case "dense4096"->4096;case "pb256","fluids256"->256;case "fluids320"->320;case "boundary8"->512;default->0;};
        var pieces=new ArrayList<PieceRecord>();var owners=new TreeMap<SectionPos,List<PieceRecord>>();var chunks=new HashSet<net.minecraft.world.level.ChunkPos>();
        for(int i=0;i<count;i++){
            int x=0,y=0,z=0;
            switch(geometry){
                case "wall10000"->{x=i%100;y=i/100;}
                case "dense4096"->{x=i%16;y=i/16%16;z=i/256;}
                case "pb256"->{x=(i%16)*2;z=(i/16)*2;}
                case "boundary8"->{int chunk=i/64;x=chunk*16+(chunk==7?14:15);y=i%8;z=i/8%8;}
                case "fluids256","fluids320"->{int width=geometry.equals("fluids320")?20:16;x=i%width;z=i/width;}
                default->throw new IllegalStateException();
            }
            var pos=anchor.offset(x,y,z);var id=UUID.nameUUIDFromBytes((run+"|"+i).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            PieceRecord p;
            if(geometry.equals("pb256"))p=painted(level,pos,id,i);
            else p=NativeStates.capture((geometry.startsWith("fluids")?Blocks.QUARTZ_SLAB:Blocks.STONE).defaultBlockState(),level,pos,id);
            if(!scene.startsWith("native_")&&!geometry.startsWith("fluids"))p=move(p,p.origin16().add(new GridPoint(8,0,0)),0);
            for(var c:p.worldOccupation().cells(8)){
                requirePosition(level,WorldService.pos(c));
                chunks.add(new net.minecraft.world.level.ChunkPos(Math.toIntExact(Math.floorDiv(c.x(),16)),Math.toIntExact(Math.floorDiv(c.z(),16))));
            }
            pieces.add(p);owners.computeIfAbsent(p.ownerSection(),k->new ArrayList<>()).add(p);
        }
        if(!scene.startsWith("native_"))for(var e:owners.entrySet())SectionStore.encode(e.getKey(),0,e.getValue());
        var enclosure=new ArrayList<BlockPos>();
        if(geometry.startsWith("fluids")){
            int width=geometry.equals("fluids320")?20:16;
            for(int x=-1;x<=width;x++)for(int z=-1;z<=16;z++)if(x==-1||x==width||z==-1||z==16)for(int y=0;y<3;y++){
                var wall=anchor.offset(x,y,z);requirePosition(level,wall);enclosure.add(wall);chunks.add(new net.minecraft.world.level.ChunkPos(Math.floorDiv(wall.getX(),16),Math.floorDiv(wall.getZ(),16)));
            }
        }
        if(geometry.equals("boundary8")&&chunks.size()!=8)throw new IllegalStateException("Boundary scene must touch exactly 8 chunks");
        return new Plan(scene,run,anchor,pieces,enclosure,chunks);
    }
    static void requirePosition(ServerLevel level,BlockPos pos){if(!level.isLoaded(pos)||level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos))throw new IllegalArgumentException("Load scene chunks within world limits: "+pos);}
    private static PieceRecord painted(ServerLevel level,BlockPos pos,UUID id,int i)throws Exception{
        var bridge=PaintableBridge.available().orElseThrow(()->new IllegalArgumentException("PB required"));
        var adapter=new PaintableAdapter(BenchmarkScenes.class.getClassLoader());var base=bridge.create(level,pos,4);
        Object design=adapter.restore(base.geometryPayload());int cell=i%4+4*(3+4*(i/4%4));
        design=design.getClass().getMethod("paint",int.class,Direction.class,int.class).invoke(design,cell,Direction.UP,1+i/16);
        var payload=adapter.capture(design);
        return new PieceRecord(id,0,base.adapterId(),base.adapterVersion(),base.sourceBlockId(),base.sourceState(),base.origin16(),base.orientation(),base.pivot16(),bridge.snapshot(payload),payload,base.materialPayload(),base.itemPayload(),base.groupId(),base.flags());
    }
    static PieceRecord move(PieceRecord p,GridPoint origin,long revision){return new PieceRecord(p.pieceId(),revision,p.adapterId(),p.adapterVersion(),p.sourceBlockId(),p.sourceState(),origin,p.orientation(),p.pivot16(),p.geometry(),p.geometryPayload(),p.materialPayload(),p.itemPayload(),p.groupId(),p.flags());}
}
