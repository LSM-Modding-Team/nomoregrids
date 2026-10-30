package net.nicomar.nomoregrids.geometry;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ConvexRaycastTest {
    @Test void eyeInsideUnionExitsAtOuterSurfaceInsteadOfInternalPrimitive(){
        var left=ConvexVolume.box(0,0,0,8,16,16);var right=ConvexVolume.box(8,0,0,16,16,16);
        var hit=ConvexRaycast.cast(List.of(left,right),new ConvexVolume.Vec(4,8,8),new ConvexVolume.Vec(1,0,0),32).orElseThrow();
        assertEquals(12,hit.distance16(),1e-9);assertEquals(1,hit.primitive());assertEquals(16,hit.point().x(),1e-9);
    }
    @Test void realGapKeepsFirstVisibleSurfaceAndReachIsEnforced(){
        var near=ConvexVolume.box(0,0,0,7,16,16);var far=ConvexVolume.box(8,0,0,16,16,16);
        var hit=ConvexRaycast.cast(List.of(near,far),new ConvexVolume.Vec(4,8,8),new ConvexVolume.Vec(1,0,0),32).orElseThrow();assertEquals(3,hit.distance16(),1e-9);
        assertTrue(ConvexRaycast.cast(List.of(far),new ConvexVolume.Vec(4,8,8),new ConvexVolume.Vec(1,0,0),3).isEmpty());
    }
}
