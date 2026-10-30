package net.nicomar.nomoregrids.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.nicomar.nomoregrids.geometry.ConstructionShapes;
import org.junit.jupiter.api.Test;

class PieceRecordTest {
    @Test
    void opaqueDataCannotBeMutatedThroughCallerArrays() {
        byte[] bytes = {1,2,3};
        BinaryData data = new BinaryData(bytes);
        bytes[0] = 9;
        byte[] exposed = data.copy();
        exposed[1] = 9;
        assertArrayEquals(new byte[] {1,2,3}, data.copy());
        assertEquals(new BinaryData(new byte[] {1,2,3}), data);
        assertThrows(IllegalArgumentException.class, () -> new BinaryData(new byte[BinaryData.MAX_BYTES+1]));
    }

    @Test
    void movePreservesIdentityAndUsesBoundsMinimumForOwner() {
        PieceRecord old = TestPieces.of(UUID.randomUUID(), ConstructionShapes.CUBE, new GridPoint(-1,-1,-1));
        assertEquals(new SectionPos(-1,-1,-1), old.ownerSection());
        PieceRecord moved = old.moved(new GridPoint(256,0,0), 8);
        assertEquals(old.pieceId(), moved.pieceId());
        assertEquals(1, moved.revision());
        assertEquals(0, old.revision());
        assertEquals(new GridPoint(-1,-1,-1), old.origin16());
        assertEquals(new SectionPos(1,0,0), moved.ownerSection());
        assertEquals(old.geometryPayload(), moved.geometryPayload());
    }

    @Test
    void blockStateAndFlagsAreImmutable() {
        var state = new HashMap<>(Map.of("axis", "y"));
        var geometry = new BoxGeometrySnapshot(ConstructionShapes.CUBE, ConstructionShapes.CUBE, ConstructionShapes.CUBE);
        PieceRecord piece = new PieceRecord(UUID.randomUUID(),0,new ResourceId("nomoregrids:box"),1,
            new ResourceId("minecraft:oak_log"),state,GridPoint.ZERO,0,GridPoint.BLOCK_CENTER,geometry,
            BinaryData.EMPTY,BinaryData.EMPTY,BinaryData.EMPTY,Optional.empty(),Set.of());
        state.put("axis","x");
        assertEquals("y", piece.sourceState().get("axis"));
        assertThrows(UnsupportedOperationException.class, () -> piece.sourceState().clear());
        assertThrows(UnsupportedOperationException.class, () -> piece.flags().add(PieceRecord.Flag.SUBSTITUTE));
        assertThrows(IllegalArgumentException.class, () -> new ResourceId("Minecraft:stone"));
    }
}
