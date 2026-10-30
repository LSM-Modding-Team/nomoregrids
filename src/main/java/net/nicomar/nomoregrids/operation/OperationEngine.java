package net.nicomar.nomoregrids.operation;

import java.io.IOException;
import java.util.*;
import net.nicomar.nomoregrids.persistence.DurableJournal;
import net.nicomar.nomoregrids.world.PieceIndex;

/** Server-thread coordinator. Failed post-commit operations are quarantined, never partially rolled back. */
public final class OperationEngine {
    public interface Host {
        void assertServerThread();
        /** Must verify loading, reach, permissions, entities, support, adapters and resource-before image. */
        void validate(OperationPlan plan,PieceIndex before,PieceIndex after);
        /** Compare before/after receipt, apply at most once; conflicts must throw, not guess. */
        void install(OperationPlan plan,PieceIndex after) throws IOException;
        /** True only when both resource receipt and all world checkpoints are durably applied. */
        boolean alreadyApplied(UUID operation) throws IOException;
        void afterCommit(OperationPlan plan);
    }
    private final DurableJournal journal;
    private final Host host;
    private final PieceIndex.Limits limits;
    private PieceIndex index;
    private boolean quarantined;

    public OperationEngine(PieceIndex index,DurableJournal journal,Host host,PieceIndex.Limits limits){
        this.index=Objects.requireNonNull(index);this.journal=Objects.requireNonNull(journal);
        this.host=Objects.requireNonNull(host);this.limits=Objects.requireNonNull(limits);
    }
    public PieceIndex snapshot(){return index;}
    public void execute(OperationPlan plan) throws IOException {
        host.assertServerThread();if(quarantined)throw new IOException("Operation engine quarantined");
        if(journal.contains(plan.id())) {
            // commit() also verifies that a repeated ID has byte-identical intent.
            journal.commit(plan);return;
        }
        for(var change:plan.changes()) {
            if(!Objects.equals(Optional.ofNullable(index.records().get(change.id())),change.before())) {
                throw new IllegalArgumentException("STALE_REVISION");
            }
        }
        PieceIndex next=index.changed(plan.changes().stream().map(OperationPlan.Change::indexChange).toList(),limits);
        host.validate(plan,index,next);
        journal.commit(plan);
        try {host.install(plan,next);index=next;host.afterCommit(plan);}
        catch(IOException | RuntimeException ex){quarantined=true;throw new IOException("Committed operation requires recovery: "+plan.id(),ex);}
    }
    /** Replay only records whose actual state is before or after; never consume resources blindly. */
    public void recover() throws IOException {
        host.assertServerThread();
        try {
            for(var plan:journal.committed()) {
                if(host.alreadyApplied(plan.id()))continue;
                var records=new HashMap<>(index.records());
                for(var c:plan.changes()) {
                    var current=Optional.ofNullable(records.get(c.id()));
                    if(!current.equals(c.before()) && !current.equals(c.after()))throw new IOException("Conflicting piece during replay: "+c.id());
                    if(c.after().isPresent())records.put(c.id(),c.after().orElseThrow());else records.remove(c.id());
                }
                PieceIndex next=PieceIndex.restored(records.values());
                host.install(plan,next);index=next;
            }
            quarantined=false;
        } catch(IOException | RuntimeException ex){quarantined=true;throw new IOException("Recovery conflict; data retained",ex);}
    }
}
