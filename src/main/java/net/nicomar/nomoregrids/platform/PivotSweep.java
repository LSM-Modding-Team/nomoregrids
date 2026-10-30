package net.nicomar.nomoregrids.platform;

import java.util.*;
import net.minecraft.world.phys.AABB;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.*;

/** Continuous quarter-turn bounds, recursively refined. Unresolved grazing contacts block the operation. */
public final class PivotSweep {
    private PivotSweep(){}
    public static void validate(WorldService service,PieceRecord before,PieceRecord after,Set<UUID> moving){
        var a=before.geometry().collision().bounds();var b=after.geometry().collision().bounds();if(a.equals(b))return;
        int axis=before.sourceState().containsKey("half")&&before.sourceBlockId().value().endsWith("_door")?1:a.max().x()-a.min().x()==b.max().x()-b.min().x()?0:2;
        int u=(axis+1)%3,v=(axis+2)%3;double[] ac=center(a),bc=center(b);double best=Double.POSITIVE_INFINITY,pu=0,pv=0,angle=0;
        for(int sign:new int[]{-1,1}){double x=(bc[u]+ac[u]+sign*(ac[v]-bc[v]))/2,y=(bc[v]+ac[v]+sign*(bc[u]-ac[u]))/2;double score=Math.hypot(x-Math.max(min(a,u),min(b,u)),y-Math.max(min(a,v),min(b,v)));if(score<best){best=score;pu=x;pv=y;angle=sign*Math.PI/2;}}
        var swept=bounds(a,axis,pu,pv,Math.min(0,angle),Math.max(0,angle));var world=worldBounds(swept,before);var cells=Box16.of((long)Math.floor(world[0]),(long)Math.floor(world[1]),(long)Math.floor(world[2]),(long)Math.ceil(world[3]),(long)Math.ceil(world[4]),(long)Math.ceil(world[5])).cells(32);var obstacles=new ArrayList<ConvexVolume>();var seen=new HashSet<UUID>();
        for(var cell:cells){var pos=WorldService.pos(cell);if(!service.level().isLoaded(pos))throw new IllegalArgumentException("UNLOADED_REGION: pivot sweep");for(var id:service.snapshot().at(cell))if(!moving.contains(id)&&seen.add(id))obstacles.addAll(service.adapter(service.snapshot().records().get(id)).occupation(service.snapshot().records().get(id)).stream().map(g->{var p=service.snapshot().records().get(id);return g.transformed(p.orientation(),p.pivot16(),p.origin16());}).toList());if(!service.snapshot().at(cell).isEmpty())continue;var state=service.level().getBlockState(pos);for(var box:state.getCollisionShape(service.level(),pos).toAabbs())obstacles.add(ConvexVolume.box((box.minX+cell.x())*16,(box.minY+cell.y())*16,(box.minZ+cell.z())*16,(box.maxX+cell.x())*16,(box.maxY+cell.y())*16,(box.maxZ+cell.z())*16));}
        for(var box:service.level().getEntityCollisions(null,new AABB(world[0]/16,world[1]/16,world[2]/16,world[3]/16,world[4]/16,world[5]/16)))for(var q:box.toAabbs())obstacles.add(ConvexVolume.box(q.minX*16,q.minY*16,q.minZ*16,q.maxX*16,q.maxY*16,q.maxZ*16));
        for(var obstacle:obstacles)if(blocked(a,axis,pu,pv,Math.min(0,angle),Math.max(0,angle),before,obstacle,0))throw new IllegalArgumentException("ENTITY_BLOCKED or occupied pivot trajectory");
    }
    private static boolean blocked(Box16 box,int axis,double pu,double pv,double lo,double hi,PieceRecord p,ConvexVolume obstacle,int depth){
        var b=worldBounds(bounds(box,axis,pu,pv,lo,hi),p);var contact=obstacle.contact(ConvexVolume.box(b[0],b[1],b[2],b[3],b[4],b[5]));if(contact==ConvexVolume.Contact.DISJOINT||contact==ConvexVolume.Contact.TOUCH)return false;
        // At the refinement limit reject uncertainty rather than allowing tunnelling between samples.
        if(depth==18)return true;double mid=(lo+hi)/2;return blocked(box,axis,pu,pv,lo,mid,p,obstacle,depth+1)||blocked(box,axis,pu,pv,mid,hi,p,obstacle,depth+1);
    }
    private static double[] bounds(Box16 box,int axis,double pu,double pv,double lo,double hi){
        double[] result={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};int u=(axis+1)%3,v=(axis+2)%3;result[axis]=min(box,axis);result[axis+3]=max(box,axis);
        for(double x:new double[]{min(box,u),max(box,u)})for(double y:new double[]{min(box,v),max(box,v)}){var ru=range(x-pu,-(y-pv),lo,hi);var rv=range(y-pv,x-pu,lo,hi);result[u]=Math.min(result[u],pu+ru[0]);result[u+3]=Math.max(result[u+3],pu+ru[1]);result[v]=Math.min(result[v],pv+rv[0]);result[v+3]=Math.max(result[v+3],pv+rv[1]);}return result;
    }
    private static double[] range(double a,double b,double lo,double hi){double low=Math.min(a*Math.cos(lo)+b*Math.sin(lo),a*Math.cos(hi)+b*Math.sin(hi)),high=Math.max(a*Math.cos(lo)+b*Math.sin(lo),a*Math.cos(hi)+b*Math.sin(hi));double t=Math.atan2(b,a);for(int n=-2;n<=2;n++){double angle=t+n*Math.PI;if(angle>=lo&&angle<=hi){double q=a*Math.cos(angle)+b*Math.sin(angle);low=Math.min(low,q);high=Math.max(high,q);}}return new double[]{low,high};}
    private static double[] worldBounds(double[] b,PieceRecord p){var volume=ConvexVolume.box(b[0],b[1],b[2],b[3],b[4],b[5]).transformed(p.orientation(),p.pivot16(),p.origin16());double[] r={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};for(var x:volume.vertices()){r[0]=Math.min(r[0],x.x());r[1]=Math.min(r[1],x.y());r[2]=Math.min(r[2],x.z());r[3]=Math.max(r[3],x.x());r[4]=Math.max(r[4],x.y());r[5]=Math.max(r[5],x.z());}return r;}
    private static double min(Box16 b,int axis){return axis==0?b.min().x():axis==1?b.min().y():b.min().z();}
    private static double max(Box16 b,int axis){return axis==0?b.max().x():axis==1?b.max().y():b.max().z();}
    private static double[] center(Box16 b){return new double[]{(b.min().x()+b.max().x())/2.0,(b.min().y()+b.max().y())/2.0,(b.min().z()+b.max().z())/2.0};}
}
