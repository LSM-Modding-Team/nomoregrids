package net.nicomar.nomoregrids.simulation.light;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import net.nicomar.nomoregrids.geometry.Box16;
import net.nicomar.nomoregrids.material.MaterialProfile;

class CellLightingTest {
    @Test void emittersUseMaximumAndTwoHalvesSealOnlyTheirUnion(){
        var bright=new MaterialProfile(1,1,.6f,0,15,15,true,true,false);
        var dim=new MaterialProfile(1,1,.6f,0,7,15,true,true,false);
        var lower=new CellLighting.Part(List.of(Box16.of(0,0,0,16,8,16)),bright);
        var upper=new CellLighting.Part(List.of(Box16.of(0,8,0,16,16,16)),dim);
        var full=CellLighting.calculate(List.of(lower,upper));assertEquals(15,full.emission());assertEquals(15,full.attenuation());assertTrue(full.sealed());
        assertFalse(CellLighting.calculate(List.of(lower)).sealed());
    }
    @Test void anEmptyEmitterCannotIlluminateTheCell(){
        var light=new MaterialProfile(1,1,.6f,0,15,0,false,false,false);
        assertEquals(new CellLighting.Light(0,0,false),CellLighting.calculate(List.of(new CellLighting.Part(List.of(),light))));
    }
}
