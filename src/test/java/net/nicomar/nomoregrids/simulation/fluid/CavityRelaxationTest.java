package net.nicomar.nomoregrids.simulation.fluid;

import java.util.*;
import org.junit.jupiter.api.Test;
import net.nicomar.nomoregrids.core.CellPos;
import static org.junit.jupiter.api.Assertions.*;

class CavityRelaxationTest {
    @Test void stableWindowFinishesAfterOnePassWithoutPublishingPartialWork(){
        var pos=new CellPos(0,0,0);var dry=new CavityCell(new BitSet(),new byte[4096],CavityCell.Kind.EMPTY);
        var job=new CavityRelaxation(Map.of(pos,dry),Set.of(pos),CavityCell.Kind.WATER);
        assertFalse(job.advance(4095));assertThrows(IllegalStateException.class,job::result);
        assertTrue(job.advance(1));assertEquals(dry,job.result().get(pos));
    }
    @Test void persistedBoundaryDistancesFeedThenDrainAnIsolatedCorridor(){
        var left=new CellPos(-1,0,0);var right=new CellPos(0,0,0);byte[] water=new byte[4096];Arrays.fill(water,(byte)1);var source=new CavityCell(new BitSet(),water,CavityCell.Kind.WATER);
        var blocked=new BitSet(4096);blocked.set(0,4096);blocked.clear(0,16);var target=new CavityCell(blocked,new byte[4096],CavityCell.Kind.EMPTY);
        var job=new CavityRelaxation(Map.of(left,source,right,target),Set.of(right),CavityCell.Kind.WATER);while(!job.advance(4096)){}target=job.result().get(right);assertEquals(16,target.amount(CavityCell.Kind.WATER));assertEquals(16,target.distance(15));
        var dry=new CavityCell(new BitSet(),new byte[4096],CavityCell.Kind.EMPTY);for(int i=0;i<128&&target.amount(CavityCell.Kind.WATER)>0;i++){job=new CavityRelaxation(Map.of(left,dry,right,target),Set.of(right),CavityCell.Kind.WATER);while(!job.advance(4096)){}target=job.result().get(right);}assertEquals(0,target.amount(CavityCell.Kind.WATER));
    }
}
