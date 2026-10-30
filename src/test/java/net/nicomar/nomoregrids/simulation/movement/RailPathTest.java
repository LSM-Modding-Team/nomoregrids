package net.nicomar.nomoregrids.simulation.movement;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import net.nicomar.nomoregrids.core.GridPoint;

class RailPathTest {
    @Test void matchingEndpointsAllowARealTurnAndAOneQuantumGapDoesNot(){
        var curve=new RailPath("north_east",0,GridPoint.BLOCK_CENTER,GridPoint.ZERO);var east=new RailPath("east_west",0,GridPoint.BLOCK_CENTER,new GridPoint(16,0,0));var gap=new RailPath("east_west",0,GridPoint.BLOCK_CENTER,new GridPoint(17,0,0));assertTrue(RailConnections.continuous(curve.endpoint(true),east.endpoint(false)));assertFalse(RailConnections.continuous(curve.endpoint(true),gap.endpoint(false)));assertEquals(16,curve.sample(1).position().x(),1e-8);assertEquals(1,curve.sample(1).tangent().x(),1e-8);
    }
    @Test void ascendingEndpointMatchesTheNextBlockAndDistanceRoundTrips(){
        var slope=new RailPath("ascending_east",0,GridPoint.BLOCK_CENTER,GridPoint.ZERO);var raised=new RailPath("east_west",0,GridPoint.BLOCK_CENTER,new GridPoint(16,16,0));assertTrue(RailConnections.continuous(slope.endpoint(true),raised.endpoint(false)));assertEquals(0.37,slope.parameter(slope.distance(0.37)),1e-8);assertEquals(17,slope.sample(1).position().y(),1e-8);
    }
}
