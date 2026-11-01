package net.nicomar.nomoregrids.geometry;

import java.util.*;

/** Lossless greedy packing of integral 16³ masks used only for conservative public query boxes. */
public final class MicroBoxes {
    private MicroBoxes(){}
    public static BitSet mask(BoxUnion shape){var result=new BitSet(4096);for(var b:shape.boxes()){if(b.min().x()<0||b.min().y()<0||b.min().z()<0||b.max().x()>16||b.max().y()>16||b.max().z()>16)throw new IllegalArgumentException("Unclipped geometry");for(int z=(int)b.min().z();z<b.max().z();z++)for(int y=(int)b.min().y();y<b.max().y();y++)result.set((int)b.min().x()+16*(y+16*z),(int)b.max().x()+16*(y+16*z));}return result;}
    public static BoxUnion pack(BitSet input){if(input.length()>4096)throw new IllegalArgumentException("Micro mask");var remaining=(BitSet)input.clone();var boxes=new ArrayList<Box16>();
        for(int i=remaining.nextSetBit(0);i>=0;i=remaining.nextSetBit(0)){int x=i%16,y=i/16%16,z=i/256,x1=x+1,y1=y+1,z1=z+1;while(x1<16&&remaining.get(x1+16*(y+16*z)))x1++;
            outer:while(y1<16){for(int a=x;a<x1;a++)if(!remaining.get(a+16*(y1+16*z)))break outer;y1++;}
            outer:while(z1<16){for(int b=y;b<y1;b++)for(int a=x;a<x1;a++)if(!remaining.get(a+16*(b+16*z1)))break outer;z1++;}
            for(int c=z;c<z1;c++)for(int b=y;b<y1;b++)remaining.clear(x+16*(b+16*c),x1+16*(b+16*c));boxes.add(Box16.of(x,y,z,x1,y1,z1));
        }return new BoxUnion(boxes);
    }
}
