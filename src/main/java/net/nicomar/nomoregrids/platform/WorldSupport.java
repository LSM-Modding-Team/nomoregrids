package net.nicomar.nomoregrids.platform;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.compat.vanilla.VanillaCatalog;
import net.nicomar.nomoregrids.simulation.support.*;
import net.nicomar.nomoregrids.world.PieceIndex;

/** Exact proposed support. Declared decorative chains must reach a root; a cycle is never its own support. */
public final class WorldSupport {
    private WorldSupport(){}
    public static boolean supported(ServerLevel level,PieceIndex index,Set<CellPos> changed,PieceRecord dependent,VanillaCatalog catalog){return supported(level,index,changed,dependent,catalog,new HashSet<>(),new int[]{512});}
    private static boolean chain(PieceRecord p){try{return NativeStates.resolve(p).getBlock() instanceof net.minecraft.world.level.block.WallHangingSignBlock&&WorldDecorations.eligible(NativeStates.resolve(p));}catch(RuntimeException ex){return false;}}
    private static boolean supported(ServerLevel level,PieceIndex index,Set<CellPos> changed,PieceRecord dependent,VanillaCatalog catalog,Set<UUID> path,int[] remaining){
        if(remaining[0]--<=0||path.size()>=32)throw new IllegalArgumentException("COMPLEXITY_LIMIT: support chain");if(!path.add(dependent.pieceId()))return false;
        try{for(var alternative:SupportPatches.alternatives(dependent))if(supported(level,index,changed,dependent,catalog,alternative,path,remaining))return true;return false;}finally{path.remove(dependent.pieceId());}
    }
    private static boolean supported(ServerLevel level,PieceIndex index,Set<CellPos> changed,PieceRecord dependent,VanillaCatalog catalog,List<SupportResolver.Patch> requirements,Set<UUID> path,int[] remaining){
        for(var requirement:requirements){
            List<SupportResolver.Patch> surfaces=new ArrayList<>();Set<UUID> seen=new HashSet<>();
            for(var cell:SupportPatches.contactCells(requirement).cells(8)){
                var pos=WorldService.pos(cell);if(!level.isLoaded(pos))throw new IllegalArgumentException("UNLOADED_REGION: support");
                if(!index.at(cell).isEmpty()||changed.contains(cell)&&WorldNativeView.at(cell).isEmpty()){
                    for(UUID id:index.at(cell))if(!id.equals(dependent.pieceId())&&seen.add(id)){
                        var p=index.records().get(id);if(p.flags().contains(PieceRecord.Flag.SUBSTITUTE))continue;boolean link=chain(p)&&chain(dependent);if(!SupportPatches.required(p).isEmpty()&&(!link||!supported(level,index,changed,p,catalog,path,remaining)))continue;
                        net.nicomar.nomoregrids.geometry.BoxUnion support;
                        if(link)support=NativeStates.boxes(NativeStates.resolve(p).getCollisionShape(level,WorldService.pos(p.origin16().cell())));
                        else if(p.adapterId().value().equals("nomoregrids:box")||WorldFunctionals.piece(p))support=p.geometry().occupation();
                        else if(p.adapterId().value().equals("nomoregrids:paintableblocks"))support=net.nicomar.nomoregrids.compat.paintableblocks.PaintableBridge.available().orElseThrow(()->new IllegalArgumentException("UNSUPPORTED_ADAPTER: PB support")).support(p.geometryPayload());
                        else continue;
                        surfaces.addAll(SupportPatches.surfaces(support.transform(net.nicomar.nomoregrids.geometry.Rotation.of(p.orientation()),p.pivot16(),p.origin16())));
                    }
                }else{
                    var state=WorldNativeView.at(cell).orElseGet(()->level.getBlockState(pos));if(state.isAir()||state.getBlock() instanceof net.nicomar.nomoregrids.world.CompositeCellBlock)continue;
                    boolean link=state.getBlock() instanceof net.minecraft.world.level.block.WallHangingSignBlock&&WorldDecorations.eligible(state)&&chain(dependent);
                    if(link){var box=NativeStates.boxes(state.getCollisionShape(level,pos));var source=new ResourceId(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());var id=UUID.nameUUIDFromBytes(("native-support|"+pos.asLong()).getBytes(java.nio.charset.StandardCharsets.UTF_8));var nativeAnchor=new PieceRecord(id,0,new ResourceId("nomoregrids:box"),1,source,NativeStates.properties(state),new GridPoint(cell.x()*16,cell.y()*16,cell.z()*16),0,GridPoint.BLOCK_CENTER,new BoxGeometrySnapshot(box,box,box),BinaryData.EMPTY,BinaryData.EMPTY,BinaryData.EMPTY,Optional.empty(),Set.of());if(!supported(level,index,changed,nativeAnchor,catalog,path,remaining))continue;surfaces.addAll(SupportPatches.surfaces(nativeAnchor.worldOccupation()));continue;}
                    var nativePiece=catalog.eligible(state)?NativeStates.capture(state,level,pos,UUID.randomUUID()):null;if(nativePiece!=null&&!SupportPatches.required(nativePiece).isEmpty())continue;Direction face=Direction.valueOf(requirement.normal().opposite().name());if(!catalog.eligible(state)&&!state.isFaceSturdy(level,pos,face))continue;
                    try{var support=nativePiece!=null?nativePiece.geometry().occupation():NativeStates.boxes(state.getBlockSupportShape(level,pos));surfaces.addAll(SupportPatches.surfaces(support.transform(net.nicomar.nomoregrids.geometry.Rotation.of(0),GridPoint.ZERO,new GridPoint(cell.x()*16,cell.y()*16,cell.z()*16))));}
                    catch(IllegalArgumentException ex){throw new IllegalArgumentException("NO_SUPPORT: native anchor geometry needs an adapter",ex);}
                }
            }
            if(!SupportResolver.covered(requirement,surfaces))return false;
        }return true;
    }
    public static Set<UUID> nearby(PieceIndex index,Set<CellPos> changed){Set<UUID> result=new TreeSet<>();for(var cell:changed)for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)result.addAll(index.at(new CellPos(cell.x()+x,cell.y()+y,cell.z()+z)));if(result.size()>4096)throw new IllegalArgumentException("COMPLEXITY_LIMIT: support neighborhood");return result;}
}
