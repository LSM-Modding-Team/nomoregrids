package net.nicomar.nomoregrids.simulation.fluid;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import net.nicomar.nomoregrids.core.CellPos;

class CavityCurrentsTest {
    @Test void currentUsesTheContactedDistanceGradientAndCannotFlowThroughWalls(){var mask=new BitSet(4096);mask.set(0,4096);var fluids=new byte[4096];var distance=new byte[4096];Arrays.fill(distance,(byte)127);for(int x=7;x<=9;x++){int i=x+16*(8+16*8);mask.clear(i);fluids[i]=1;distance[i]=(byte)(x+2);}var cell=new CavityCell(mask,fluids,CavityCell.Kind.EMPTY,distance);var flow=CavityCurrents.flow(new CellPos(0,0,0),cell,new CavityContacts.Body(8,8,8,9,9,9),CavityCell.Kind.WATER,p->null);assertEquals(1,flow.x(),1e-8);assertEquals(0,flow.y(),1e-8);assertEquals(0,flow.z(),1e-8);}
}
