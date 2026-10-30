package net.nicomar.nomoregrids.geometry;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.nicomar.nomoregrids.core.CellPos;
import net.nicomar.nomoregrids.core.GridPoint;
import net.nicomar.nomoregrids.core.SectionPos;
import org.junit.jupiter.api.Test;

class GeometryTest {
    @Test
    void negativeCoordinatesAndSectionBoundariesUseFloor() {
        assertEquals(new CellPos(-1,-2,0), new GridPoint(-1,-17,15).cell());
        assertEquals(new GridPoint(15,15,15), new GridPoint(-1,-17,15).offsetInCell());
        assertEquals(new SectionPos(-1,0,1), new CellPos(-1,15,16).section());
        for (long q = -513; q <= 513; q++) {
            GridPoint p = new GridPoint(q,q,q);
            assertEquals(q, p.cell().x()*16 + p.offsetInCell().x());
            assertTrue(p.offsetInCell().x() >= 0 && p.offsetInCell().x() < 16);
        }
    }

    @Test
    void cubeOnSlabTouchesTwoCellsAndDoesNotIntersectSupport() {
        BoxUnion cube = ConstructionShapes.CUBE.transform(Rotation.of(0), GridPoint.BLOCK_CENTER, new GridPoint(0,8,0));
        assertEquals(Box16.of(0,8,0,16,24,16), cube.bounds());
        assertEquals(Set.of(new CellPos(0,0,0), new CellPos(0,1,0)), cube.cells(8));
        assertFalse(cube.overlaps(ConstructionShapes.LOWER_SLAB));
        assertEquals(Set.of(new CellPos(0,0,0)), ConstructionShapes.CUBE.cells(8));
    }

    @Test
    void offsetAtNegativeSectionCornerTouchesEightSections() {
        BoxUnion cube = ConstructionShapes.CUBE.transform(Rotation.of(0), GridPoint.BLOCK_CENTER, new GridPoint(-1,-1,-1));
        assertEquals(8, cube.cells(8).size());
        assertEquals(8, cube.cells(8).stream().map(CellPos::section).distinct().count());
        assertThrows(IllegalArgumentException.class, () -> cube.cells(7));
    }

    @Test
    void touchingIsAllowedButOneUnitOfPenetrationIsNot() {
        BoxUnion left = new BoxUnion(List.of(Box16.of(0,0,0,8,16,16)));
        assertFalse(left.overlaps(new BoxUnion(List.of(Box16.of(8,0,0,16,16,16)))));
        assertTrue(left.overlaps(new BoxUnion(List.of(Box16.of(7,0,0,16,16,16)))));
        assertFalse(ConstructionShapes.STRAIGHT_STAIR.overlaps(
            new BoxUnion(List.of(Box16.of(0,8,8,16,16,16)))));
    }

    @Test
    void allRotationsAreUniqueProperAndClosedUnderComposition() {
        Set<List<GridPoint>> frames = new HashSet<>();
        GridPoint x = new GridPoint(1,0,0), y = new GridPoint(0,1,0), z = new GridPoint(0,0,1);
        GridPoint sample = new GridPoint(3,-5,11);
        for (Rotation r : Rotation.all()) {
            GridPoint a = r.apply(x), b = r.apply(y), c = r.apply(z);
            frames.add(List.of(a,b,c));
            long determinant = a.x()*(b.y()*c.z()-b.z()*c.y())
                - b.x()*(a.y()*c.z()-a.z()*c.y()) + c.x()*(a.y()*b.z()-a.z()*b.y());
            assertEquals(1, determinant);
            assertEquals(sample, r.inverse().apply(r.apply(sample)));
            for (Rotation s : Rotation.all()) assertEquals(r.apply(s.apply(sample)), r.compose(s).apply(sample));
        }
        assertEquals(24, frames.size());
        assertEquals(sample, Rotation.of(0).apply(sample));
        assertEquals(new GridPoint(0,1,0), Rotation.of(8).apply(new GridPoint(0,0,-1)));
        assertThrows(IllegalArgumentException.class, () -> Rotation.of(24));
    }

    @Test
    void slabHasSixDistinctHalvesAndPivotSurvivesRoundTrip() {
        Set<Box16> halves = new HashSet<>();
        for (int id : new int[] {0,4,8,12,16,20}) halves.add(ConstructionShapes.slab(id).bounds());
        assertEquals(Set.of(Box16.of(0,0,0,16,8,16), Box16.of(0,8,0,16,16,16),
            Box16.of(0,0,0,16,16,8), Box16.of(0,0,8,16,16,16),
            Box16.of(0,0,0,8,16,16), Box16.of(8,0,0,16,16,16)), halves);
        BoxUnion cropped = new BoxUnion(List.of(Box16.of(6,0,7,10,6,9)));
        for (Rotation r : Rotation.all()) {
            assertEquals(cropped, cropped.transform(r, GridPoint.BLOCK_CENTER, GridPoint.ZERO)
                .transform(r.inverse(), GridPoint.BLOCK_CENTER, GridPoint.ZERO));
        }
    }

    @Test
    void outwardQuantizationLeavesRealGapAndTiesGoPositive() {
        assertEquals(2, PlacementQuantizer.outward(1.5/16, true));
        assertEquals(1, PlacementQuantizer.outward(1.5/16, false));
        assertEquals(-1, PlacementQuantizer.outward(-1.5/16, true));
        assertEquals(-2, PlacementQuantizer.outward(-1.5/16, false));
        assertEquals(-1, PlacementQuantizer.tangential(-1.5/16));
        assertEquals(2, PlacementQuantizer.tangential(1.5/16));
        assertThrows(IllegalArgumentException.class, () -> PlacementQuantizer.tangential(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> PlacementQuantizer.outward(0x1.0p63/16,true));
    }

    @Test
    void malformedGeometryAndOverflowAreRejectedBeforeEnumeration() {
        assertThrows(IllegalArgumentException.class, () -> Box16.of(0,0,0,0,16,16));
        assertThrows(IllegalArgumentException.class, () -> Box16.of(Long.MIN_VALUE,0,0,Long.MAX_VALUE,1,1).cells(8));
        assertThrows(ArithmeticException.class, () -> new GridPoint(Long.MAX_VALUE,0,0).add(new GridPoint(1,0,0)));
        assertThrows(UnsupportedOperationException.class, () -> ConstructionShapes.CUBE.boxes().clear());
    }
}
