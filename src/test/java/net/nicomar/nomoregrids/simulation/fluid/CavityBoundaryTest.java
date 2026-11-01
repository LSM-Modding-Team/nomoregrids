package net.nicomar.nomoregrids.simulation.fluid;

import java.util.BitSet;
import net.nicomar.nomoregrids.core.Direction6;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CavityBoundaryTest {
    @Test void onlyMatchingAdjacentFluidHidesEachBoundaryFace(){
        byte[] water=new byte[4096];java.util.Arrays.fill(water,(byte)1);
        var full=new CavityCell(new BitSet(),water,CavityCell.Kind.WATER);
        var dry=new CavityCell(new BitSet(),new byte[4096],CavityCell.Kind.EMPTY);
        for(var d:Direction6.values()){
            int x=d.x<0?0:d.x>0?15:7,y=d.y<0?0:d.y>0?15:7,z=d.z<0?0:d.z>0?15:7;
            assertFalse(CavityBoundary.visible(full,full,CavityCell.Kind.WATER,x,y,z,d.x,d.y,d.z));
            assertTrue(CavityBoundary.visible(full,dry,CavityCell.Kind.WATER,x,y,z,d.x,d.y,d.z));
            assertTrue(CavityBoundary.visible(full,null,CavityCell.Kind.WATER,x,y,z,d.x,d.y,d.z));
        }
    }
}
