package net.nicomar.nomoregrids.persistence;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.simulation.functional.FunctionalStore;
import net.nicomar.nomoregrids.simulation.redstone.SignalNetwork;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SimulationCodecTest {
    @TempDir Path directory;
    private static SimulationSnapshot state(){
        UUID source=UUID.randomUUID(),repeater=UUID.randomUUID();var network=new SignalNetwork(List.of(new SignalNetwork.Node(source,SignalNetwork.Kind.SOURCE,0,false),new SignalNetwork.Node(repeater,SignalNetwork.Kind.REPEATER,2,false)),List.of(new SignalNetwork.Edge(source,repeater,SignalNetwork.Input.REAR,0)));network.source(source,15);network.advance(0,100);
        var machine=new FunctionalStore.State(source,4,List.of(new BinaryData(new byte[]{1,2,3}),BinaryData.EMPTY),100,new BinaryData(new byte[]{9}));
        return new SimulationSnapshot(new SectionPos(-1,0,0),7,Map.of(source,machine),Map.of(UUID.randomUUID(),Set.of(source,repeater)),Map.of(UUID.randomUUID(),network.snapshot()));
    }
    @Test void roundTripPreservesOneInventorySourcesAndLogicalPendingTick()throws Exception{
        var before=state();var after=SimulationCodec.decode(SimulationCodec.encode(before));assertEquals(before,after);var circuit=SignalNetwork.restored(after.circuits().values().iterator().next());
        UUID repeater=after.circuits().values().iterator().next().nodes().stream().filter(n->n.kind()==SignalNetwork.Kind.REPEATER).findFirst().orElseThrow().id();
        circuit.advance(1,100);assertEquals(0,circuit.value(repeater));circuit.advance(2,100);assertEquals(15,circuit.value(repeater));
    }
    @Test void emptySectionAndCorruptionAreHandledWithoutSubstitution()throws Exception{
        var empty=new SimulationSnapshot(new SectionPos(0,0,0),0,Map.of(),Map.of(),Map.of());assertEquals(empty,SimulationCodec.decode(SimulationCodec.encode(empty)));
        byte[] bytes=SimulationCodec.encode(state());bytes[50]^=1;assertThrows(IOException.class,()->SimulationCodec.decode(bytes));
    }
    @Test void checkpointCannotOvertakeJournalOrOverwriteCorruption()throws Exception{
        var store=new SimulationStore(directory);var state=state();assertThrows(IOException.class,()->store.writeCommitted(state,6));store.writeCommitted(state,7);assertEquals(state,store.read(state.section()).orElseThrow());
        Path file=directory.resolve("-1_0_0.simulation");byte[] bytes=Files.readAllBytes(file);bytes[10]^=1;Files.write(file,bytes);
        assertThrows(IOException.class,()->store.writeCommitted(state,7));assertArrayEquals(bytes,Files.readAllBytes(file));
    }
}
