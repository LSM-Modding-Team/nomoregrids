package net.nicomar.nomoregrids.network;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class SnapshotAssemblerTest {
    @Test void reorderedAndDuplicatedChunksAssembleOnce() throws Exception {
        byte[] bytes=new byte[150000];new Random(42).nextBytes(bytes);
        var chunks=SnapshotAssembler.split(UUID.randomUUID(),bytes);var a=new SnapshotAssembler();
        assertTrue(a.accept(chunks.get(2),0).isEmpty());assertTrue(a.accept(chunks.get(2),1).isEmpty());
        assertTrue(a.accept(chunks.get(0),2).isEmpty());assertArrayEquals(bytes,a.accept(chunks.get(1),3).orElseThrow());
    }
    @Test void budgetAndHashAreEnforced() throws Exception {
        var a=new SnapshotAssembler();byte[] data=new byte[70000];
        for(int i=0;i<4;i++)a.accept(SnapshotAssembler.split(UUID.randomUUID(),data).getFirst(),0);
        assertThrows(java.io.IOException.class,()->a.accept(SnapshotAssembler.split(UUID.randomUUID(),data).getFirst(),1));
        assertTrue(a.accept(SnapshotAssembler.split(UUID.randomUUID(),data).getFirst(),10_000_000_001L).isEmpty());
        assertThrows(IllegalArgumentException.class,()->new SnapshotAssembler.Fragment(UUID.randomUUID(),Integer.MAX_VALUE,0,new byte[32],new byte[1]));
    }
}
