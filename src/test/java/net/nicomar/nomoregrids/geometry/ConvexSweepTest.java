package net.nicomar.nomoregrids.geometry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConvexSweepTest {
    @Test void extrusionRetainsSlopeAndNewEdgeFacets(){
        var wedge=ConvexVolume.box(0,0,0,16,16,16).clipped(new ConvexVolume.Plane(new ConvexVolume.Vec(1,0,1),16));var moved=wedge.swept(new ConvexVolume.Vec(16,0,0));
        assertEquals(ConvexVolume.Contact.DISJOINT,moved.contact(ConvexVolume.box(27,2,11,29,4,13)));
        assertEquals(ConvexVolume.Contact.OVERLAP,moved.contact(ConvexVolume.box(19,2,3,21,4,5)));
        assertEquals(ConvexVolume.Contact.DISJOINT,moved.contact(ConvexVolume.box(1,17,1,2,18,2)));
    }
}
