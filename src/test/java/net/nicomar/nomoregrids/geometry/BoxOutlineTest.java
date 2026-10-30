package net.nicomar.nomoregrids.geometry;

import java.util.*;
import org.junit.jupiter.api.Test;
import net.nicomar.nomoregrids.core.GridPoint;
import static org.junit.jupiter.api.Assertions.*;

class BoxOutlineTest {
    @Test void removesSeamsAcrossUnequalCoplanarFacesAndAllRotations(){
        var tiled=new BoxUnion(List.of(Box16.of(0,0,0,8,16,16),Box16.of(8,0,0,16,8,16),Box16.of(8,8,0,16,16,16)));
        for(var rotation:Rotation.all()){
            var shape=tiled.transform(rotation,GridPoint.BLOCK_CENTER,new GridPoint(-17,15,255));
            assertEquals(12,BoxOutline.extract(shape,1024).size());
            assertEquals(12,BoxOutline.extract(ConstructionShapes.LOWER_SLAB.transform(rotation,GridPoint.BLOCK_CENTER,GridPoint.ZERO),1024).size());
        }
    }
    @Test void retainsConcaveStairCreasesAndHonorsBudget(){
        assertTrue(BoxOutline.extract(ConstructionShapes.STRAIGHT_STAIR,1024).size()>12);
        assertThrows(IllegalArgumentException.class,()->BoxOutline.extract(ConstructionShapes.STRAIGHT_STAIR,1));
    }
}
