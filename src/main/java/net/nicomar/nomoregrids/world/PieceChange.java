package net.nicomar.nomoregrids.world;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.nicomar.nomoregrids.core.PieceRecord;

/** Pure batch change. Revision -1 explicitly requires absence; empty replacement removes. */
public record PieceChange(UUID pieceId, long expectedRevision, Optional<PieceRecord> replacement) {
    public PieceChange {
        Objects.requireNonNull(pieceId);
        Objects.requireNonNull(replacement);
        if (expectedRevision < -1 || (expectedRevision == Long.MAX_VALUE && replacement.isPresent())) {
            throw new IllegalArgumentException("Invalid expected revision");
        }
        if (replacement.isEmpty() && expectedRevision == -1) throw new IllegalArgumentException("Removing absent piece");
        replacement.ifPresent(piece -> {
            if (!pieceId.equals(piece.pieceId()) || piece.revision() != expectedRevision + 1) {
                throw new IllegalArgumentException("Replacement must retain identity and increment revision");
            }
        });
    }
}
