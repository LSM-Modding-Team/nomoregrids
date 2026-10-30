package net.nicomar.nomoregrids.client.render;

import java.util.*;
import net.nicomar.nomoregrids.core.Direction6;
import net.nicomar.nomoregrids.geometry.*;

/** Exact union surface extraction; rect subtraction preserves original UV scale and material data. */
public final class BoxMesh {
    public record Rect(long u0,long v0,long u1,long v1){}
    public record Face(Direction6 normal,long plane,Rect rect){}
    private BoxMesh(){}
    public static List<Face> extract(BoxUnion shape,int maxQuads){
        var result=new ArrayList<Face>();var boxes=shape.boxes();
        for(int i=0;i<boxes.size();i++)for(var normal:Direction6.values()){
            Box16 b=boxes.get(i);int axis=normal.axis();long plane=coordinate(normal.sign()>0?b.max():b.min(),axis);
            List<Rect> visible=new ArrayList<>(List.of(rect(b,axis)));
            for(int j=0;j<boxes.size()&&!visible.isEmpty();j++)if(i!=j){
                Box16 o=boxes.get(j);long lo=coordinate(o.min(),axis),hi=coordinate(o.max(),axis);
                boolean blocks=normal.sign()>0?lo<=plane&&hi>plane:lo<plane&&hi>=plane;
                boolean duplicate=j<i&&(normal.sign()>0?hi==plane&&lo<plane:lo==plane&&hi>plane);
                if(!blocks&&!duplicate)continue;
                var next=new ArrayList<Rect>();for(var r:visible)next.addAll(subtract(r,rect(o,axis)));
                if(next.size()+result.size()>maxQuads)throw new IllegalArgumentException("Mesh complexity limit");visible=next;
            }
            for(var r:visible){if(result.size()>=maxQuads)throw new IllegalArgumentException("Mesh complexity limit");result.add(new Face(normal,plane,r));}
        }
        return List.copyOf(result);
    }
    public static List<Rect> subtract(Rect a,Rect b){
        long u0=Math.max(a.u0,b.u0),v0=Math.max(a.v0,b.v0),u1=Math.min(a.u1,b.u1),v1=Math.min(a.v1,b.v1);
        if(u0>=u1||v0>=v1)return List.of(a);
        var out=new ArrayList<Rect>(4);
        if(a.u0<u0)out.add(new Rect(a.u0,a.v0,u0,a.v1));if(u1<a.u1)out.add(new Rect(u1,a.v0,a.u1,a.v1));
        if(a.v0<v0)out.add(new Rect(u0,a.v0,u1,v0));if(v1<a.v1)out.add(new Rect(u0,v1,u1,a.v1));return out;
    }
    private static long coordinate(net.nicomar.nomoregrids.core.GridPoint p,int axis){return axis==0?p.x():axis==1?p.y():p.z();}
    private static Rect rect(Box16 b,int axis){
        return axis==0?new Rect(b.min().z(),b.min().y(),b.max().z(),b.max().y()):axis==1?
            new Rect(b.min().x(),b.min().z(),b.max().x(),b.max().z()):new Rect(b.min().x(),b.min().y(),b.max().x(),b.max().y());
    }
}
