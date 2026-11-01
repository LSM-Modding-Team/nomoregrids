package net.nicomar.nomoregrids.geometry;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MicroBoxesTest {
    @Test void compactsFullDesignAndPreservesEveryConservativeVoxel(){var full=new BitSet(4096);full.set(0,4096);assertEquals(List.of(Box16.of(0,0,0,16,16,16)),MicroBoxes.pack(full).boxes());var random=new Random(7);var mask=new BitSet(4096);for(int i=0;i<4096;i++)if(random.nextBoolean())mask.set(i);assertEquals(mask,MicroBoxes.mask(MicroBoxes.pack(mask)));}
}
