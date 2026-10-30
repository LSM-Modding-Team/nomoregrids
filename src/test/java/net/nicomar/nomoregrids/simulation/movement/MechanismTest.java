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
}
