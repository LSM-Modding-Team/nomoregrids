package net.nicomar.nomoregrids.simulation.fluid;
import java.util.*;
import net.nicomar.nomoregrids.core.Direction6;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CavityExportTest {
    @Test void onlyWetFacesExportAndNeverExportASource(){
        byte[] fluid=new byte[4096];fluid[15+16*(4+16*5)]=1;var cell=new CavityCell(new BitSet(),fluid,CavityCell.Kind.EMPTY);
        assertEquals(7,CavityExport.flow(cell,CavityCell.Kind.WATER,Direction6.EAST).orElseThrow().amount());
        assertTrue(CavityExport.flow(cell,CavityCell.Kind.WATER,Direction6.WEST).isEmpty());
        assertTrue(CavityExport.flow(cell,CavityCell.Kind.WATER,Direction6.UP).isEmpty());
        fluid[15+16*(4+16*5)]=0;fluid[2+16*(0+16*3)]=1;cell=new CavityCell(new BitSet(),fluid,CavityCell.Kind.EMPTY);
        var falling=CavityExport.flow(cell,CavityCell.Kind.WATER,Direction6.DOWN).orElseThrow();assertTrue(falling.falling());assertEquals(8,falling.amount());
        var wall=new BitSet();for(int y=0;y<16;y++)for(int z=0;z<16;z++)wall.set(15+16*(y+16*z));
        assertTrue(CavityExport.flow(new CavityCell(wall,fluid,CavityCell.Kind.EMPTY),CavityCell.Kind.WATER,Direction6.EAST).isEmpty());
    }
}
