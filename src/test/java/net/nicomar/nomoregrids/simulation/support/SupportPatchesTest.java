package net.nicomar.nomoregrids.simulation.support;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.*;
import org.junit.jupiter.api.Test;

class SupportPatchesTest {
    private static PieceRecord carpet(int rotation){return new PieceRecord(UUID.randomUUID(),0,new ResourceId("nomoregrids:box"),1,new ResourceId("minecraft:red_carpet"),Map.of(),new GridPoint(-8,16,0),rotation,GridPoint.BLOCK_CENTER,
        new BoxGeometrySnapshot(new BoxUnion(List.of(Box16.of(0,0,0,16,1,16))),new BoxUnion(List.of(Box16.of(0,0,0,16,1,16))),new BoxUnion(List.of(Box16.of(0,0,0,16,1,16)))),BinaryData.EMPTY,BinaryData.EMPTY,BinaryData.EMPTY,Optional.empty(),Set.of());}
    @Test void carpetCanSpanTwoSupportsButNotOneQuantumGap(){
        var required=SupportPatches.required(carpet(0)).getFirst();assertEquals(16,required.plane());
        var a=new SupportResolver.Patch(Direction6.UP,16,-8,0,0,16);var b=new SupportResolver.Patch(Direction6.UP,16,0,0,8,16);
        assertTrue(SupportResolver.covered(required,List.of(a,b)));
        assertFalse(SupportResolver.covered(required,List.of(a,new SupportResolver.Patch(Direction6.UP,16,1,0,8,16))));
        assertEquals(2,SupportPatches.contactCells(required).cells(8).size());
    }
    @Test void everyRotationTransformsNormalAndAnchorTogether(){
        for(var rotation:Rotation.all()){
            var p=carpet(rotation.id());var required=SupportPatches.required(p).getFirst();
            assertEquals(Direction6.DOWN.rotated(rotation.id()),required.normal());
            var box=p.worldOccupation().bounds();long plane=required.normal().axis()==0?(required.normal().sign()>0?box.max().x():box.min().x()):required.normal().axis()==1?(required.normal().sign()>0?box.max().y():box.min().y()):(required.normal().sign()>0?box.max().z():box.min().z());
            assertEquals(plane,required.plane());
        }
    }
}
