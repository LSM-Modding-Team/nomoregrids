package net.nicomar.nomoregrids.simulation.fluid;

import java.util.Optional;

/** A height field is safe only for unobstructed, bottom-connected columns. */
public final class CavitySurface {
    private CavitySurface(){}
    public static Optional<int[]> heights(CavityCell cell,CavityCell.Kind kind){
        if(!cell.blocked().isEmpty()||cell.amount(kind==CavityCell.Kind.WATER?CavityCell.Kind.LAVA:CavityCell.Kind.WATER)>0)return Optional.empty();
        int[] heights=new int[256];
        for(int z=0;z<16;z++)for(int x=0;x<16;x++){
            int h=0;while(h<16&&cell.at(x+16*(h+16*z))==kind)h++;
            for(int y=h;y<16;y++)if(cell.at(x+16*(y+16*z))==kind)return Optional.empty();
            heights[x+16*z]=h;
        }return Optional.of(heights);
    }
}
