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
        BinaryData resourcesBefore, BinaryData resourcesAfter, List<Effect> effects) {
    public enum Kind { PLACE, REMOVE, EDIT, REPLACE, MOVE, SYSTEM, CLAIM, TRANSFER }
    public static final UUID WORLD_ACTOR=new UUID(0,0);
    public record Effect(String key,CellPos owner,Optional<BinaryData> before,Optional<BinaryData> after){
        public Effect{java.util.Objects.requireNonNull(owner);java.util.Objects.requireNonNull(before);java.util.Objects.requireNonNull(after);
            if(!key.matches("(fluid|machine|loot|circuit|group|native):[a-zA-Z0-9_:.-]{1,192}")||before.equals(after))throw new IllegalArgumentException("Invalid world effect");}
    }
    public OperationPlan(UUID id,UUID actor,Kind kind,List<Change> changes,BinaryData before,BinaryData after){this(id,actor,kind,changes,before,after,List.of());}
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
        changes = List.copyOf(changes);effects=List.copyOf(effects);
        if(effects.size()>512)throw new IllegalArgumentException("Effect budget");
        var keys=new HashSet<String>();for(var e:effects)if(!keys.add(e.key()))throw new IllegalArgumentException("Duplicate world effect");
        if(actor.equals(WORLD_ACTOR)&&(kind!=Kind.SYSTEM||resourcesBefore.size()!=0||resourcesAfter.size()!=0))throw new IllegalArgumentException("Invalid system actor");
        if (changes.isEmpty()&&effects.isEmpty() || changes.size()>128) throw new IllegalArgumentException("Operation piece budget");
        Set<UUID> ids = new HashSet<>();
        for (Change c : changes) if (!ids.add(c.id)) throw new IllegalArgumentException("Duplicate change");
        if (allCells(changes,effects).size()>512) throw new IllegalArgumentException("Operation cell budget");
    }
    private static Set<CellPos> cells(List<Change> changes) {
        Set<CellPos> cells = new HashSet<>();
        for (Change c : changes) {
            c.before.ifPresent(p -> cells.addAll(p.worldOccupation().cells(8)));
            c.after.ifPresent(p -> cells.addAll(p.worldOccupation().cells(8)));
        }
        return Set.copyOf(cells);
    }
    private static Set<CellPos> allCells(List<Change> changes,List<Effect> effects){var cells=new HashSet<>(cells(changes));effects.forEach(e->cells.add(e.owner()));return Set.copyOf(cells);}
    public Set<CellPos> pieceCells(){return cells(changes);}
    public Set<CellPos> affectedCells() { return allCells(changes,effects); }
}
