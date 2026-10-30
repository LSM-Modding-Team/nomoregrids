package net.nicomar.nomoregrids.client.selection;

import java.util.*;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.*;
import net.nicomar.nomoregrids.compat.paintableblocks.PaintableBridge;
import net.nicomar.nomoregrids.compat.paintableblocks.PaintableAdapter;

/** Depth-tested selected component contour; PB uses its actual convex planes, including slopes. */
public final class ComponentOutline {
    private record Edge(Vec3 a,Vec3 b){}
    private static PieceRecord cached;
    private static List<Edge> edges=List.of();
    private ComponentOutline(){}
    public static void draw(PieceRecord piece,int color){
        if(!piece.equals(cached)){edges=build(piece);cached=piece;}
        for(var e:edges)Gizmos.line(e.a,e.b,color,1.5f);
    }
    public static void clear(){cached=null;edges=List.of();}
    private static List<Edge> build(PieceRecord p){
        if(!p.adapterId().value().equals("nomoregrids:paintableblocks")||PaintableBridge.available().isEmpty())return BoxOutline.extract(p.geometry().selection().transform(Rotation.of(p.orientation()),p.pivot16(),p.origin16()),16384).stream().map(e->new Edge(vec(e.a()),vec(e.b()))).toList();
        try{
            var adapter=new PaintableAdapter(ComponentOutline.class.getClassLoader());var primitives=adapter.primitives(p.geometryPayload());var result=new ArrayList<Edge>();
            for(var primitive:primitives)for(var plane:primitive.geometry().planes()){
                var vertices=primitive.geometry().vertices().stream().filter(v->Math.abs(plane.normal().dot(v)-plane.distance())<ConvexVolume.EPS).toList();if(vertices.size()<3)continue;
                var center=new ConvexVolume.Vec(0,0,0);for(var v:vertices)center=center.add(v.scale(1.0/vertices.size()));var outside=center.add(plane.normal().scale(ConvexVolume.EPS*4));
                if(primitives.stream().anyMatch(other->other!=primitive&&other.geometry().planes().stream().allMatch(f->f.normal().dot(outside)<=f.distance())))continue;
                var n=plane.normal();var u=n.cross(Math.abs(n.x())<0.8?new ConvexVolume.Vec(1,0,0):new ConvexVolume.Vec(0,1,0));var v=n.cross(u);var c=center;
                var sorted=vertices.stream().sorted(Comparator.comparingDouble(x->Math.atan2(x.sub(c).dot(v),x.sub(c).dot(u)))).toList();
                for(int i=0;i<sorted.size();i++){result.add(new Edge(transform(p,sorted.get(i)),transform(p,sorted.get((i+1)%sorted.size()))));if(result.size()>16384)throw new IllegalArgumentException("Outline budget");}
            }return List.copyOf(result);
        }catch(ReflectiveOperationException ex){throw new IllegalArgumentException("PB contour API",ex);}
    }
    private static Vec3 vec(GridPoint p){return new Vec3(p.x()/16.0,p.y()/16.0,p.z()/16.0);}
    private static Vec3 transform(PieceRecord p,ConvexVolume.Vec v){
        var r=Rotation.of(p.orientation());var x=r.apply(new GridPoint(1,0,0));var y=r.apply(new GridPoint(0,1,0));var z=r.apply(new GridPoint(0,0,1));double a=v.x()-p.pivot16().x(),b=v.y()-p.pivot16().y(),c=v.z()-p.pivot16().z();
        return new Vec3((x.x()*a+y.x()*b+z.x()*c+p.pivot16().x()+p.origin16().x())/16,(x.y()*a+y.y()*b+z.y()*c+p.pivot16().y()+p.origin16().y())/16,(x.z()*a+y.z()*b+z.z()*c+p.pivot16().z()+p.origin16().z())/16);
    }
}
