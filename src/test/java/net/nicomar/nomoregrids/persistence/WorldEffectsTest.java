package net.nicomar.nomoregrids.persistence;

import java.nio.file.*;
import java.util.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.operation.OperationPlan;
import static org.junit.jupiter.api.Assertions.*;

class WorldEffectsTest {
    @TempDir Path root;
    @Test void journalReplaysEffectOnlyWorldOperations()throws Exception{
        var owner=new CellPos(-1,15,16);var value=new BinaryData(new byte[]{1,2,3});
        var effect=new OperationPlan.Effect("loot:"+UUID.randomUUID(),owner,Optional.empty(),Optional.of(value));
        var plan=new OperationPlan(UUID.randomUUID(),OperationPlan.WORLD_ACTOR,OperationPlan.Kind.SYSTEM,List.of(),BinaryData.EMPTY,BinaryData.EMPTY,List.of(effect));
        assertEquals(plan,OperationCodec.decode(OperationCodec.encode(plan)));assertEquals(Set.of(owner),plan.affectedCells());
        var file=root.resolve("operations.log");try(var journal=new DurableJournal(file)){journal.commit(plan);}
        try(var journal=new DurableJournal(file)){assertEquals(List.of(plan),journal.committed());}
    }
    @Test void corruptedCheckpointIsRetainedRatherThanReplaced()throws Exception{
        var store=new WorldEffectStore(root);var owner=new CellPos(-1,0,0);var values=Map.of("loot:sample",new WorldEffectStore.Value(owner,new BinaryData(new byte[]{4})));
        store.write(owner.section(),1,values);assertEquals(1,store.checkpoint(owner.section()));
        var file=root.resolve("-1_0_0.effects");byte[] damaged=Files.readAllBytes(file);damaged[damaged.length-1]^=1;Files.write(file,damaged);
        assertThrows(IOException.class,()->store.write(owner.section(),2,values));assertArrayEquals(damaged,Files.readAllBytes(file));
    }
}
