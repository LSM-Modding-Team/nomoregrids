package net.nicomar.nomoregrids.geometry;

import java.util.*;
import net.nicomar.nomoregrids.core.*;

/** Union creases. Splits collinear edges before cancelling coplanar seams, including T junctions. */
public final class BoxOutline {
    public record Edge(GridPoint a,GridPoint b){}
    private record Line(int axis,long first,long second){}
    private BoxOutline(){}
    public static List<Edge> extract(BoxUnion boxes,int budget){
        var lines=new LinkedHashMap<Line,TreeMap<Long,Integer>>();
        for(var face:BoxMesh.extract(boxes,budget)){
            var r=face.rect();var corners=List.of(point(face,r.u0(),r.v0()),point(face,r.u1(),r.v0()),point(face,r.u1(),r.v1()),point(face,r.u0(),r.v1()));
            for(int i=0;i<4;i++){
                var a=corners.get(i);var b=corners.get((i+1)%4);int axis=a.x()!=b.x()?0:a.y()!=b.y()?1:2;
                var key=new Line(axis,axis==0?a.y():a.x(),axis==2?a.y():a.z());
                var events=lines.computeIfAbsent(key,k->new TreeMap<>());long lo=Math.min(coord(a,axis),coord(b,axis)),hi=Math.max(coord(a,axis),coord(b,axis));int mask=1<<face.normal().ordinal();
                events.merge(lo,mask,(x,y)->x^y);events.merge(hi,mask,(x,y)->x^y);
                if(lines.size()>budget)throw new IllegalArgumentException("Outline budget");
            }
        }
        var out=new ArrayList<Edge>();for(var entry:lines.entrySet()){
            int mask=0;Long start=null;var events=entry.getValue();for(var event:events.entrySet()){
                int next=mask^event.getValue();if(mask==0&&next!=0)start=event.getKey();
                if(mask!=0&&next==0){out.add(new Edge(on(entry.getKey(),start),on(entry.getKey(),event.getKey())));if(out.size()>budget)throw new IllegalArgumentException("Outline budget");start=null;}
                mask=next;
            }
        }return List.copyOf(out);
    }
    private static long coord(GridPoint p,int a){return a==0?p.x():a==1?p.y():p.z();}
    private static GridPoint on(Line line,long value){return line.axis==0?new GridPoint(value,line.first,line.second):line.axis==1?new GridPoint(line.first,value,line.second):new GridPoint(line.first,line.second,value);}
    private static GridPoint point(BoxMesh.Face f,long u,long v){return f.normal().axis()==0?new GridPoint(f.plane(),v,u):f.normal().axis()==1?new GridPoint(u,f.plane(),v):new GridPoint(u,v,f.plane());}
}
