package net.nicomar.nomoregrids.simulation.functional;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExperienceProgressTest {
    @Test void preservesFractionAcrossLevelBoundary(){var next=new ExperienceProgress(0,6,.9f).add(1);assertEquals(1,next.level());assertEquals(7,next.total());assertEquals(.3/9,next.progress(),1e-6);}
    @Test void followsNativeLevelCurve(){assertEquals(15,new ExperienceProgress(14,0,0).add(35).level());assertEquals(30,new ExperienceProgress(29,0,0).add(107).level());assertThrows(IllegalArgumentException.class,()->new ExperienceProgress(0,0,0).add(-1));}
}
