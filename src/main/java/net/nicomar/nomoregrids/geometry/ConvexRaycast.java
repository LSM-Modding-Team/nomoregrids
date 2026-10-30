package net.nicomar.nomoregrids.geometry;

import java.util.*;

/** Ray against a union of convexes. Merges intervals so internal primitive faces are never selected. */
public final class ConvexRaycast {
    public record Hit(double distance16,int primitive,int plane,ConvexVolume.Vec point){}
    private record Span(double near,double far,int entering,int enterPlane,int exiting,int exitPlane){}
    private ConvexRaycast(){}
    public static Optional<Hit> cast(List<ConvexVolume> volumes,ConvexVolume.Vec origin,ConvexVolume.Vec direction,double reach){
        if(volumes.size()>16384||direction.length()==0||reach<0||!Double.isFinite(reach))throw new IllegalArgumentException("Convex ray budget/direction");var spans=new ArrayList<Span>();
        for(int i=0;i<volumes.size();i++){var interval=volumes.get(i).interval(origin,direction,reach);if(interval.isPresent()){var s=interval.orElseThrow();spans.add(new Span(s.near(),s.far(),i,s.enterPlane(),i,s.exitPlane()));}}
        spans.sort(Comparator.comparingDouble(Span::near).thenComparingInt(Span::entering));Span current=null;
        for(var next:spans){
            if(current==null){current=next;continue;}
            if(next.near<=current.far+ConvexVolume.EPS){if(next.far>current.far)current=new Span(current.near,next.far,current.entering,current.enterPlane,next.exiting,next.exitPlane);}
            else{var hit=hit(current,origin,direction,reach);if(hit.isPresent())return hit;current=next;}
        }return current==null?Optional.empty():hit(current,origin,direction,reach);
    }
    private static Optional<Hit> hit(Span span,ConvexVolume.Vec origin,ConvexVolume.Vec direction,double reach){
        boolean entry=span.near>=0;double distance=entry?span.near:span.far;if(distance<0||distance>reach)return Optional.empty();return Optional.of(new Hit(distance,entry?span.entering:span.exiting,entry?span.enterPlane:span.exitPlane,origin.add(direction.scale(distance/direction.length()))));
    }
}
