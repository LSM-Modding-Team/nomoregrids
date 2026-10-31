package net.nicomar.nomoregrids.client.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class PlayerModesTest {
    @Test void candidateStaysInTheWireRangeInBothDirections() {
        var modes = new PlayerModes();
        modes.scroll(-1, false, true, false, 64);
        assertEquals(63, modes.candidate());
        modes.scroll(1, false, true, false, 64);
        assertEquals(0, modes.candidate());
        for (int i = 0; i < 65; i++) modes.scroll(1, false, true, false, 64);
        assertEquals(1, modes.candidate());
        modes.changedDimension();
        assertEquals(0, modes.candidate());
        modes.scroll(-1, false, true, false, 3);
        assertEquals(2, modes.candidate());
        modes.scroll(1, false, true, false, 3);
        assertEquals(0, modes.candidate());
    }
}
