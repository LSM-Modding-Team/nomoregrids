package net.nicomar.nomoregrids.geometry;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import net.nicomar.nomoregrids.geometry.BoxRaycast.Face;
import net.nicomar.nomoregrids.geometry.BoxRaycast.Vector;
import org.junit.jupiter.api.Test;

class BoxRaycastTest {
    @Test
    void rayPassesThroughStairVoidAndStopsAtActualSurface() {
        assertTrue(BoxRaycast.cast(ConstructionShapes.STRAIGHT_STAIR,
            new Vector(-4,12,12), new Vector(1,0,0), 32).isEmpty());
        var hit = BoxRaycast.cast(ConstructionShapes.STRAIGHT_STAIR,
            new Vector(8,12,20), new Vector(0,0,-1), 32).orElseThrow();
        assertEquals(12, hit.distance16());
        assertEquals(Face.SOUTH, hit.face());
        assertEquals(new Vector(8,12,8), hit.point16());
    }

    @Test
    void internalSeamIsNotSelectedFromInsideUnion() {
        BoxUnion halves = new BoxUnion(List.of(Box16.of(0,0,0,8,16,16), Box16.of(8,0,0,16,16,16)));
        var hit = BoxRaycast.cast(halves, new Vector(4,8,8), new Vector(1,0,0), 32).orElseThrow();
        assertEquals(12, hit.distance16());
        assertEquals(Face.EAST, hit.face());
        assertTrue(BoxRaycast.cast(halves, new Vector(4,8,8), new Vector(1,0,0), 11).isEmpty());
    }

    @Test
    void reachAndRayInputsAreValidated() {
        assertTrue(BoxRaycast.cast(ConstructionShapes.CUBE, new Vector(-4,8,8), new Vector(1,0,0), 3).isEmpty());
        assertEquals(4, BoxRaycast.cast(ConstructionShapes.CUBE,
            new Vector(-4,8,8), new Vector(10,0,0), 4).orElseThrow().distance16());
        assertThrows(IllegalArgumentException.class, () -> BoxRaycast.cast(ConstructionShapes.CUBE,
            new Vector(0,0,0), new Vector(0,0,0), 32));
    }
}
