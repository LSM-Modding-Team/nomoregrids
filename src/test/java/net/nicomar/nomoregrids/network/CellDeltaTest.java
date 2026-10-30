package net.nicomar.nomoregrids.network;

import java.io.IOException;
import java.util.*;
import org.junit.jupiter.api.Test;
import net.nicomar.nomoregrids.world.CellView;
import static org.junit.jupiter.api.Assertions.*;

class CellDeltaTest {
    @Test void emptyDeltaRejectsWrongBaseAndTampering()throws Exception{
        var empty=new CellView(List.of());byte[] delta=CellDelta.encode(empty,empty);
        assertEquals(empty,CellDelta.apply(empty,delta));
        delta[4]^=1;assertThrows(IOException.class,()->CellDelta.apply(empty,delta));
    }
}
