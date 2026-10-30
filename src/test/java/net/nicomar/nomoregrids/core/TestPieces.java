package net.nicomar.nomoregrids.core;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.nicomar.nomoregrids.geometry.BoxUnion;

/** Test fixtures build real records; no platform behavior is simulated. */
public final class TestPieces {
    private TestPieces() {}

    public static PieceRecord of(UUID id, BoxUnion shape, GridPoint origin) {
        return new PieceRecord(id, 0, new ResourceId("nomoregrids:box"), 1,
            new ResourceId("minecraft:stone"), Map.of(), origin, 0, GridPoint.BLOCK_CENTER,
            new BoxGeometrySnapshot(shape, shape, shape), BinaryData.EMPTY, BinaryData.EMPTY,
            BinaryData.EMPTY, Optional.empty(), Set.of());
    }

    public static PieceRecord withGroupAndFlags(PieceRecord base, Optional<UUID> group, Set<PieceRecord.Flag> flags) {
        return new PieceRecord(base.pieceId(),base.revision(),base.adapterId(),base.adapterVersion(),base.sourceBlockId(),
            base.sourceState(),base.origin16(),base.orientation(),base.pivot16(),base.geometry(),base.geometryPayload(),
            base.materialPayload(),base.itemPayload(),group,flags);
    }
}
