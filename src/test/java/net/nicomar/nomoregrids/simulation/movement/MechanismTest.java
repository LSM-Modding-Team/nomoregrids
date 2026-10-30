package net.nicomar.nomoregrids.simulation.movement;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.*;
import net.nicomar.nomoregrids.world.*;
import net.nicomar.nomoregrids.simulation.redstone.SignalNetwork;
import org.junit.jupiter.api.Test;

class MechanismTest {
    @Test void pistonMovesWholeOffsetPieceAndRejectsSweepObstruction(){
        var p=TestPieces.of(UUID.randomUUID(),ConstructionShapes.CUBE,new GridPoint(0,8,0));var index=PieceIndex.restored(List.of(p));
        var changes=PistonPlanner.plan(index,Set.of(p.pieceId()),Direction6.EAST,false,new GroupRegistry(),id->true,cell->true,box->false,(a,b)->false);
        assertEquals(new GridPoint(16,8,0),changes.getFirst().replacement().orElseThrow().origin16());assertEquals(p.pieceId(),changes.getFirst().pieceId());
        assertThrows(IllegalArgumentException.class,()->PistonPlanner.plan(index,Set.of(p.pieceId()),Direction6.EAST,false,new GroupRegistry(),id->true,cell->true,
            box->box.overlaps(Box16.of(17,8,0,18,24,16)),(a,b)->false));
    }
    @Test void thirteenComponentsDoNotBecomeTwelveCells(){
        var pieces=new ArrayList<PieceRecord>();for(int i=0;i<13;i++)pieces.add(TestPieces.of(UUID.randomUUID(),ConstructionShapes.CUBE,new GridPoint(i*16,0,0)));
        var index=PieceIndex.restored(pieces);
        assertThrows(IllegalArgumentException.class,()->PistonPlanner.plan(index,Set.of(pieces.getFirst().pieceId()),Direction6.EAST,false,new GroupRegistry(),id->true,cell->true,b->false,(a,b)->false));
    }
    @Test void repeaterUsesLogicalDelayAndUnlinkedNodeReceivesNothing(){
        UUID source=UUID.randomUUID(),repeater=UUID.randomUUID(),unlinked=UUID.randomUUID();
        var network=new SignalNetwork(List.of(new SignalNetwork.Node(source,SignalNetwork.Kind.SOURCE,0,false),new SignalNetwork.Node(repeater,SignalNetwork.Kind.REPEATER,2,false),
            new SignalNetwork.Node(unlinked,SignalNetwork.Kind.DUST,0,false)),List.of(new SignalNetwork.Edge(source,repeater,SignalNetwork.Input.REAR,0)));
        network.source(source,15);network.advance(0,100);assertEquals(0,network.value(repeater));network.advance(1,100);assertEquals(0,network.value(repeater));
        network.advance(2,100);assertEquals(15,network.value(repeater));assertEquals(0,network.value(unlinked));
    }
    @Test void sideAdhesionFindsPieceOutsideSweptCells(){
        var a=TestPieces.of(UUID.randomUUID(),ConstructionShapes.CUBE,GridPoint.ZERO);
        var b=TestPieces.of(UUID.randomUUID(),ConstructionShapes.CUBE,new GridPoint(-16,0,0));
        var changes=PistonPlanner.plan(PieceIndex.restored(List.of(a,b)),Set.of(a.pieceId()),Direction6.EAST,false,new GroupRegistry(),id->true,c->true,s->false,(x,y)->true);
        assertEquals(Set.of(a.pieceId(),b.pieceId()),changes.stream().map(PieceChange::pieceId).collect(java.util.stream.Collectors.toSet()));
    }
    @Test void deferredCircuitPreservesLogicalDelayAndCoalescesRequests(){
        UUID source=UUID.randomUUID(),repeater=UUID.randomUUID();
        var circuit=new SignalNetwork(List.of(new SignalNetwork.Node(source,SignalNetwork.Kind.SOURCE,0,false),new SignalNetwork.Node(repeater,SignalNetwork.Kind.REPEATER,2,false)),List.of(new SignalNetwork.Edge(source,repeater,SignalNetwork.Input.REAR,0)));
        for(int i=0;i<10000;i++)circuit.source(source,i%16);
        assertEquals(1,circuit.pending());circuit.source(source,15);
        circuit.advance(10,1);assertEquals(0,circuit.value(repeater));
        circuit.advance(10,1);assertEquals(15,circuit.value(repeater));assertEquals(0,circuit.pending());
    }
    @Test void aNewInputCannotEraseAnEarlierUnprocessedPulse(){
        UUID background=new UUID(0,1),source=new UUID(0,2),repeater=new UUID(0,3);
        var network=new SignalNetwork(List.of(new SignalNetwork.Node(background,SignalNetwork.Kind.SOURCE,0,false),new SignalNetwork.Node(source,SignalNetwork.Kind.SOURCE,0,false),new SignalNetwork.Node(repeater,SignalNetwork.Kind.REPEATER,2,false)),List.of(new SignalNetwork.Edge(source,repeater,SignalNetwork.Input.REAR,0)));
        network.source(background,15);network.source(source,15);network.advance(10,1);network.source(source,0);
        var restored=SignalNetwork.restored(network.snapshot());restored.advance(10,100);assertEquals(15,restored.value(repeater));restored.advance(12,100);assertEquals(0,restored.value(repeater));
    }
}
