package net.nicomar.nomoregrids.simulation.fluid;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.nicomar.nomoregrids.core.CellPos;
import net.nicomar.nomoregrids.simulation.fluid.CavityCell.Kind;
import org.junit.jupiter.api.Test;

class CavityTest {
    @Test void bucketCannotRecoverPartialWater(){
        var empty=new CavityCell(new BitSet(),new byte[4096],Kind.EMPTY);var source=empty.bucket(Kind.WATER,false);
        assertEquals(4096,source.amount(Kind.WATER));assertEquals(0,source.bucket(Kind.WATER,true).amount(Kind.WATER));
        byte[] partial=new byte[4096];partial[0]=1;var fragment=new CavityCell(new BitSet(),partial,Kind.EMPTY);
        assertThrows(IllegalArgumentException.class,()->fragment.bucket(Kind.WATER,true));
    }
    @Test void redistributionConservesVolumeAndLeavesCallerUntouched(){
        byte[] fluids=new byte[4096];fluids[0]=1;var old=new CavityCell(new BitSet(),fluids,Kind.EMPTY);var pos=new CellPos(0,0,0);
        BitSet obstacle=new BitSet();obstacle.set(0);var next=CavitySimulation.displace(Map.of(pos,old),Map.of(pos,obstacle),100);
        assertEquals(1,next.get(pos).amount(Kind.WATER));assertEquals(Kind.EMPTY,next.get(pos).at(0));assertEquals(Kind.WATER,old.at(0));
    }
    @Test void spentBudgetDoesNotPublishPartialDrain(){
        var source=new CavityCell(new BitSet(),new byte[4096],Kind.EMPTY).bucket(Kind.WATER,false);
        var job=new CavitySimulation.FlowJob(Map.of(new CellPos(0,0,0),source));assertFalse(job.advance(1));
        assertThrows(IllegalStateException.class,job::result);
        while(!job.advance(8192)){}assertEquals(4096,job.result().get(new CellPos(0,0,0)).amount(Kind.WATER));
    }
    @Test void reactionChecksTheTouchingFaceInEveryDirection(){
        var center=new CellPos(0,0,0);var lava=new CavityCell(new BitSet(),new byte[4096],Kind.EMPTY).bucket(Kind.LAVA,false);
        for(var direction:net.nicomar.nomoregrids.core.Direction6.values()){
            var neighbor=new CellPos(direction.x,direction.y,direction.z);byte[] water=new byte[4096];
            int x=direction.x!=0?(direction.x>0?0:15):7,y=direction.y!=0?(direction.y>0?0:15):7,z=direction.z!=0?(direction.z>0?0:15):7;
            water[x+16*(y+16*z)]=1;var touching=new CavityCell(new BitSet(),water,Kind.EMPTY);
            assertTrue(CavitySimulation.react(Map.of(center,lava,neighbor,touching)).products().containsKey(center));
            Arrays.fill(water,(byte)0);water[7+16*(7+16*7)]=1;var separated=new CavityCell(new BitSet(),water,Kind.EMPTY);
            assertFalse(CavitySimulation.react(Map.of(center,lava,neighbor,separated)).products().containsKey(center));
        }
    }
    @Test void amountsIncludeBlockedEmptyMicrocells(){
        var blocked=new BitSet(4096);blocked.set(5);byte[] fluids=new byte[4096];fluids[0]=1;fluids[1]=2;
        var cell=new CavityCell(blocked,fluids,Kind.EMPTY);assertEquals(1,cell.amount(Kind.WATER));assertEquals(1,cell.amount(Kind.LAVA));assertEquals(4094,cell.amount(Kind.EMPTY));
    }
}
