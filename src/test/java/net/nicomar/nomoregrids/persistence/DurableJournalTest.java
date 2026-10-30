package net.nicomar.nomoregrids.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.ConstructionShapes;
import net.nicomar.nomoregrids.operation.OperationPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DurableJournalTest {
    @TempDir Path dir;
    private OperationPlan plan(UUID id){
        var p=TestPieces.of(UUID.randomUUID(),ConstructionShapes.CUBE,GridPoint.ZERO);
        return new OperationPlan(id,UUID.randomUUID(),OperationPlan.Kind.PLACE,
            List.of(new OperationPlan.Change(p.pieceId(),Optional.empty(),Optional.of(p))),BinaryData.EMPTY,BinaryData.EMPTY);
    }
    @Test void durableReopenAndDuplicateCommitHaveOneEffect() throws Exception {
        var p=plan(UUID.randomUUID());var file=dir.resolve("dimension.log");
        try(var log=new DurableJournal(file)){log.commit(p);long size=Files.size(file);log.commit(p);assertEquals(size,Files.size(file));}
        try(var log=new DurableJournal(file)){assertEquals(List.of(p),log.committed());assertThrows(java.io.IOException.class,()->log.commit(plan(p.id())));}
    }
    @Test void tornTailIsRetainedRatherThanOverwritten() throws Exception {
        var file=dir.resolve("dimension.log");try(var log=new DurableJournal(file)){log.commit(plan(UUID.randomUUID()));}
        Files.write(file,new byte[]{1,2,3},StandardOpenOption.APPEND);byte[] original=Files.readAllBytes(file);
        assertThrows(java.io.IOException.class,()->new DurableJournal(file));assertArrayEquals(original,Files.readAllBytes(file));
    }
}
