package net.nicomar.nomoregrids.platform;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.compat.vanilla.VanillaCatalog;
import net.nicomar.nomoregrids.simulation.support.*;
import net.nicomar.nomoregrids.world.PieceIndex;

/** Exact support against the proposed index and loaded native neighbors. */
public final class WorldSupport {
    private WorldSupport(){}
    public static boolean supported(ServerLevel level,PieceIndex index,Set<CellPos> changed,PieceRecord dependent,VanillaCatalog catalog){
        for(var requirement:SupportPatches.required(dependent)){
            List<SupportResolver.Patch> surfaces=new ArrayList<>();Set<UUID> seen=new HashSet<>();
            for(var cell:SupportPatches.contactCells(requirement).cells(8)){
                var pos=WorldService.pos(cell);if(!level.isLoaded(pos))throw new IllegalArgumentException("UNLOADED_REGION: support");
                if(changed.contains(cell)||!index.at(cell).isEmpty()){
                    for(UUID id:index.at(cell))if(!id.equals(dependent.pieceId())&&seen.add(id)){
                        var p=index.records().get(id);if(p.flags().contains(PieceRecord.Flag.SUBSTITUTE)||!p.adapterId().value().equals("nomoregrids:box")||!SupportPatches.required(p).isEmpty())continue;
                        surfaces.addAll(SupportPatches.surfaces(p.geometry().collision().transform(net.nicomar.nomoregrids.geometry.Rotation.of(p.orientation()),p.pivot16(),p.origin16())));
                    }
                }else{
                    var state=level.getBlockState(pos);if(state.isAir()||state.getBlock() instanceof net.nicomar.nomoregrids.world.CompositeCellBlock)continue;
                    if(catalog.eligible(state)&&!SupportPatches.required(NativeStates.capture(state,level,pos,UUID.randomUUID())).isEmpty())continue;
                    Direction face=Direction.valueOf(requirement.normal().opposite().name());
                    if(!catalog.eligible(state)&&!state.isFaceSturdy(level,pos,face))continue;
                    try{surfaces.addAll(SupportPatches.surfaces(NativeStates.boxes(state.getCollisionShape(level,pos)).transform(net.nicomar.nomoregrids.geometry.Rotation.of(0),GridPoint.ZERO,new GridPoint(cell.x()*16,cell.y()*16,cell.z()*16))));}
                    catch(IllegalArgumentException ex){throw new IllegalArgumentException("NO_SUPPORT: native anchor geometry needs an adapter",ex);}
                }
            }
            if(!SupportResolver.covered(requirement,surfaces))return false;
        }return true;
    }
    public static Set<UUID> nearby(PieceIndex index,Set<CellPos> changed){
        Set<UUID> result=new TreeSet<>();for(var cell:changed)for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)result.addAll(index.at(new CellPos(cell.x()+x,cell.y()+y,cell.z()+z)));
        if(result.size()>4096)throw new IllegalArgumentException("COMPLEXITY_LIMIT: support neighborhood");return result;
    }
}
