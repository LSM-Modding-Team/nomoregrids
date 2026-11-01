package net.nicomar.nomoregrids.simulation.fluid;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CavitySurfaceTest {
    @Test void onlyUnobstructedBottomConnectedColumnsCanBeSmoothed(){
        byte[] data=new byte[4096];for(int y=0;y<5;y++)data[2+16*(y+16*3)]=1;
        var flat=new CavityCell(new BitSet(),data,CavityCell.Kind.EMPTY);
        assertEquals(5,CavitySurface.heights(flat,CavityCell.Kind.WATER).orElseThrow()[2+16*3]);
        data[2+16*(7+16*3)]=1;assertTrue(CavitySurface.heights(new CavityCell(new BitSet(),data,CavityCell.Kind.EMPTY),CavityCell.Kind.WATER).isEmpty());
        data[2+16*(7+16*3)]=0;var blocked=new BitSet();blocked.set(2+16*(8+16*3));assertTrue(CavitySurface.heights(new CavityCell(blocked,data,CavityCell.Kind.EMPTY),CavityCell.Kind.WATER).isEmpty());
    }
    @Test void waterAboveAUniformSolidFloorCanBeSmoothed(){
        var blocked=new BitSet();byte[] data=new byte[4096];for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int y=0;y<16;y++){int i=x+16*(y+16*z);if(y<8)blocked.set(i);else if(y<12)data[i]=1;}
        var cell=new CavityCell(blocked,data,CavityCell.Kind.EMPTY);assertEquals(12,CavitySurface.heights(cell,CavityCell.Kind.WATER).orElseThrow()[0]);
    }
}
