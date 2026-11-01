package net.nicomar.nomoregrids.simulation.fluid;

import java.util.Optional;

/** A height field is safe when wet columns share a solid floor and have no ceiling. */
public final class CavitySurface {
    private CavitySurface(){}
    public static Optional<int[]> heights(CavityCell cell,CavityCell.Kind kind){
        if(cell.amount(kind==CavityCell.Kind.WATER?CavityCell.Kind.LAVA:CavityCell.Kind.WATER)>0)return Optional.empty();
        int[] heights=new int[256];int commonFloor=-1;
        for(int z=0;z<16;z++)for(int x=0;x<16;x++){
            int floor=0;while(floor<16&&cell.at(x+16*(floor+16*z))!=kind)floor++;if(floor==16)continue;
            for(int y=0;y<floor;y++)if(cell.free(x+16*(y+16*z)))return Optional.empty();
            if(commonFloor<0)commonFloor=floor;else if(commonFloor!=floor)return Optional.empty();
            int h=floor;while(h<16&&cell.at(x+16*(h+16*z))==kind)h++;
            for(int y=h;y<16;y++){int i=x+16*(y+16*z);if(cell.at(i)==kind||!cell.free(i))return Optional.empty();}
            heights[x+16*z]=h;
        }return Optional.of(heights);
    }
}
