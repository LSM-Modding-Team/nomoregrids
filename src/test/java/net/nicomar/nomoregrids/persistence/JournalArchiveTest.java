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
    @Test void sealedCheckpointCanReceiveAnIncrementalTailBeforePublication()throws Exception{
        var prepared=JournalArchive.prepare(root,new JournalArchive.State(10,Map.of(),Map.of(),Map.of()),null,Map.of(),Set.of());prepared.seal();
        var cell=new net.nicomar.nomoregrids.core.CellPos(0,0,0);var data=new net.nicomar.nomoregrids.core.BinaryData(new byte[]{7});var plan=new OperationPlan(UUID.randomUUID(),OperationPlan.WORLD_ACTOR,OperationPlan.Kind.SYSTEM,List.of(),net.nicomar.nomoregrids.core.BinaryData.EMPTY,net.nicomar.nomoregrids.core.BinaryData.EMPTY,List.of(new OperationPlan.Effect("loot:"+UUID.randomUUID(),cell,Optional.empty(),Optional.of(data))));
        try(var tail=new DurableJournal(prepared.activeLog())){tail.commit(plan);prepared.publish();assertEquals(10,JournalArchive.open(root).orElseThrow().state().sequence());assertTrue(tail.contains(plan.id()));}
        try(var reopened=new DurableJournal(JournalArchive.open(root).orElseThrow().activeLog())){assertEquals(List.of(plan),reopened.committed());}
    }
    @Test void aDamagedPreparedCheckpointCannotReplaceTheManifest()throws Exception{
        var first=JournalArchive.prepare(root,new JournalArchive.State(0,Map.of(),Map.of(),Map.of()),null,Map.of(),Set.of());first.publish();byte[] pointer=Files.readAllBytes(root.resolve("active.json"));
        var next=JournalArchive.prepare(root,new JournalArchive.State(0,Map.of(),Map.of(),Map.of()),first,Map.of(),Set.of());var file=next.activeLog().getParent().resolve("state.bin");byte[] damaged=Files.readAllBytes(file);damaged[damaged.length-1]^=1;Files.write(file,damaged);
        assertThrows(IOException.class,next::publish);assertArrayEquals(pointer,Files.readAllBytes(root.resolve("active.json")));assertTrue(JournalArchive.open(root).isPresent());
    }
}
