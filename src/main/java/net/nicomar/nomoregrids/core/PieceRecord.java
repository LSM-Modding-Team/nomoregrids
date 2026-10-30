package net.nicomar.nomoregrids.core;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.nicomar.nomoregrids.geometry.BoxUnion;
import net.nicomar.nomoregrids.geometry.Rotation;

/** Immutable authoritative record for the initial integral box families. Not a block entity. */
public record PieceRecord(UUID pieceId, long revision, ResourceId adapterId, int adapterVersion,
        ResourceId sourceBlockId, Map<String, String> sourceState, GridPoint origin16,
        int orientation, GridPoint pivot16, BoxGeometrySnapshot geometry,
        BinaryData geometryPayload, BinaryData materialPayload, BinaryData itemPayload,
        Optional<UUID> groupId, Set<Flag> flags) {
    public enum Flag { CONNECTIONS_LOCKED, SUBSTITUTE }

    public PieceRecord {
        Objects.requireNonNull(pieceId);
        Objects.requireNonNull(adapterId);
        Objects.requireNonNull(sourceBlockId);
        Objects.requireNonNull(origin16);
        Objects.requireNonNull(pivot16);
        Objects.requireNonNull(geometry);
        Objects.requireNonNull(groupId);
        if (revision < 0 || adapterVersion < 1) throw new IllegalArgumentException("Invalid revision/version");
        Rotation.of(orientation);
        if (sourceState.size() > 64) throw new IllegalArgumentException("Too many block properties");
        sourceState = Map.copyOf(sourceState);
        for (var entry : sourceState.entrySet()) {
            if (!entry.getKey().matches("[a-z0-9_]{1,128}") || !entry.getValue().matches("[a-z0-9_]{1,128}")) {
                throw new IllegalArgumentException("Invalid block property");
            }
        }
        flags = Set.copyOf(flags);
        if (geometryPayload.size() + materialPayload.size() + itemPayload.size() > BinaryData.MAX_BYTES) {
            throw new IllegalArgumentException("Combined payload budget exceeded");
        }
        // Fail on transform overflow before admitting a record to any index.
        geometry.occupation().transform(Rotation.of(orientation), pivot16, origin16).cells(8);
    }

    public BoxUnion worldOccupation() {
        return geometry.occupation().transform(Rotation.of(orientation), pivot16, origin16);
    }

    public SectionPos ownerSection() { return worldOccupation().bounds().min().cell().section(); }

    public PieceRecord moved(GridPoint origin, int rotation) {
        if (flags.contains(Flag.SUBSTITUTE)) throw new IllegalStateException("Substitute cannot be edited");
        return new PieceRecord(pieceId, Math.incrementExact(revision), adapterId, adapterVersion,
            sourceBlockId, sourceState, origin, rotation, pivot16, geometry, geometryPayload,
            materialPayload, itemPayload, groupId, flags);
    }
}
