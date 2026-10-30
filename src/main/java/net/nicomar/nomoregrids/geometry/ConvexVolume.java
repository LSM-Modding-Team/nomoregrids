package net.nicomar.nomoregrids.geometry;

import java.util.*;
import net.nicomar.nomoregrids.core.GridPoint;

/** Convex half-space geometry in local q units. Retains non-integral slope geometry. */
public final class ConvexVolume {
    public static final double EPS=16e-7;
    public record Vec(double x,double y,double z){
        public Vec{if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IllegalArgumentException("Non-finite vertex");}
        public Vec add(Vec b){return new Vec(x+b.x,y+b.y,z+b.z);}public Vec sub(Vec b){return new Vec(x-b.x,y-b.y,z-b.z);}
        public Vec scale(double n){return new Vec(x*n,y*n,z*n);}public double dot(Vec b){return x*b.x+y*b.y+z*b.z;}
        public Vec cross(Vec b){return new Vec(y*b.z-z*b.y,z*b.x-x*b.z,x*b.y-y*b.x);}
        public double length(){return Math.hypot(Math.hypot(x,y),z);}
    }
    /** Interior n dot p <= distance. Unit normal makes tolerances meaningful. */
    public record Plane(Vec normal,double distance){
        public Plane{double n=normal.length();if(n<1e-12||!Double.isFinite(distance))throw new IllegalArgumentException("Invalid plane");normal=normal.scale(1/n);distance/=n;}
    }
    private final List<Plane> planes;
    private final List<Vec> vertices;
    public ConvexVolume(List<Plane> planes){
        if(planes.size()<4||planes.size()>32)throw new IllegalArgumentException("Plane budget");this.planes=List.copyOf(planes);vertices=vertices(this.planes);
    }
    public List<Plane> planes(){return planes;}public List<Vec> vertices(){return vertices;}
    public static ConvexVolume box(double x0,double y0,double z0,double x1,double y1,double z1){
        if(!(x0<x1&&y0<y1&&z0<z1))throw new IllegalArgumentException("Empty box");
        return new ConvexVolume(List.of(new Plane(new Vec(-1,0,0),-x0),new Plane(new Vec(1,0,0),x1),
            new Plane(new Vec(0,-1,0),-y0),new Plane(new Vec(0,1,0),y1),new Plane(new Vec(0,0,-1),-z0),new Plane(new Vec(0,0,1),z1)));
    }
    public ConvexVolume clipped(Plane plane){var next=new ArrayList<>(planes);next.add(plane);return new ConvexVolume(next);}
    /** Exact Minkowski sum with a translation segment, including the extruded edge facets. */
    public ConvexVolume swept(Vec delta){
        if(delta.length()==0)return this;var points=new ArrayList<>(vertices);vertices.forEach(v->points.add(v.add(delta)));var hull=new ArrayList<Plane>();
        for(var p:planes)addPlane(hull,new Plane(p.normal,p.distance+Math.max(0,p.normal.dot(delta))));
        for(var edge:edges()){
            var normal=edge.cross(delta);if(normal.length()<1e-10)continue;normal=normal.scale(1/normal.length());double low=Double.POSITIVE_INFINITY,high=Double.NEGATIVE_INFINITY;for(var v:points){double d=normal.dot(v);low=Math.min(low,d);high=Math.max(high,d);}
            // Only support planes containing an original edge are facets of the extrusion.
            for(var candidate:List.of(new Plane(normal,high),new Plane(normal.scale(-1),-low)))if(vertices.stream().filter(v->Math.abs(candidate.normal.dot(v)-candidate.distance)<EPS).count()>=2)addPlane(hull,candidate);
        }
        return new ConvexVolume(hull);
    }
    private static void addPlane(List<Plane> planes,Plane p){if(planes.stream().noneMatch(q->q.normal.sub(p.normal).length()<1e-10&&Math.abs(q.distance-p.distance)<EPS))planes.add(p);}
    private static List<Vec> vertices(List<Plane> planes){
        var result=new ArrayList<Vec>();
        for(int i=0;i<planes.size();i++)for(int j=i+1;j<planes.size();j++)for(int k=j+1;k<planes.size();k++){
            Plane a=planes.get(i),b=planes.get(j),c=planes.get(k);double determinant=a.normal.dot(b.normal.cross(c.normal));
            if(Math.abs(determinant)<1e-12)continue;
            Vec p=b.normal.cross(c.normal).scale(a.distance).add(c.normal.cross(a.normal).scale(b.distance)).add(a.normal.cross(b.normal).scale(c.distance)).scale(1/determinant);
            if(planes.stream().allMatch(f->f.normal.dot(p)-f.distance<=EPS)&&result.stream().noneMatch(v->v.sub(p).length()<EPS))result.add(p);
        }
        return List.copyOf(result);
    }
    public boolean hasVolume(){
        if(vertices.size()<4)return false;Vec a=vertices.getFirst();
        for(int i=1;i<vertices.size();i++)for(int j=i+1;j<vertices.size();j++)for(int k=j+1;k<vertices.size();k++)
            if(Math.abs(vertices.get(i).sub(a).dot(vertices.get(j).sub(a).cross(vertices.get(k).sub(a))))>1e-10)return true;
        return false;
    }
    public enum Contact { DISJOINT,TOUCH,OVERLAP,INDETERMINATE }
    /** Separating-axis test for convex polytopes, including edge cross products. */
    public Contact contact(ConvexVolume other){
        var axes=new ArrayList<Vec>();planes.forEach(p->axes.add(p.normal));other.planes.forEach(p->axes.add(p.normal));
        var edges=edges();var otherEdges=other.edges();for(var a:edges)for(var b:otherEdges){Vec c=a.cross(b);if(c.length()>1e-10)axes.add(c.scale(1/c.length()));}
        boolean touch=false,uncertain=false;
        if(!hasVolume()||!other.hasVolume())return Contact.DISJOINT;
        for(var axis:axes){double lo=Double.POSITIVE_INFINITY,hi=Double.NEGATIVE_INFINITY,lo2=lo,hi2=hi;
            for(var v:vertices){double d=axis.dot(v);lo=Math.min(lo,d);hi=Math.max(hi,d);}for(var v:other.vertices){double d=axis.dot(v);lo2=Math.min(lo2,d);hi2=Math.max(hi2,d);}
            double overlap=Math.min(hi,hi2)-Math.max(lo,lo2);
            if(overlap < -EPS)return Contact.DISJOINT;
            if(overlap==0)touch=true;else if(Math.abs(overlap)<=EPS)uncertain=true;
        }
        return touch?Contact.TOUCH:uncertain?Contact.INDETERMINATE:Contact.OVERLAP;
    }
    private List<Vec> edges(){
        var result=new ArrayList<Vec>();
        for(int i=0;i<vertices.size();i++)for(int j=i+1;j<vertices.size();j++){
            Vec a=vertices.get(i),b=vertices.get(j);long shared=planes.stream().filter(p->Math.abs(p.normal.dot(a)-p.distance)<EPS&&Math.abs(p.normal.dot(b)-p.distance)<EPS).count();
            if(shared>=2)result.add(b.sub(a));
        }return result;
    }
    public ConvexVolume transformed(int orientation,GridPoint pivot,GridPoint origin){
        Rotation r=Rotation.of(orientation);Vec center=new Vec(pivot.x(),pivot.y(),pivot.z());
        Vec translation=center.add(new Vec(origin.x(),origin.y(),origin.z())).sub(rotate(r,center));
        return new ConvexVolume(planes.stream().map(p->{Vec n=rotate(r,p.normal);return new Plane(n,p.distance+n.dot(translation));}).toList());
    }
    private static Vec rotate(Rotation r,Vec p){
        GridPoint x=r.apply(new GridPoint(1,0,0)),y=r.apply(new GridPoint(0,1,0)),z=r.apply(new GridPoint(0,0,1));
        return new Vec(x.x()*p.x+y.x()*p.y+z.x()*p.z,x.y()*p.x+y.y()*p.y+z.y()*p.z,x.z()*p.x+y.z()*p.y+z.z()*p.z);
    }
    public record Hit(double distance,int plane,Vec point){}
    public record RayInterval(double near,double far,int enterPlane,int exitPlane){}
    public Optional<RayInterval> interval(Vec origin,Vec direction,double reach){
        double length=direction.length();if(length==0||!Double.isFinite(length)||reach<0||!Double.isFinite(reach))throw new IllegalArgumentException("Invalid ray");
        Vec d=direction.scale(1/length);double near=Double.NEGATIVE_INFINITY,far=Double.POSITIVE_INFINITY;int entry=-1,exit=-1;
        for(int i=0;i<planes.size();i++){var p=planes.get(i);double speed=p.normal.dot(d),distance=p.distance-p.normal.dot(origin);
            if(Math.abs(speed)<1e-12){if(distance<0)return Optional.empty();continue;}double t=distance/speed;
            if(speed<0&&t>near){near=t;entry=i;}else if(speed>0&&t<far){far=t;exit=i;}if(near>far)return Optional.empty();}
        return far<0||near>reach||entry<0||exit<0?Optional.empty():Optional.of(new RayInterval(near,far,entry,exit));
    }
    public Optional<Hit> ray(Vec origin,Vec direction,double reach){
        return interval(origin,direction,reach).flatMap(interval->{double t=interval.near>=0?interval.near:interval.far;int plane=interval.near>=0?interval.enterPlane:interval.exitPlane;return t>reach?Optional.empty():Optional.of(new Hit(t,plane,origin.add(direction.scale(t/direction.length()))));});
    }
}
