package net.nicomar.nomoregrids.geometry;

import static org.junit.jupiter.api.Assertions.*;
import net.nicomar.nomoregrids.geometry.ConvexVolume.*;
import org.junit.jupiter.api.Test;

class ConvexVolumeTest {
    @Test void slopeUsesActualVolumeInsteadOfBoundingBox(){
        var slope=ConvexVolume.box(0,0,0,16,16,16).clipped(new Plane(new Vec(0,1,1),16));
        assertTrue(slope.hasVolume());assertEquals(Contact.DISJOINT,slope.contact(ConvexVolume.box(0,12,12,16,16,16)));
        assertEquals(Contact.OVERLAP,slope.contact(ConvexVolume.box(0,0,0,4,4,4)));
        var hit=slope.ray(new Vec(8,20,8),new Vec(0,-1,0),40).orElseThrow();assertEquals(8,hit.point().y(),1e-8);
    }
}
