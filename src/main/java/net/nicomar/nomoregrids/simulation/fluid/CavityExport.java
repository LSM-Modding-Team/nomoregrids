package net.nicomar.nomoregrids.simulation.fluid;

import java.util.Optional;
import net.nicomar.nomoregrids.core.Direction6;

/** Native outflow is driven only by wet, unobstructed microcells on the shared face. */
public final class CavityExport {
    public record Flow(CavityCell.Kind kind,int amount,boolean falling){}
    private CavityExport(){}
    public static Optional<Flow> flow(CavityCell cell,CavityCell.Kind kind,Direction6 direction){
        if(kind==CavityCell.Kind.EMPTY||direction==Direction6.UP)return Optional.empty();int nearest=127;
        for(int a=0;a<16;a++)for(int b=0;b<16;b++){
            int x=direction.x!=0?(direction.x>0?15:0):a;
            int y=direction.y!=0?(direction.y>0?15:0):direction.x!=0?a:b;
            int z=direction.z!=0?(direction.z>0?15:0):b;
            int index=x+16*(y+16*z);if(cell.at(index)==kind)nearest=Math.min(nearest,cell.distance(index));
        }
        if(nearest==127)return Optional.empty();if(direction==Direction6.DOWN)return Optional.of(new Flow(kind,8,true));
        int amount=(kind==CavityCell.Kind.WATER?7:3)-nearest/16;
        return amount>0?Optional.of(new Flow(kind,amount,false)):Optional.empty();
    }
}
