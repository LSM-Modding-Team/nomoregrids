package net.nicomar.nomoregrids.persistence;

import java.io.IOException;
import java.util.*;
import org.junit.jupiter.api.Test;
import net.nicomar.nomoregrids.core.BinaryData;
import net.nicomar.nomoregrids.simulation.fluid.CavityCell;
import static org.junit.jupiter.api.Assertions.*;

class CavityCodecTest {
    @Test void preservesPartialMasksMixedKindsAndRejectsFutureData()throws Exception{
        var mask=new BitSet(4096);mask.set(0,16);byte[] fluids=new byte[4096];Arrays.fill(fluids,16,64,(byte)1);Arrays.fill(fluids,80,96,(byte)2);
        var cell=new CavityCell(mask,fluids,CavityCell.Kind.EMPTY);var bytes=CavityCodec.encode(cell);assertEquals(CavityCodec.MAX_BYTES,bytes.size());assertEquals(cell,CavityCodec.decode(bytes));
        byte[] future=bytes.copy();future[0]=3;assertThrows(IOException.class,()->CavityCodec.decode(new BinaryData(future)));
    }
    @Test void sharedDecodedImagesRemainImmutableAndFutureImagesStillFail()throws Exception{
        var source=new CavityCell(new BitSet(),new byte[4096],CavityCell.Kind.EMPTY).bucket(CavityCell.Kind.WATER,false);
        var image=CavityCodec.encode(source);var first=CavityCodec.decode(image);first.fluids()[0]=0;first.distances()[0]=127;first.blocked().set(0);
        assertEquals(source,CavityCodec.decode(image));assertTrue(CavityCodec.cacheStats().bytes()<=8L*1024*1024);
        var invalid=image.copy();invalid[0]=99;assertThrows(IOException.class,()->CavityCodec.decode(new BinaryData(invalid)));
    }
    @Test void neverTurnsPartialFlowIntoABucketSource(){
        byte[] fluids=new byte[4096];Arrays.fill(fluids,0,2048,(byte)1);var partial=new CavityCell(new BitSet(),fluids,CavityCell.Kind.EMPTY);
        assertThrows(IllegalArgumentException.class,()->partial.bucket(CavityCell.Kind.WATER,true));
    }
}
