package net.nicomar.nomoregrids.world;

import static net.nicomar.nomoregrids.world.IndexException.Code.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import net.nicomar.nomoregrids.core.CellPos;
import net.nicomar.nomoregrids.core.PieceRecord;
import net.nicomar.nomoregrids.core.SectionPos;
import net.nicomar.nomoregrids.geometry.BoxUnion;

/**
 * Immutable in-memory index of an explicitly supplied region, not a Minecraft world store.
 * Each UUID has one record; cell/section maps contain UUID references only.
 */
public final class PieceIndex {
    public record Limits(int perCell, int ownedPerSection, int referencesPerSection) {
        public static final Limits DEFAULT = new Limits(64,4096,16384);
        public Limits {
            if (perCell < 1 || ownedPerSection < 1 || referencesPerSection < 1) throw new IllegalArgumentException("Invalid index limits");
        }
    }

    private final Map<UUID, PieceRecord> records;
    private final Map<CellPos, Set<UUID>> cells;
    private final Map<SectionPos, Set<UUID>> owners;
    private final Map<SectionPos, Set<UUID>> references;

    private PieceIndex(Collection<PieceRecord> pieces) {
        Map<UUID, PieceRecord> byId = new HashMap<>();
        Map<CellPos, Set<UUID>> byCell = new TreeMap<>();
        Map<SectionPos, Set<UUID>> owned = new TreeMap<>(), referenced = new TreeMap<>();
        for (PieceRecord p : pieces) {
            if (byId.putIfAbsent(p.pieceId(), p) != null) throw new IndexException(INVALID_DATA,"Duplicate piece UUID");
            owned.computeIfAbsent(p.ownerSection(), ignored -> new HashSet<>()).add(p.pieceId());
            for (CellPos cell : p.worldOccupation().cells(8)) {
                byCell.computeIfAbsent(cell, ignored -> new HashSet<>()).add(p.pieceId());
                referenced.computeIfAbsent(cell.section(), ignored -> new HashSet<>()).add(p.pieceId());
            }
        }
        records = Map.copyOf(byId);
        cells = freeze(byCell);
        owners = freeze(owned);
        references = freeze(referenced);
    }

    public static PieceIndex empty() { return new PieceIndex(List.of()); }

    /** Validates a supplied snapshot; no filesystem or region loading occurs here. */
    public static PieceIndex from(Collection<PieceRecord> pieces, Limits limits) {
        PieceIndex index = new PieceIndex(pieces);
        index.checkBudgets(limits, null);
        index.checkOverlaps(index.records.keySet());
        return index;
    }

    private static <K> Map<K, Set<UUID>> freeze(Map<K, Set<UUID>> source) {
        Map<K, Set<UUID>> result = new HashMap<>();
        source.forEach((key, value) -> result.put(key, Set.copyOf(value)));
        return Map.copyOf(result);
    }

    public Map<UUID, PieceRecord> records() { return records; }
    public Set<UUID> at(CellPos cell) { return cells.getOrDefault(cell, Set.of()); }
    public Set<UUID> ownedBy(SectionPos section) { return owners.getOrDefault(section, Set.of()); }
    public Set<UUID> referencedBy(SectionPos section) { return references.getOrDefault(section, Set.of()); }

    /**
     * Produces a new snapshot or throws without changing this one. This does not commit
     * world/inventory effects and must not be used as a substitute for OperationPlan.
     */
    public PieceIndex changed(List<PieceChange> changes, Limits limits) {
        if (changes.isEmpty() || changes.size() > 128) throw new IndexException(COMPLEXITY_LIMIT,"Invalid batch size");
        Map<UUID, PieceRecord> next = new HashMap<>(records);
        Set<UUID> touched = new HashSet<>();
        Set<CellPos> affected = new HashSet<>();
        for (PieceChange change : changes) {
            if (!touched.add(change.pieceId())) throw new IndexException(INVALID_DATA,"Duplicate change UUID");
            PieceRecord old = records.get(change.pieceId());
            if ((old == null ? -1 : old.revision()) != change.expectedRevision()) {
                throw new IndexException(STALE_REVISION,"Piece revision differs");
            }
            if (old != null) {
                if (old.flags().contains(PieceRecord.Flag.SUBSTITUTE)) throw new IndexException(INVALID_DATA,"Substitute cannot be edited");
                affected.addAll(old.worldOccupation().cells(8));
            }
            if (change.replacement().isPresent()) {
                PieceRecord replacement = change.replacement().orElseThrow();
                if (replacement.flags().contains(PieceRecord.Flag.SUBSTITUTE)) throw new IndexException(INVALID_DATA,"Cannot place substitute");
                next.put(change.pieceId(), replacement);
                affected.addAll(replacement.worldOccupation().cells(8));
            } else {
                next.remove(change.pieceId());
            }
        }
        if (affected.size() > 512) throw new IndexException(COMPLEXITY_LIMIT,"Affected cell budget exceeded");
        PieceIndex result = new PieceIndex(next.values());
        result.checkBudgets(limits, this);
        result.checkOverlaps(touched);
        return result;
    }

    private void checkOverlaps(Set<UUID> changed) {
        Map<UUID, BoxUnion> shapes = new HashMap<>();
        for (UUID id : changed) {
            PieceRecord piece = records.get(id);
            if (piece == null) continue;
            BoxUnion shape = shapes.computeIfAbsent(id, ignored -> piece.worldOccupation());
            Set<UUID> compared = new HashSet<>();
            for (CellPos cell : shape.cells(8)) for (UUID other : at(cell)) {
                if (other.equals(id) || !compared.add(other)) continue;
                BoxUnion otherShape = shapes.computeIfAbsent(other, key -> records.get(key).worldOccupation());
                if (shape.overlaps(otherShape)) throw new IndexException(OVERLAP,"Positive volume intersection");
            }
        }
    }

    private void checkBudgets(Limits limits, PieceIndex previous) {
        checkCounts(cells, previous == null ? Map.of() : previous.cells, limits.perCell);
        checkCounts(owners, previous == null ? Map.of() : previous.owners, limits.ownedPerSection);
        checkCounts(references, previous == null ? Map.of() : previous.references, limits.referencesPerSection);
    }

    private static <K> void checkCounts(Map<K, Set<UUID>> current, Map<K, Set<UUID>> previous, int limit) {
        current.forEach((key, value) -> {
            int oldCount = previous.getOrDefault(key, Set.of()).size();
            if (value.size() > Math.max(limit, oldCount)) throw new IndexException(COMPLEXITY_LIMIT,"Index count exceeded");
        });
    }
}
