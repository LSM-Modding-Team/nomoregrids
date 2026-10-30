package net.nicomar.nomoregrids.network;

import java.io.IOException;
import java.util.*;
import org.junit.jupiter.api.Test;
import net.nicomar.nomoregrids.world.CellView;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.ConstructionShapes;
import static org.junit.jupiter.api.Assertions.*;

class CellDeltaTest {
    @Test void replacementRemovalAndNewIdentityRequireTheExactBase()throws Exception{
        var first=TestPieces.of(UUID.randomUUID(),ConstructionShapes.LOWER_SLAB,GridPoint.ZERO);
        var removed=TestPieces.of(UUID.randomUUID(),ConstructionShapes.CUBE,new GridPoint(16,0,0));
        var added=TestPieces.of(UUID.randomUUID(),ConstructionShapes.CUBE,new GridPoint(0,16,0));
        var before=new CellView(List.of(removed,first));var after=new CellView(List.of(added,first.moved(new GridPoint(8,0,0),0)));
        var bytes=CellDelta.encode(before,after);assertEquals(after,CellDelta.apply(before,bytes));
        assertThrows(IOException.class,()->CellDelta.apply(new CellView(List.of(first)),bytes));
        assertThrows(IOException.class,()->CellDelta.apply(after,bytes));
    }
    @Test void emptyDeltaRejectsWrongBaseAndTampering()throws Exception{
        var empty=new CellView(List.of());byte[] delta=CellDelta.encode(empty,empty);
        assertEquals(empty,CellDelta.apply(empty,delta));
        delta[4]^=1;assertThrows(IOException.class,()->CellDelta.apply(empty,delta));
    }
}
