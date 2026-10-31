package net.nicomar.nomoregrids.world;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.ConstructionShapes;

class GroupAuthorityTest {
    @Test void completeGroupCanBeCreatedAndMovedButNeverSplit(){
        var group=UUID.randomUUID();var a=TestPieces.withGroupAndFlags(TestPieces.of(UUID.randomUUID(),ConstructionShapes.CUBE,GridPoint.ZERO),Optional.of(group),Set.of());var b=TestPieces.withGroupAndFlags(TestPieces.of(UUID.randomUUID(),ConstructionShapes.CUBE,new GridPoint(16,0,0)),Optional.of(group),Set.of());
        assertThrows(IndexException.class,()->PieceIndex.empty().changed(List.of(new PieceChange(a.pieceId(),-1,Optional.of(a))),PieceIndex.Limits.DEFAULT));
        var index=PieceIndex.empty().changed(List.of(new PieceChange(a.pieceId(),-1,Optional.of(a)),new PieceChange(b.pieceId(),-1,Optional.of(b))),PieceIndex.Limits.DEFAULT);
        assertEquals(Set.of(a.pieceId(),b.pieceId()),index.groupMembers().get(group));
        assertThrows(IllegalArgumentException.class,()->index.changed(List.of(new PieceChange(a.pieceId(),0,Optional.empty())),PieceIndex.Limits.DEFAULT));
        assertThrows(IndexException.class,()->index.changed(List.of(new PieceChange(a.pieceId(),0,Optional.empty()),new PieceChange(b.pieceId(),0,Optional.of(moved(b)))),PieceIndex.Limits.DEFAULT));
        var moved=index.changed(List.of(new PieceChange(a.pieceId(),0,Optional.of(moved(a))),new PieceChange(b.pieceId(),0,Optional.of(moved(b)))),PieceIndex.Limits.DEFAULT);assertEquals(group,moved.records().get(a.pieceId()).groupId().orElseThrow());assertEquals(index.groupMembers(),moved.groupMembers());
        assertTrue(index.changed(List.of(new PieceChange(a.pieceId(),0,Optional.empty()),new PieceChange(b.pieceId(),0,Optional.empty())),PieceIndex.Limits.DEFAULT).records().isEmpty());
    }
    @Test void effectsOnlyPlanKeepsTheSameGeometryAuthority(){var empty=PieceIndex.empty();assertSame(empty,empty.changed(List.of(),PieceIndex.Limits.DEFAULT));}
    private PieceRecord moved(PieceRecord p){return new PieceRecord(p.pieceId(),p.revision()+1,p.adapterId(),p.adapterVersion(),p.sourceBlockId(),p.sourceState(),p.origin16().add(new GridPoint(0,16,0)),p.orientation(),p.pivot16(),p.geometry(),p.geometryPayload(),p.materialPayload(),p.itemPayload(),p.groupId(),p.flags());}
}
