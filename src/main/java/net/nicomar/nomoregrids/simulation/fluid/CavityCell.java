package net.nicomar.nomoregrids.simulation.fluid;

import java.util.*;
import net.nicomar.nomoregrids.geometry.*;

/** Conservative 16^3 cavity mask. This approximation is used for fluid only. */
public final class CavityCell {
    public enum Kind { EMPTY,WATER,LAVA }
    private final BitSet blocked;
    private final byte[] fluid;
    private final Kind source;
    public CavityCell(BitSet blocked,byte[] fluid,Kind source){
        if(blocked.length()>4096||fluid.length!=4096)throw new IllegalArgumentException("Invalid cavity cell");
        this.blocked=(BitSet)blocked.clone();this.fluid=fluid.clone();this.source=Objects.requireNonNull(source);
        for(int i=0;i<4096;i++)if(fluid[i]<0||fluid[i]>2||blocked.get(i)&&fluid[i]!=0)throw new IllegalArgumentException("Fluid intersects solid");
        if(source!=Kind.EMPTY){if(!blocked.isEmpty())throw new IllegalArgumentException("Fractional source");for(byte b:fluid)if(b!=code(source))throw new IllegalArgumentException("Incomplete source");}
    }
    public static byte code(Kind kind){return switch(kind){case EMPTY->0;case WATER->1;case LAVA->2;};}
    public static Kind kind(byte code){return switch(code){case 0->Kind.EMPTY;case 1->Kind.WATER;case 2->Kind.LAVA;default->throw new IllegalArgumentException("Fluid ID");};}
    public static BitSet blocked(BoxUnion localSolid){
        BitSet mask=new BitSet(4096);
        for(var box:localSolid.boxes()){
            if(box.min().x()<0||box.min().y()<0||box.min().z()<0||box.max().x()>16||box.max().y()>16||box.max().z()>16)throw new IllegalArgumentException("Unclipped solid");
            for(int z=(int)box.min().z();z<box.max().z();z++)for(int y=(int)box.min().y();y<box.max().y();y++)
                mask.set((int)box.min().x()+16*(y+16*z),(int)box.max().x()+16*(y+16*z));
        }
        return mask;
    }
    public boolean free(int index){return index>=0&&index<4096&&!blocked.get(index);}
    public Kind at(int index){return kind(fluid[index]);}
    public Kind source(){return source;}
    public BitSet blocked(){return (BitSet)blocked.clone();}
    public byte[] fluids(){return fluid.clone();}
    @Override public boolean equals(Object other){return other instanceof CavityCell cell&&source==cell.source&&blocked.equals(cell.blocked)&&Arrays.equals(fluid,cell.fluid);}
    @Override public int hashCode(){return Objects.hash(blocked,source,Arrays.hashCode(fluid));}
    public int amount(Kind kind){int n=0;for(byte b:fluid)if(b==code(kind))n++;return n;}
    public CavityCell bucket(Kind kind,boolean collect){
        if(kind==Kind.EMPTY||!blocked.isEmpty())throw new IllegalArgumentException("Bucket needs a full cavity");
        if(collect){if(source!=kind||amount(kind)!=4096)throw new IllegalArgumentException("Not an accessible complete source");return new CavityCell(blocked,new byte[4096],Kind.EMPTY);}
        if(amount(Kind.EMPTY)!=4096)throw new IllegalArgumentException("Bucket cavity occupied");
        byte[] next=new byte[4096];Arrays.fill(next,code(kind));return new CavityCell(blocked,next,kind);
    }
}
