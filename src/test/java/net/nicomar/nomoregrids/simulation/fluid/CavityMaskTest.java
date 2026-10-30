package net.nicomar.nomoregrids.simulation.fluid;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.nicomar.nomoregrids.geometry.*;
import org.junit.jupiter.api.Test;

class CavityMaskTest {
    @Test void boxMaskMatchesIntegralMaskAndPublishesOnlyWhenComplete(){
        var job=new CavityMaskJob(List.of(ConvexVolume.box(0,0,0,16,8,16)));assertFalse(job.advance(128));assertThrows(IllegalStateException.class,job::result);
        while(!job.advance(128)){}assertEquals(CavityCell.blocked(ConstructionShapes.LOWER_SLAB),job.result());var copy=job.result();copy.clear();assertEquals(2048,job.result().cardinality());
    }
    @Test void slopeReservesIntersectedMicrocellsButKeepsItsEmptyCorner(){
        var slope=ConvexVolume.box(0,0,0,16,16,16).clipped(new ConvexVolume.Plane(new ConvexVolume.Vec(1,1,0),16));var job=new CavityMaskJob(List.of(slope));assertTrue(job.advance(4096));
        assertTrue(job.result().get(0));assertFalse(job.result().get(15+16*(15+16*15)));assertTrue(job.result().cardinality()<4096);
        assertEquals(7,slope.planes().size());
    }
}
