package net.nicomar.nomoregrids.persistence;

import java.nio.file.*;
import java.util.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import net.nicomar.nomoregrids.operation.OperationPlan;
import static org.junit.jupiter.api.Assertions.*;

class JournalArchiveTest {
    @TempDir Path root;
    private JournalArchive.Stamp stamp(UUID id,UUID actor,long sequence){return new JournalArchive.Stamp(id,actor,OperationPlan.Kind.PLACE,sequence,new byte[32]);}
    @Test void generationsPreserveDeduplicationAndKnownRemovedPieces()throws Exception{
        UUID actor=UUID.randomUUID(),operation=UUID.randomUUID(),piece=UUID.randomUUID();
        var first=JournalArchive.prepare(root,new JournalArchive.State(1,Map.of(),Map.of(),Map.of(actor,1L)),null,Map.of(operation,stamp(operation,actor,1)),Set.of(piece));first.publish();var opened=JournalArchive.open(root).orElseThrow();
        assertTrue(opened.knownPiece(piece));assertEquals(actor,opened.operation(operation).orElseThrow().actor());assertTrue(opened.operation(UUID.randomUUID()).isEmpty());
        UUID next=UUID.randomUUID();var second=JournalArchive.prepare(root,new JournalArchive.State(2,Map.of(),Map.of(),Map.of(actor,2L)),opened,Map.of(next,stamp(next,actor,2)),Set.of());second.publish();
        var current=JournalArchive.open(root).orElseThrow();assertEquals(2,current.state().sequence());assertTrue(current.operation(operation).isPresent());assertTrue(current.operation(next).isPresent());assertTrue(current.knownPiece(piece));assertTrue(Files.exists(root.resolve("active.previous.json")));
    }
    @Test void aDamagedPreparedCheckpointCannotReplaceTheManifest()throws Exception{
        var first=JournalArchive.prepare(root,new JournalArchive.State(0,Map.of(),Map.of(),Map.of()),null,Map.of(),Set.of());first.publish();byte[] pointer=Files.readAllBytes(root.resolve("active.json"));
        var next=JournalArchive.prepare(root,new JournalArchive.State(0,Map.of(),Map.of(),Map.of()),first,Map.of(),Set.of());var file=next.activeLog().getParent().resolve("state.bin");byte[] damaged=Files.readAllBytes(file);damaged[damaged.length-1]^=1;Files.write(file,damaged);
        assertThrows(IOException.class,next::publish);assertArrayEquals(pointer,Files.readAllBytes(root.resolve("active.json")));assertTrue(JournalArchive.open(root).isPresent());
    }
}
