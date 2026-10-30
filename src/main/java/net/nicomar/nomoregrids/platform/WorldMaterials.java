package net.nicomar.nomoregrids.platform;

import java.io.IOException;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.Vec3;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.*;
import net.nicomar.nomoregrids.world.CompositeCellEntity;

/** Material hooks operate on exposed components rather than deleting the technical block. */
public final class WorldMaterials {
    private WorldMaterials(){}
    public static List<PieceRecord> exposed(CompositeCellEntity cell,Direction face){
        var result=new ArrayList<PieceRecord>();if(!cell.valid())return result;var at=cell.getBlockPos();
        for(var p:cell.view().replicas())if(!p.flags().contains(PieceRecord.Flag.SUBSTITUTE)){
            boolean exposed=false;for(var surface:net.nicomar.nomoregrids.diagnostic.GeometryCache.faces(p.worldOccupation(),4096)){
                if(face!=null&&!surface.normal().name().equals(face.name()))continue;var r=surface.rect();double u=(r.u0()+r.u1())/32.0,v=(r.v0()+r.v1())/32.0,plane=surface.plane()/16.0;
                var point=surface.normal().axis()==0?new Vec3(plane,v,u):surface.normal().axis()==1?new Vec3(u,plane,v):new Vec3(u,v,plane);
                if(point.x<at.getX()-1e-7||point.x>at.getX()+1+1e-7||point.y<at.getY()-1e-7||point.y>at.getY()+1+1e-7||point.z<at.getZ()-1e-7||point.z>at.getZ()+1+1e-7)continue;
                var n=surface.normal();var start=point.add(n.x*2,n.y*2,n.z*2);var end=point.add(-n.x*1e-7,-n.y*1e-7,-n.z*1e-7);var own=PieceQueries.ray(p,start,end);if(own.isEmpty())continue;
                if(cell.view().replicas().stream().filter(other->!other.pieceId().equals(p.pieceId())).noneMatch(other->PieceQueries.ray(other,start,end).map(hit->hit.distance16()<own.orElseThrow().distance16()-1e-5).orElse(false))){exposed=true;break;}
            }if(exposed)result.add(p);
        }return List.copyOf(result);
    }
    public static int fire(CompositeCellEntity cell,BlockGetter level,BlockPos pos,Direction face,boolean spread){int max=0;for(var p:exposed(cell,face))try{var state=NativeStates.resolve(p);max=Math.max(max,spread?state.getFireSpreadSpeed(level,pos,face):state.getFlammability(level,pos,face));}catch(RuntimeException ignored){}return max;}
    public static boolean burn(ServerLevel level,BlockPos pos,Direction face,LivingEntity igniter){
        var service=NmgRuntime.existing(level);if(service==null||service.quarantined()||!(level.getBlockEntity(pos) instanceof CompositeCellEntity cell))return false;
        var candidates=exposed(cell,face);int max=fire(cell,level,pos,face,false);var ids=new HashSet<UUID>();for(var p:candidates){int odds=NativeStates.resolve(p).getFlammability(level,pos,face);if(odds>0&&level.getRandom().nextInt(Math.max(1,max))<odds)ids.add(p.pieceId());}
        try{service.removeWorld(ids,false);return !ids.isEmpty();}catch(IOException|RuntimeException ex){if(service.quarantined())NmgRuntime.failed(level,ex);return false;}
    }
    public static void explode(ServerLevel level,BlockPos pos,Explosion explosion){
        if(explosion.getBlockInteraction()==Explosion.BlockInteraction.KEEP)return;var service=NmgRuntime.existing(level);if(service==null||service.quarantined()||!(level.getBlockEntity(pos) instanceof CompositeCellEntity cell))return;
        var ids=new HashSet<UUID>();for(var p:cell.view().replicas())if(!p.flags().contains(PieceRecord.Flag.SUBSTITUTE)){
            var b=p.worldOccupation().bounds();var center=new Vec3((b.min().x()+b.max().x())/32.0,(b.min().y()+b.max().y())/32.0,(b.min().z()+b.max().z())/32.0);double distance=explosion.center().distanceTo(center);var state=NativeStates.resolve(p);double energy=explosion.radius()*1.3-distance*0.75-(state.getExplosionResistance(level,pos,explosion)+0.3)*0.3;
            if(energy<=0)continue;var own=PieceQueries.ray(p,explosion.center(),center);if(own.isPresent()&&cell.view().replicas().stream().filter(q->!q.pieceId().equals(p.pieceId())).anyMatch(q->PieceQueries.ray(q,explosion.center(),center).map(hit->hit.distance16()<own.orElseThrow().distance16()-1e-5).orElse(false)))continue;ids.add(p.pieceId());
        }
        try{service.removeWorld(ids,true);}catch(IOException|RuntimeException ex){if(service.quarantined())NmgRuntime.failed(level,ex);}
    }
}
