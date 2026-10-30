package net.nicomar.nomoregrids.world;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.nicomar.nomoregrids.core.CellPos;
import net.nicomar.nomoregrids.core.GridPoint;
import net.nicomar.nomoregrids.core.PieceRecord;
import net.nicomar.nomoregrids.core.SectionPos;
import net.nicomar.nomoregrids.core.TestPieces;
import net.nicomar.nomoregrids.geometry.ConstructionShapes;
import org.junit.jupiter.api.Test;

class PieceIndexTest {
    private static final PieceIndex.Limits LIMITS = PieceIndex.Limits.DEFAULT;

    private static PieceChange place(PieceRecord piece) {
        return new PieceChange(piece.pieceId(),-1,Optional.of(piece));
    }

    @Test
    void multicellPieceHasOneAuthorityAndRemovingItClearsAllReferences() {
        PieceRecord piece = TestPieces.of(UUID.randomUUID(), ConstructionShapes.CUBE, new GridPoint(-1,-1,-1));
        PieceIndex index = PieceIndex.empty().changed(List.of(place(piece)), LIMITS);
        assertEquals(1,index.records().size());
        assertEquals(Set.of(piece.pieceId()),index.ownedBy(new SectionPos(-1,-1,-1)));
        assertTrue(index.ownedBy(new SectionPos(0,0,0)).isEmpty());
        assertEquals(Set.of(piece.pieceId()),index.referencedBy(new SectionPos(0,0,0)));
        assertEquals(Set.of(piece.pieceId()),index.at(new CellPos(0,0,0)));
        PieceIndex removed = index.changed(List.of(new PieceChange(piece.pieceId(),0,Optional.empty())), LIMITS);
        assertTrue(removed.records().isEmpty());
        assertTrue(removed.at(new CellPos(0,0,0)).isEmpty());
        assertEquals(1,index.records().size());
    }

    @Test
    void componentsRemainIndependentAndInvalidChangesLeaveOriginalUntouched() {
        PieceRecord lower = TestPieces.of(UUID.randomUUID(),ConstructionShapes.LOWER_SLAB,GridPoint.ZERO);
        PieceRecord upper = TestPieces.of(UUID.randomUUID(),ConstructionShapes.slab(4),GridPoint.ZERO);
        PieceIndex index = PieceIndex.empty().changed(List.of(place(lower),place(upper)),LIMITS);
        assertEquals(2,index.at(new CellPos(0,0,0)).size());
        PieceRecord invading = upper.moved(new GridPoint(0,-1,0),0);
        IndexException overlap = assertThrows(IndexException.class, () -> index.changed(
            List.of(new PieceChange(upper.pieceId(),0,Optional.of(invading))),LIMITS));
        assertEquals(IndexException.Code.OVERLAP,overlap.code());
        assertEquals(upper,index.records().get(upper.pieceId()));
        PieceIndex remaining = index.changed(List.of(new PieceChange(upper.pieceId(),0,Optional.empty())),LIMITS);
        assertEquals(Set.of(lower.pieceId()),remaining.at(new CellPos(0,0,0)));
    }

    @Test
    void staleRevisionAndDuplicateIdentityAreRejected() {
        PieceRecord piece = TestPieces.of(UUID.randomUUID(),ConstructionShapes.CUBE,GridPoint.ZERO);
        PieceIndex index = PieceIndex.empty().changed(List.of(place(piece)),LIMITS);
        IndexException stale = assertThrows(IndexException.class, () -> index.changed(List.of(place(piece)),LIMITS));
        assertEquals(IndexException.Code.STALE_REVISION,stale.code());
        assertThrows(IndexException.class, () -> PieceIndex.restored(List.of(piece,piece)));
        assertThrows(IndexException.class, () -> PieceIndex.empty().changed(List.of(place(piece),place(piece)),LIMITS));
        assertThrows(IllegalArgumentException.class, () -> new PieceChange(UUID.randomUUID(),0,Optional.of(piece)));
    }

    @Test
    void batchSwapChecksFinalSnapshotRatherThanIntermediateCollision() {
        PieceRecord a = TestPieces.of(UUID.randomUUID(),ConstructionShapes.CUBE,GridPoint.ZERO);
        PieceRecord b = TestPieces.of(UUID.randomUUID(),ConstructionShapes.CUBE,new GridPoint(16,0,0));
        PieceIndex before = PieceIndex.restored(List.of(a,b));
        PieceIndex after = before.changed(List.of(
            new PieceChange(a.pieceId(),0,Optional.of(a.moved(b.origin16(),0))),
            new PieceChange(b.pieceId(),0,Optional.of(b.moved(a.origin16(),0)))),LIMITS);
        assertEquals(Set.of(b.pieceId()),after.at(new CellPos(0,0,0)));
        assertEquals(Set.of(a.pieceId()),before.at(new CellPos(0,0,0)));
    }

    @Test
    void loweringLimitsAllowsRemovalAndDoesNotEraseExistingPieces() {
        PieceRecord lower = TestPieces.of(UUID.randomUUID(),ConstructionShapes.LOWER_SLAB,GridPoint.ZERO);
        PieceRecord upper = TestPieces.of(UUID.randomUUID(),ConstructionShapes.slab(4),GridPoint.ZERO);
        PieceIndex index = PieceIndex.restored(List.of(lower,upper));
        PieceIndex.Limits reduced = new PieceIndex.Limits(1,1,1);
        assertEquals(1,index.changed(List.of(new PieceChange(upper.pieceId(),0,Optional.empty())),reduced).records().size());
        PieceRecord extra = TestPieces.of(UUID.randomUUID(),ConstructionShapes.CUBE,new GridPoint(16,0,0));
        IndexException tooMany = assertThrows(IndexException.class, () -> index.changed(List.of(place(extra)),reduced));
        assertEquals(IndexException.Code.COMPLEXITY_LIMIT,tooMany.code());
        assertEquals(2,index.records().size());
    }

    @Test
    void substituteAndGroupDataCanBeRetainedButCannotBeEditedAsOrdinaryPieces() {
        PieceRecord base = TestPieces.of(UUID.randomUUID(),ConstructionShapes.CUBE,GridPoint.ZERO);
        for (PieceRecord protectedPiece : List.of(
                TestPieces.withGroupAndFlags(base,Optional.empty(),Set.of(PieceRecord.Flag.SUBSTITUTE)),
                TestPieces.withGroupAndFlags(base,Optional.of(UUID.randomUUID()),Set.of()))) {
            PieceIndex retained = PieceIndex.restored(List.of(protectedPiece));
            assertThrows(IllegalStateException.class, () -> protectedPiece.moved(new GridPoint(16,0,0),0));
            assertThrows(IndexException.class, () -> retained.changed(
                List.of(new PieceChange(protectedPiece.pieceId(),0,Optional.empty())),LIMITS));
            assertThrows(IndexException.class, () -> PieceIndex.empty().changed(List.of(place(protectedPiece)),LIMITS));
            assertEquals(protectedPiece,retained.records().get(base.pieceId()));
        }
    }
}
