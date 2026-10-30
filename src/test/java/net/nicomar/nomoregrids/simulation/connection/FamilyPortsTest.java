package net.nicomar.nomoregrids.simulation.connection;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.*;
import org.junit.jupiter.api.Test;

class FamilyPortsTest {
    private static PieceRecord pane(GridPoint origin,int rotation){return new PieceRecord(UUID.randomUUID(),0,new ResourceId("nomoregrids:box"),1,new ResourceId("minecraft:glass_pane"),Map.of(),origin,rotation,GridPoint.BLOCK_CENTER,
        new BoxGeometrySnapshot(ConstructionShapes.CUBE,ConstructionShapes.CUBE,ConstructionShapes.CUBE),BinaryData.EMPTY,BinaryData.EMPTY,BinaryData.EMPTY,Optional.empty(),Set.of());}
    @Test void cellSharingAndHeightOffsetsDoNotCreateConnections(){
        var a=pane(GridPoint.ZERO,0);var b=pane(new GridPoint(16,0,0),0);var east=FamilyPorts.ports(a).stream().filter(p->p.local()==Direction6.EAST).findFirst().orElseThrow();
        assertTrue(FamilyPorts.ports(b).stream().anyMatch(p->FamilyPorts.match(east,p)));
        assertFalse(FamilyPorts.ports(pane(new GridPoint(16,1,0),0)).stream().anyMatch(p->FamilyPorts.match(east,p)));
        assertFalse(FamilyPorts.ports(pane(new GridPoint(1,0,0),0)).stream().anyMatch(p->FamilyPorts.match(east,p)));
    }
    @Test void rotatedPortsMaintainContactInEveryFrame(){
        for(var rotation:Rotation.all()){
            var a=pane(GridPoint.ZERO,rotation.id());var b=pane(rotation.apply(new GridPoint(16,0,0)),rotation.id());
            assertTrue(FamilyPorts.ports(a).stream().anyMatch(x->FamilyPorts.ports(b).stream().anyMatch(y->FamilyPorts.match(x,y))));
        }
    }
}
