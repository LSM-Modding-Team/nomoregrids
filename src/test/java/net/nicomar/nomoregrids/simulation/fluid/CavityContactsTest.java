package net.nicomar.nomoregrids.simulation.fluid;

import java.util.*;
import org.junit.jupiter.api.Test;
import net.nicomar.nomoregrids.core.CellPos;
import static org.junit.jupiter.api.Assertions.*;

class CavityContactsTest {
    @Test void dryBodyBesideAPocketDoesNotBecomeSubmerged(){
        byte[] fluid=new byte[4096];fluid[0]=1;var cavity=new CavityCell(new BitSet(),fluid,CavityCell.Kind.EMPTY);
        assertTrue(CavityContacts.query(new CellPos(0,0,0),cavity,new CavityContacts.Body(1,0,0,2,1,1),1.5,.5,.5).isEmpty());
        var wet=CavityContacts.query(new CellPos(0,0,0),cavity,new CavityContacts.Body(.5,0,0,1.5,1,1),.5,.5,.5).get(CavityCell.Kind.WATER);assertEquals(.5,wet.volume16());assertTrue(wet.eyesInside());
    }
    @Test void negativeCoordinatesAndMixedFluidsKeepEyeIdentity(){
        byte[] fluid=new byte[4096];fluid[0]=1;fluid[1]=2;var cavity=new CavityCell(new BitSet(),fluid,CavityCell.Kind.EMPTY);
        var contacts=CavityContacts.query(new CellPos(-1,0,0),cavity,new CavityContacts.Body(-16,0,0,-14,1,1),-15.5,.5,.5);
        assertTrue(contacts.get(CavityCell.Kind.WATER).eyesInside());assertFalse(contacts.get(CavityCell.Kind.LAVA).eyesInside());assertEquals(1,contacts.get(CavityCell.Kind.LAVA).volume16());
    }
}
