package net.nicomar.nomoregrids.simulation.contact;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.nicomar.nomoregrids.core.Direction6;
import net.nicomar.nomoregrids.geometry.*;
import org.junit.jupiter.api.Test;

class ContactResolverTest {
    private static BoxMesh.Face patch(int x0,int x1){return new BoxMesh.Face(Direction6.UP,16,new BoxMesh.Rect(x0,0,x1,16));}
    @Test void contactAreaAndStableIdentityChooseMaterial(){
        UUID a=new UUID(0,1),b=new UUID(0,2);var foot=new ContactResolver.Foot(0,0,16,16,16);
        var picked=ContactResolver.ground(foot,Map.of(a,List.of(patch(0,4)),b,List.of(patch(4,16)))).orElseThrow();assertEquals(b,picked.piece());assertEquals(192,picked.area16());
        assertEquals(a,ContactResolver.ground(foot,Map.of(b,List.of(patch(0,8)),a,List.of(patch(8,16)))).orElseThrow().piece());
    }
    @Test void repeatedGeometryDoesNotDoubleItsContactWeight(){
        UUID a=new UUID(0,1),b=new UUID(0,2);var foot=new ContactResolver.Foot(0,0,16,16,16);
        assertEquals(b,ContactResolver.ground(foot,Map.of(a,List.of(patch(0,4),patch(0,4)),b,List.of(patch(4,10)))).orElseThrow().piece());
        assertTrue(ContactResolver.ground(new ContactResolver.Foot(0,0,16,16,17),Map.of(a,List.of(patch(0,16)))).isEmpty());
    }
    @Test void hiddenStairFaceCannotSupportFeetInsideItsUpperStep(){
        UUID stair=UUID.randomUUID();var surfaces=Map.of(stair,BoxMesh.extract(ConstructionShapes.STRAIGHT_STAIR,64));
        assertTrue(ContactResolver.ground(new ContactResolver.Foot(0,0,16,8,8),surfaces).isEmpty());
        assertTrue(ContactResolver.ground(new ContactResolver.Foot(0,8,16,16,8),surfaces).isPresent());
    }
}
