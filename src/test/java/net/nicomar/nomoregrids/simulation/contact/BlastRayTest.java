package net.nicomar.nomoregrids.simulation.contact;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BlastRayTest {
    @Test void shieldingUsesThicknessAndUnionIntervalsAreNotChargedTwice(){
        var target=UUID.randomUUID();var shield=UUID.randomUUID();var material=new BlastRay.Span(target,1,1.1,1);assertTrue(BlastRay.reaches(target,List.of(material,material),2,2));assertFalse(BlastRay.reaches(target,List.of(new BlastRay.Span(shield,0.1,0.9,100),material),10,2));assertTrue(BlastRay.reaches(target,List.of(new BlastRay.Span(shield,0.1,0.11,100),material),10,2));
    }
}
