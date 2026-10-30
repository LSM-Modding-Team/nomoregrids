package net.nicomar.nomoregrids.simulation.functional;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.nicomar.nomoregrids.core.*;
import org.junit.jupiter.api.Test;

class FunctionalStoreTest {
    @Test void referencesResolveToOneAuthorityAndStaleEditsAreRejected(){
        UUID id=UUID.randomUUID();var store=new FunctionalStore();
        var old=new FunctionalStore.State(id,0,List.of(BinaryData.EMPTY),0,BinaryData.EMPTY);store.restore(old);
        var next=old.edited(List.of(new BinaryData(new byte[]{1})),10,BinaryData.EMPTY);store.replace(old,next);
        assertSame(next,store.require(id));assertThrows(IllegalArgumentException.class,()->store.replace(old,next));
        assertEquals(1,store.snapshot().size());
    }
    @Test void multiplePortsAtSameContactDoNotMergeInventories(){
        var cap=new ResourceId("nomoregrids:items");var pos=GridPoint.ZERO;
        var a=new FunctionalStore.Port(UUID.randomUUID(),"a",pos,Direction6.NORTH,cap,true,true);
        var b=new FunctionalStore.Port(UUID.randomUUID(),"b",pos,Direction6.NORTH,cap,true,true);
        assertEquals(Optional.of(a),FunctionalStore.route(List.of(a),pos,Direction6.NORTH,cap));
        assertTrue(FunctionalStore.route(List.of(a,b),pos,Direction6.NORTH,cap).isEmpty());
    }
    @Test void furnaceProducesOnceAndDoesNotConsumeFuelWhenOutputIsBlocked(){
        var ore=new ResourceId("minecraft:iron_ore");var ingot=new ResourceId("minecraft:iron_ingot");var coal=new ResourceId("minecraft:coal");
        var recipe=new FurnaceLogic.Recipe(ore,new FurnaceLogic.Stack(ingot,1),2,0.7f);
        var initial=new FurnaceLogic.State(Optional.of(new FurnaceLogic.Stack(ore,1)),Optional.of(new FurnaceLogic.Stack(coal,1)),Optional.empty(),0,0,0);
        var t=FurnaceLogic.tick(initial,Optional.of(recipe),id->Optional.of(new FurnaceLogic.Fuel(100,Optional.empty()))).state();
        var done=FurnaceLogic.tick(t,Optional.of(recipe),id->Optional.empty()).state();assertTrue(done.input().isEmpty());assertEquals(1,done.output().orElseThrow().count());
        var blocked=new FurnaceLogic.State(initial.input(),initial.fuel(),Optional.of(new FurnaceLogic.Stack(ingot,64)),0,0,0);
        assertEquals(blocked,FurnaceLogic.tick(blocked,Optional.of(recipe),id->Optional.of(new FurnaceLogic.Fuel(100,Optional.empty()))).state());
    }
}
