package net.nicomar.nomoregrids.operation;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.nicomar.nomoregrids.core.BinaryData;
import net.nicomar.nomoregrids.core.CellPos;
import net.nicomar.nomoregrids.core.PieceRecord;
import net.nicomar.nomoregrids.world.PieceChange;

/** Complete immutable before/after images, including inventory receipt snapshots. */
public record OperationPlan(UUID id, UUID actor, Kind kind, List<Change> changes,
        BinaryData resourcesBefore, BinaryData resourcesAfter) {
    public enum Kind { PLACE, REMOVE, EDIT, REPLACE, MOVE }
    public record Change(UUID id, Optional<PieceRecord> before, Optional<PieceRecord> after) {
        public Change {
            if (before.isEmpty() && after.isEmpty()) throw new IllegalArgumentException("Empty change");
            if (before.isPresent() && !before.orElseThrow().pieceId().equals(id)
                || after.isPresent() && !after.orElseThrow().pieceId().equals(id)) throw new IllegalArgumentException("Identity changed");
            long expected = before.map(PieceRecord::revision).orElse(-1L);
            if (after.isPresent() && (expected == Long.MAX_VALUE || after.orElseThrow().revision() != expected+1)) {
                throw new IllegalArgumentException("Invalid next revision");
            }
        }
        public PieceChange indexChange() { return new PieceChange(id,before.map(PieceRecord::revision).orElse(-1L),after); }
    }
    public OperationPlan {
        java.util.Objects.requireNonNull(id); java.util.Objects.requireNonNull(actor); java.util.Objects.requireNonNull(kind);
        java.util.Objects.requireNonNull(resourcesBefore); java.util.Objects.requireNonNull(resourcesAfter);
        changes = List.copyOf(changes);
        if (changes.isEmpty() || changes.size()>128) throw new IllegalArgumentException("Operation piece budget");
        Set<UUID> ids = new HashSet<>();
        for (Change c : changes) if (!ids.add(c.id)) throw new IllegalArgumentException("Duplicate change");
        if (cells(changes).size()>512) throw new IllegalArgumentException("Operation cell budget");
    }
    private static Set<CellPos> cells(List<Change> changes) {
        Set<CellPos> cells = new HashSet<>();
        for (Change c : changes) {
            c.before.ifPresent(p -> cells.addAll(p.worldOccupation().cells(8)));
            c.after.ifPresent(p -> cells.addAll(p.worldOccupation().cells(8)));
        }
        return Set.copyOf(cells);
    }
    public Set<CellPos> affectedCells() { return cells(changes); }
}
