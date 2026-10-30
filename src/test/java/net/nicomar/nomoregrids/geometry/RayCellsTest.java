package net.nicomar.nomoregrids.geometry;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import net.nicomar.nomoregrids.core.CellPos;

class RayCellsTest {
    @Test void negativeBoundaryAndShortIntermediateCellsAreVisited(){assertEquals(List.of(new CellPos(0,0,0),new CellPos(-1,0,0)),RayCells.trace(new ConvexVolume.Vec(0,0.5,0.5),new ConvexVolume.Vec(-0.5,0.5,0.5),8));assertTrue(RayCells.trace(new ConvexVolume.Vec(0.9,0.900001,0.5),new ConvexVolume.Vec(1.9,1.900001,0.5),8).contains(new CellPos(0,1,0)));}
}
