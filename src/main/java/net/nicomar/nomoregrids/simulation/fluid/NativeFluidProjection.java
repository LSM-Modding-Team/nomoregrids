package net.nicomar.nomoregrids.simulation.fluid;

import java.util.Optional;
import net.nicomar.nomoregrids.simulation.fluid.CavityCell.Kind;

/** Only lossless representations of the native fluid image are transferable. */
public final class NativeFluidProjection {
    public record State(Kind kind,int amount,boolean source){}
    private NativeFluidProjection(){}
    public static Optional<State> exact(CavityCell cell){
        if(!cell.blocked().isEmpty())return Optional.empty();
        Kind kind=cell.amount(Kind.WATER)>0?Kind.WATER:cell.amount(Kind.LAVA)>0?Kind.LAVA:Kind.EMPTY;
        if(kind==Kind.EMPTY||cell.amount(kind==Kind.WATER?Kind.LAVA:Kind.WATER)>0)return Optional.empty();
        if(cell.source()!=Kind.EMPTY)return Optional.of(new State(kind,8,true));
        // Non-falling flowing amount 8 serializes to legacy level 0 (a source).
        for(int amount=1;amount<=7;amount++){
            int height=amount*2,distance=Math.min(kind==Kind.WATER?112:48,(8-amount)*16);boolean matches=true;
            for(int i=0;i<4096;i++){boolean wet=i/16%16<height;if(cell.at(i)!=(wet?kind:Kind.EMPTY)||cell.distance(i)!=(wet?distance:127)){matches=false;break;}}
            if(matches)return Optional.of(new State(kind,amount,false));
        }
        return Optional.empty();
    }
}
