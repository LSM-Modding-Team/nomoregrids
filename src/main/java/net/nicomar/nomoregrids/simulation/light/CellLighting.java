package net.nicomar.nomoregrids.simulation.light;

import java.util.*;
import net.nicomar.nomoregrids.geometry.Box16;
import net.nicomar.nomoregrids.material.MaterialProfile;

/** Vanilla-resolution light aggregation. Input boxes are clipped to this cell, in q units. */
public final class CellLighting {
    public record Part(List<Box16> boxes,MaterialProfile material){public Part{boxes=List.copyOf(boxes);}}
    public record Light(int emission,int attenuation,boolean sealed){}
    private CellLighting(){}
    public static Light calculate(Collection<Part> pieces){
        BitSet opaque=new BitSet(4096);int emission=0,partial=0;
        for(var p:pieces){
            if(p.boxes.isEmpty())continue;emission=Math.max(emission,p.material.emission());
            if(!p.material.opaque())partial=Math.max(partial,p.material.attenuation());
            if(p.material.opaque())for(var b:p.boxes){
                if(b.min().x()<0||b.min().y()<0||b.min().z()<0||b.max().x()>16||b.max().y()>16||b.max().z()>16)throw new IllegalArgumentException("Box not clipped to cell");
                for(int z=(int)b.min().z();z<b.max().z();z++)for(int y=(int)b.min().y();y<b.max().y();y++)
                    opaque.set((int)b.min().x()+16*(y+16*z),(int)b.max().x()+16*(y+16*z));
            }
        }
        boolean sealed=opaque.cardinality()==4096;return new Light(emission,sealed?15:partial,sealed);
    }
}
