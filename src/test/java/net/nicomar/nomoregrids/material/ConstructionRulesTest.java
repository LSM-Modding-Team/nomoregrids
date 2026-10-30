package net.nicomar.nomoregrids.material;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.simulation.support.SupportResolver;
import net.nicomar.nomoregrids.simulation.support.SupportResolver.Patch;
import net.nicomar.nomoregrids.client.render.BoxMesh;
import net.nicomar.nomoregrids.geometry.*;
import org.junit.jupiter.api.Test;

class ConstructionRulesTest {
    @Test void cuttingConservesLengthAndRejectsMissingRemainderSpace(){
        var key=new FragmentEconomy.Key(new ResourceId("minecraft:glass"),new ResourceId("nomoregrids:panel"),0,BinaryData.EMPTY);
        var inventory=List.of(Optional.of(new FragmentEconomy.Stack(key,16,1)));
        var cut=FragmentEconomy.take(inventory,key,6,false);
        assertEquals(6,cut.placed().length());assertEquals(10,cut.inventory().getFirst().orElseThrow().length());
        assertEquals(16,inventory.getFirst().orElseThrow().length());
        assertThrows(IllegalArgumentException.class,()->FragmentEconomy.take(List.of(Optional.of(new FragmentEconomy.Stack(key,16,2))),key,6,false));
        assertEquals(16,FragmentEconomy.join(cut.placed(),cut.inventory().getFirst().orElseThrow()).length());
    }
    @Test void supportNeedsCompleteCoverageAtActualPlane(){
        var required=new Patch(Direction6.DOWN,8,0,0,16,16);
        var left=new Patch(Direction6.UP,8,0,0,8,16);var right=new Patch(Direction6.UP,8,8,0,16,16);
        assertTrue(SupportResolver.covered(required,List.of(left,right)));assertFalse(SupportResolver.covered(required,List.of(left)));
        assertFalse(SupportResolver.covered(required,List.of(new Patch(Direction6.UP,7,0,0,16,16))));
        UUID a=UUID.randomUUID(),b=UUID.randomUUID();assertEquals(Set.of(a,b),SupportResolver.unsupported(Map.of(a,Set.of(b),b,Set.of(a)),Set.of()));
    }
    @Test void internalBoxFacesAreRemovedWithoutChangingGeometry(){
        var shape=new BoxUnion(List.of(Box16.of(0,0,0,8,16,16),Box16.of(8,0,0,16,16,16)));
        var faces=BoxMesh.extract(shape,100);
        assertTrue(faces.stream().noneMatch(f->f.normal().axis()==0&&f.plane()==8));
        assertEquals(2,shape.boxes().size());
    }
}
