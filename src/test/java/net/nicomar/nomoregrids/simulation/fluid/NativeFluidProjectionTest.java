package net.nicomar.nomoregrids.simulation.fluid;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeFluidProjectionTest {
    @Test void transfersOnlyExactNativeImages(){
        var dry=new CavityCell(new BitSet(),new byte[4096],CavityCell.Kind.EMPTY);
        var source=dry.bucket(CavityCell.Kind.WATER,false);assertTrue(NativeFluidProjection.exact(source).orElseThrow().source());
        assertTrue(NativeFluidProjection.exact(new CavityCell(new BitSet(),source.fluids(),CavityCell.Kind.EMPTY)).isEmpty());
        byte[] fluid=new byte[4096],distance=new byte[4096];Arrays.fill(distance,(byte)127);
        for(int i=0;i<4096;i++)if(i/16%16<8){fluid[i]=1;distance[i]=64;}
        var flow=new CavityCell(new BitSet(),fluid,CavityCell.Kind.EMPTY,distance);assertEquals(4,NativeFluidProjection.exact(flow).orElseThrow().amount());
        fluid[0]=0;distance[0]=127;assertTrue(NativeFluidProjection.exact(new CavityCell(new BitSet(),fluid,CavityCell.Kind.EMPTY,distance)).isEmpty());
        var blocked=new BitSet();blocked.set(0);assertTrue(NativeFluidProjection.exact(new CavityCell(blocked,fluid,CavityCell.Kind.EMPTY,distance)).isEmpty());
    }
}
