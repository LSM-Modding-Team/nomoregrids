package net.nicomar.nomoregrids.simulation.movement;

import java.util.*;
import net.nicomar.nomoregrids.core.GridPoint;
import net.nicomar.nomoregrids.geometry.*;

/** Native topology with exact q endpoints and tangent-compatible curves/grade transitions. */
public final class RailPath {
    public record Sample(ConvexVolume.Vec position,ConvexVolume.Vec tangent){}
    private final String shape;private final int orientation;private final GridPoint pivot,origin;
    private final double[] distances=new double[129];private final GridPoint first,last,firstOut,lastOut;
    public RailPath(String shape,int orientation,GridPoint pivot,GridPoint origin){
        this.shape=shape;this.orientation=orientation;this.pivot=pivot;this.origin=origin;GridPoint a,b;
        switch(shape){
            case "north_south"->{a=new GridPoint(8,1,0);b=new GridPoint(8,1,16);}
            case "east_west"->{a=new GridPoint(0,1,8);b=new GridPoint(16,1,8);}
            case "ascending_east"->{a=new GridPoint(0,1,8);b=new GridPoint(16,17,8);}
            case "ascending_west"->{a=new GridPoint(0,17,8);b=new GridPoint(16,1,8);}
            case "ascending_north"->{a=new GridPoint(8,17,0);b=new GridPoint(8,1,16);}
            case "ascending_south"->{a=new GridPoint(8,1,0);b=new GridPoint(8,17,16);}
            case "north_east"->{a=new GridPoint(8,1,0);b=new GridPoint(16,1,8);}
            case "south_east"->{a=new GridPoint(8,1,16);b=new GridPoint(16,1,8);}
            case "south_west"->{a=new GridPoint(8,1,16);b=new GridPoint(0,1,8);}
            case "north_west"->{a=new GridPoint(8,1,0);b=new GridPoint(0,1,8);}
            default->throw new IllegalArgumentException("Unsupported native rail shape");
        }
        first=a;last=b;firstOut=out(a);lastOut=out(b);var previous=sample(0).position;for(int i=1;i<distances.length;i++){var next=sample(i/128.0).position;distances[i]=distances[i-1]+next.sub(previous).length();previous=next;}
    }
    private static GridPoint out(GridPoint p){return new GridPoint(p.x()==0?-1:p.x()==16?1:0,0,p.z()==0?-1:p.z()==16?1:0);}
    public RailConnections.Endpoint endpoint(boolean end){return new RailConnections.Endpoint(Rotation.of(orientation).transform(end?last:first,pivot,origin),Rotation.of(orientation).apply(end?lastOut:firstOut));}
    public double length(){return distances[128];}
    public double distance(double t){double scaled=Math.clamp(t,0,1)*128;int i=Math.min(127,(int)scaled);return distances[i]+(distances[i+1]-distances[i])*(scaled-i);}
    public double parameter(double distance){double d=Math.clamp(distance,0,length());int i=Arrays.binarySearch(distances,d);if(i>=0)return i/128.0;i=Math.clamp(-i-2,0,127);return (i+(d-distances[i])/(distances[i+1]-distances[i]))/128;}
    public Sample sample(double t){t=Math.clamp(t,0,1);double x,y,z,dx,dy,dz;boolean curve=!shape.startsWith("ascending")&&!shape.equals("north_south")&&!shape.equals("east_west");
        if(curve){double cx=shape.endsWith("east")?16:0,cz=shape.startsWith("south")?16:0;double start=cx==16?Math.PI:0,delta=shape.equals("north_east")||shape.equals("south_west")?-Math.PI/2:Math.PI/2;double angle=start+delta*t;x=cx+8*Math.cos(angle);z=cz+8*Math.sin(angle);y=1;dx=-8*Math.sin(angle)*delta;dz=8*Math.cos(angle)*delta;dy=0;}
        else{x=first.x()+(last.x()-first.x())*t;z=first.z()+(last.z()-first.z())*t;y=first.y()+(last.y()-first.y())*(3*t*t-2*t*t*t);dx=last.x()-first.x();dz=last.z()-first.z();dy=(last.y()-first.y())*6*t*(1-t);}
        var point=rotate(new ConvexVolume.Vec(x-pivot.x(),y-pivot.y(),z-pivot.z())).add(new ConvexVolume.Vec(origin.x()+pivot.x(),origin.y()+pivot.y(),origin.z()+pivot.z()));var tangent=rotate(new ConvexVolume.Vec(dx,dy,dz));return new Sample(point,tangent.scale(1/tangent.length()));
    }
    private ConvexVolume.Vec rotate(ConvexVolume.Vec p){var r=Rotation.of(orientation);var x=r.apply(new GridPoint(1,0,0));var y=r.apply(new GridPoint(0,1,0));var z=r.apply(new GridPoint(0,0,1));return new ConvexVolume.Vec(x.x()*p.x()+y.x()*p.y()+z.x()*p.z(),x.y()*p.x()+y.y()*p.y()+z.y()*p.z(),x.z()*p.x()+y.z()*p.y()+z.z()*p.z());}
    public double nearest(ConvexVolume.Vec point){double best=0,distance=Double.POSITIVE_INFINITY;for(int i=0;i<=64;i++){double t=i/64.0,d=sample(t).position.sub(point).length();if(d<distance){best=t;distance=d;}}double lo=Math.max(0,best-1.0/64),hi=Math.min(1,best+1.0/64);for(int i=0;i<24;i++){double a=(2*lo+hi)/3,b=(lo+2*hi)/3;if(sample(a).position.sub(point).length()<sample(b).position.sub(point).length())hi=b;else lo=a;}return (lo+hi)/2;}
}
