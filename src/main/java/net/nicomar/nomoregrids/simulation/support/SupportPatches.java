package net.nicomar.nomoregrids.simulation.support;

import java.util.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.*;

/** Anchor contracts in source-local coordinates, transformed once with the piece. */
public final class SupportPatches {
    private SupportPatches(){}
    public static List<SupportResolver.Patch> required(PieceRecord piece){
        String id=piece.sourceBlockId().value();List<SupportResolver.Patch> local=new ArrayList<>();
        if(id.endsWith("_door")&&"lower".equals(piece.sourceState().get("half")))local.add(new SupportResolver.Patch(Direction6.DOWN,0,0,0,16,16));
        else if(id.endsWith("_bed")){for(var face:BoxMesh.extract(piece.geometry().collision(),4096))if(face.normal()==Direction6.DOWN&&face.plane()==0)local.add(patch(face));}
        else if(id.endsWith("_carpet")||id.equals("minecraft:snow")){
            for(var face:BoxMesh.extract(piece.geometry().occupation(),4096))if(face.normal()==Direction6.DOWN&&face.plane()==piece.geometry().occupation().bounds().min().y())local.add(patch(face));
        }else if(id.equals("minecraft:torch")||id.equals("minecraft:soul_torch")||id.equals("minecraft:redstone_torch"))local.add(new SupportResolver.Patch(Direction6.DOWN,0,6,6,10,10));
        else if(id.endsWith("wall_torch")){
            Direction6 facing=Direction6.valueOf(piece.sourceState().getOrDefault("facing","north").toUpperCase(Locale.ROOT));Direction6 anchor=facing.opposite();
            local.add(new SupportResolver.Patch(anchor,anchor.sign()>0?16:0,6,3,10,13));
        }else if(id.equals("minecraft:lantern")||id.equals("minecraft:soul_lantern")){
            boolean hanging=Boolean.parseBoolean(piece.sourceState().getOrDefault("hanging","false"));local.add(new SupportResolver.Patch(hanging?Direction6.UP:Direction6.DOWN,hanging?16:0,6,6,10,10));
        }else if(id.endsWith("_wall_sign")||id.endsWith("_wall_banner")){
            Direction6 anchor=Direction6.valueOf(piece.sourceState().getOrDefault("facing","north").toUpperCase(Locale.ROOT)).opposite();local.add(new SupportResolver.Patch(anchor,anchor.sign()>0?16:0,7,7,9,9));
        }else if(id.endsWith("_hanging_sign"))local.add(new SupportResolver.Patch(Direction6.UP,16,6,6,10,10));
        else if(id.endsWith("_sign")||id.endsWith("_banner"))local.add(new SupportResolver.Patch(Direction6.DOWN,0,7,7,9,9));
        else if(id.equals("minecraft:ladder")){
            Direction6 anchor=Direction6.valueOf(piece.sourceState().getOrDefault("facing","north").toUpperCase(Locale.ROOT)).opposite();
            local.add(new SupportResolver.Patch(anchor,anchor.sign()>0?16:0,2,2,14,6));local.add(new SupportResolver.Patch(anchor,anchor.sign()>0?16:0,2,10,14,14));
        }
        return local.stream().map(p->transform(p,piece)).toList();
    }
    public static List<List<SupportResolver.Patch>> alternatives(PieceRecord p){
        if(!p.sourceBlockId().value().endsWith("_wall_hanging_sign"))return List.of(required(p));
        Direction6 facing=Direction6.valueOf(p.sourceState().getOrDefault("facing","north").toUpperCase(Locale.ROOT));Direction6 side=facing.axis()==0?Direction6.NORTH:Direction6.EAST;
        return List.of(List.of(transform(new SupportResolver.Patch(side,side.sign()>0?16:0,6,14,10,16),p)),List.of(transform(new SupportResolver.Patch(side.opposite(),side.sign()>0?0:16,6,14,10,16),p)));
    }
    private static SupportResolver.Patch patch(BoxMesh.Face face){var r=face.rect();return new SupportResolver.Patch(face.normal(),face.plane(),r.u0(),r.v0(),r.u1(),r.v1());}
    public static List<SupportResolver.Patch> surfaces(BoxUnion shape){return BoxMesh.extract(shape,4096).stream().map(SupportPatches::patch).toList();}
    public static SupportResolver.Patch transform(SupportResolver.Patch patch,PieceRecord piece){
        Direction6 normal=patch.normal().rotated(piece.orientation());long u0=Long.MAX_VALUE,v0=Long.MAX_VALUE,u1=Long.MIN_VALUE,v1=Long.MIN_VALUE,plane=0;
        for(long u:new long[]{patch.u0(),patch.u1()})for(long v:new long[]{patch.v0(),patch.v1()}){
            GridPoint local=point(patch.normal().axis(),patch.plane(),u,v);GridPoint p=Rotation.of(piece.orientation()).transform(local,piece.pivot16(),piece.origin16());
            long a=normal.axis()==0?p.z():p.x(),b=normal.axis()==1?p.z():p.y();plane=normal.axis()==0?p.x():normal.axis()==1?p.y():p.z();u0=Math.min(u0,a);u1=Math.max(u1,a);v0=Math.min(v0,b);v1=Math.max(v1,b);
        }
        return new SupportResolver.Patch(normal,plane,u0,v0,u1,v1);
    }
    private static GridPoint point(int axis,long plane,long u,long v){return axis==0?new GridPoint(plane,v,u):axis==1?new GridPoint(u,plane,v):new GridPoint(u,v,plane);}
    /** One quantum immediately outside the anchor face. This never bridges a gap. */
    public static Box16 contactCells(SupportResolver.Patch p){
        long lo=p.normal().sign()<0?Math.subtractExact(p.plane(),1):p.plane(),hi=Math.addExact(lo,1);
        return p.normal().axis()==0?Box16.of(lo,p.v0(),p.u0(),hi,p.v1(),p.u1()):p.normal().axis()==1?Box16.of(p.u0(),lo,p.v0(),p.u1(),hi,p.v1()):Box16.of(p.u0(),p.v0(),lo,p.u1(),p.v1(),hi);
    }
}
