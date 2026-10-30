package net.nicomar.nomoregrids.operation;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.ConstructionShapes;
import net.nicomar.nomoregrids.persistence.DurableJournal;
import net.nicomar.nomoregrids.world.PieceIndex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests the real coordinator/journal contract, not a simulated Minecraft implementation. */
class OperationEngineTest {
    @TempDir Path dir;
    private static OperationPlan plan(){var p=TestPieces.of(UUID.randomUUID(),ConstructionShapes.CUBE,GridPoint.ZERO);
        return new OperationPlan(UUID.randomUUID(),UUID.randomUUID(),OperationPlan.Kind.PLACE,List.of(new OperationPlan.Change(p.pieceId(),Optional.empty(),Optional.of(p))),new BinaryData(new byte[]{5}),new BinaryData(new byte[]{4}));}
    private static final class Host implements OperationEngine.Host {
        BinaryData resources=new BinaryData(new byte[]{5});int installs;boolean refuse,failAfterInstall;
        Runnable duringValidation;
        Set<UUID> receipts=new HashSet<>();
        @Override public void assertServerThread(){}
        @Override public void validate(OperationPlan plan,PieceIndex before,PieceIndex after){if(refuse)throw new IllegalArgumentException("PROTECTED");assertEquals(plan.resourcesBefore(),resources);if(duringValidation!=null)duringValidation.run();}
        @Override public void install(OperationPlan plan,PieceIndex after)throws IOException{
            if(!resources.equals(plan.resourcesAfter())){if(!resources.equals(plan.resourcesBefore()))throw new IOException("Receipt conflict");resources=plan.resourcesAfter();installs++;}
            if(failAfterInstall)throw new IOException("Injected failure after resources");receipts.add(plan.id());
        }
        @Override public boolean alreadyApplied(UUID id){return receipts.contains(id);}
        @Override public void afterCommit(OperationPlan plan){}
    }
    @Test void validationCannotCommitAReentrantOperation()throws Exception{
        try(var journal=new DurableJournal(dir.resolve("ops"))){var host=new Host();var engine=new OperationEngine(PieceIndex.empty(),journal,host,PieceIndex.Limits.DEFAULT);var outer=plan();var nested=plan();
            host.duringValidation=()->assertThrows(IllegalStateException.class,()->engine.execute(nested));engine.execute(outer);
            assertTrue(journal.contains(outer.id()));assertFalse(journal.contains(nested.id()));assertEquals(1,host.installs);assertEquals(1,engine.snapshot().records().size());assertFalse(engine.busy());
        }
    }
    @Test void rejectionHasNoDurableCommitOrResourceChange()throws Exception{
        try(var journal=new DurableJournal(dir.resolve("ops"))){var host=new Host();host.refuse=true;var engine=new OperationEngine(PieceIndex.empty(),journal,host,PieceIndex.Limits.DEFAULT);var plan=plan();
            assertThrows(IllegalArgumentException.class,()->engine.execute(plan));assertFalse(journal.contains(plan.id()));assertTrue(engine.snapshot().records().isEmpty());assertEquals(plan.resourcesBefore(),host.resources);}
    }
    @Test void duplicateIntentDoesNotConsumeTwice()throws Exception{
        try(var journal=new DurableJournal(dir.resolve("ops"))){var host=new Host();var engine=new OperationEngine(PieceIndex.empty(),journal,host,PieceIndex.Limits.DEFAULT);var plan=plan();engine.execute(plan);engine.execute(plan);
            assertEquals(1,host.installs);assertEquals(1,engine.snapshot().records().size());assertEquals(plan.resourcesAfter(),host.resources);}
    }
    @Test void postCommitFailureQuarantinesAndRecoveryUsesReceiptImage()throws Exception{
        try(var journal=new DurableJournal(dir.resolve("ops"))){var host=new Host();host.failAfterInstall=true;var engine=new OperationEngine(PieceIndex.empty(),journal,host,PieceIndex.Limits.DEFAULT);var plan=plan();
            assertThrows(IOException.class,()->engine.execute(plan));assertTrue(engine.quarantined());assertTrue(journal.contains(plan.id()));assertThrows(IOException.class,()->engine.execute(plan));
            host.failAfterInstall=false;engine.recover();assertFalse(engine.quarantined());assertEquals(1,host.installs);assertEquals(1,engine.snapshot().records().size());assertEquals(plan.resourcesAfter(),host.resources);}
    }
}
