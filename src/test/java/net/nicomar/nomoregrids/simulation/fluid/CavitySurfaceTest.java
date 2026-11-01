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
        data[2+16*(7+16*3)]=0;var blocked=new BitSet();blocked.set(0);assertTrue(CavitySurface.heights(new CavityCell(blocked,data,CavityCell.Kind.EMPTY),CavityCell.Kind.WATER).isEmpty());
    }
}
