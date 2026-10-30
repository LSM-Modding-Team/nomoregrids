package net.nicomar.nomoregrids.simulation.connection;

import java.util.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.Rotation;
import net.nicomar.nomoregrids.simulation.support.*;

/** Geometric ports for orthogonal construction families, independent of world cell ownership. */
public final class FamilyPorts {
    public enum Family { PANEL,FENCE,WALL,STAIR,NONE }
    public record Port(Direction6 local,GridPoint point,Direction6 normal,Direction6 up,int width,int height,String kind,SupportResolver.Patch area){}
    private FamilyPorts(){}
    public static Family family(PieceRecord p){
        if(!p.adapterId().value().equals("nomoregrids:box")||!p.sourceBlockId().value().startsWith("minecraft:")||p.flags().contains(PieceRecord.Flag.SUBSTITUTE))return Family.NONE;
        String id=p.sourceBlockId().value();return id.endsWith("_pane")||id.equals("minecraft:glass_pane")||id.equals("minecraft:iron_bars")?Family.PANEL:id.endsWith("_fence")?Family.FENCE:id.endsWith("_wall")?Family.WALL:id.endsWith("_stairs")?Family.STAIR:Family.NONE;
    }
    public static List<Port> ports(PieceRecord p){
        Family family=family(p);if(family==Family.NONE||family==Family.STAIR)return List.of();
        int height=family==Family.PANEL?length(p):family==Family.WALL?14:16,width=family==Family.WALL?6:2;
        String kind=family==Family.FENCE?(p.sourceBlockId().value().equals("minecraft:nether_brick_fence")?"nether_fence":"wood_fence"):family.name();
        List<Port> result=new ArrayList<>();for(var side:List.of(Direction6.NORTH,Direction6.SOUTH,Direction6.WEST,Direction6.EAST)){
            GridPoint local=new GridPoint(8+side.x*8,0,8+side.z*8),point=Rotation.of(p.orientation()).transform(local,p.pivot16(),p.origin16());
            var area=SupportPatches.transform(new SupportResolver.Patch(side,side.sign()>0?16:0,8-width/2,0,8+width/2,height),p);
            result.add(new Port(side,point,side.rotated(p.orientation()),Direction6.UP.rotated(p.orientation()),width,height,kind,area));
        }return List.copyOf(result);
    }
    // Decode the small fragment descriptor without importing any platform item classes.
    private static int length(PieceRecord p){byte[] bytes=p.itemPayload().copy();if(bytes.length==6&&bytes[0]=='N'&&bytes[1]=='M'&&bytes[2]=='G'&&bytes[3]=='F'&&bytes[4]==1){int n=Byte.toUnsignedInt(bytes[5]);if(n>=1&&n<=16)return n;}return 16;}
    public static boolean match(Port a,Port b){return a.point.equals(b.point)&&a.normal==b.normal.opposite()&&a.up==b.up&&a.width==b.width&&a.height==b.height&&a.kind.equals(b.kind);}
    public static GridPoint center(PieceRecord p){return p.origin16().add(p.pivot16()).add(Rotation.of(p.orientation()).apply(GridPoint.BLOCK_CENTER.subtract(p.pivot16())));}
    public static Direction6 front(PieceRecord p){return Direction6.valueOf(p.sourceState().getOrDefault("facing","north").toUpperCase(Locale.ROOT)).rotated(p.orientation());}
    public static Direction6 stairUp(PieceRecord p){return (p.sourceState().getOrDefault("half","bottom").equals("top")?Direction6.DOWN:Direction6.UP).rotated(p.orientation());}
}
