package net.nicomar.nomoregrids.compat.vanilla;

import java.util.*;
import net.nicomar.nomoregrids.core.Direction6;
import net.nicomar.nomoregrids.geometry.*;

/** Local family geometry, with UV units preserved by the mesh builder. */
public final class FamilyShapes {
    public enum Family { CUBE,SLAB,STAIR,PANEL,BARS,FENCE,WALL,CARPET,SNOW,LADDER,CHAIN,TORCH,LANTERN,TRAPDOOR,SIGN }
    private FamilyShapes(){}
    public static BoxUnion shape(Family family,int length,Set<Direction6> connections){
        if(length<1||length>16)throw new IllegalArgumentException("Invalid local height");
        var boxes=new ArrayList<Box16>();
        switch(family){
            case CUBE -> {return ConstructionShapes.CUBE;}
            case SLAB -> {return ConstructionShapes.LOWER_SLAB;}
            case STAIR -> {return ConstructionShapes.STRAIGHT_STAIR;}
            case PANEL,BARS -> {boxes.add(Box16.of(7,0,7,9,length,9));arms(boxes,connections,7,9,0,length);}
            case FENCE -> {boxes.add(Box16.of(6,0,6,10,length,10));arms(boxes,connections,7,9,Math.min(6,length-1),Math.min(9,length));if(length>12)arms(boxes,connections,7,9,12,Math.min(15,length));}
            case WALL -> {boxes.add(Box16.of(4,0,4,12,length,12));arms(boxes,connections,5,11,0,Math.min(14,length));}
            case CARPET,SNOW -> boxes.add(Box16.of(0,0,0,16,length,16));
            case LADDER -> boxes.add(Box16.of(0,0,14,16,16,16));
            case CHAIN -> boxes.add(Box16.of(6,0,6,10,16,10));
            case TORCH -> boxes.add(Box16.of(6,0,6,10,10,10));
            case LANTERN -> boxes.add(Box16.of(5,0,5,11,9,11));
            case TRAPDOOR -> boxes.add(Box16.of(0,0,0,16,3,16));
            case SIGN -> {boxes.add(Box16.of(1,7,7,15,16,9));boxes.add(Box16.of(7,0,7,9,7,9));}
        }
        return new BoxUnion(boxes);
    }
    private static void arms(List<Box16> boxes,Set<Direction6> connections,int low,int high,int y0,int y1){
        if(connections.contains(Direction6.NORTH))boxes.add(Box16.of(low,y0,0,high,y1,low));
        if(connections.contains(Direction6.SOUTH))boxes.add(Box16.of(low,y0,high,high,y1,16));
        if(connections.contains(Direction6.WEST))boxes.add(Box16.of(0,y0,low,low,y1,high));
        if(connections.contains(Direction6.EAST))boxes.add(Box16.of(high,y0,low,16,y1,high));
    }
}
