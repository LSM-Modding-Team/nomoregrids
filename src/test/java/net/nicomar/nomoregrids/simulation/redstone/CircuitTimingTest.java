package net.nicomar.nomoregrids.simulation.redstone;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CircuitTimingTest {
    @Test void torchInvertsAfterTwoTicksAndLockedRepeaterHoldsItsOutput(){
        var source=UUID.randomUUID();var side=UUID.randomUUID();var torch=UUID.randomUUID();var repeater=UUID.randomUUID();var circuit=new SignalNetwork(List.of(new SignalNetwork.Node(source,SignalNetwork.Kind.SOURCE,0,false),new SignalNetwork.Node(side,SignalNetwork.Kind.SOURCE,0,false),new SignalNetwork.Node(torch,SignalNetwork.Kind.TORCH,2,false),new SignalNetwork.Node(repeater,SignalNetwork.Kind.REPEATER,2,false)),List.of(new SignalNetwork.Edge(source,torch,SignalNetwork.Input.REAR,0),new SignalNetwork.Edge(source,repeater,SignalNetwork.Input.REAR,0),new SignalNetwork.Edge(side,repeater,SignalNetwork.Input.SIDE,0)));
        circuit.refresh();circuit.advance(2,100);assertEquals(15,circuit.value(torch));circuit.source(source,15);circuit.advance(2,100);circuit.advance(4,100);assertEquals(0,circuit.value(torch));assertEquals(15,circuit.value(repeater));circuit.source(side,15);circuit.advance(4,100);circuit.source(source,0);circuit.advance(6,100);assertEquals(15,circuit.value(repeater));var restored=SignalNetwork.restored(circuit.snapshot());restored.source(side,0);restored.advance(6,100);restored.advance(8,100);assertEquals(0,restored.value(repeater));
    }
}
