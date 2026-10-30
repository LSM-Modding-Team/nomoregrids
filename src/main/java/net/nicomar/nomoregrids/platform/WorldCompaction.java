package net.nicomar.nomoregrids.platform;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.operation.OperationPlan;
import net.nicomar.nomoregrids.persistence.*;

/** Immutable checkpoint preparation and incremental tail copy; publication remains on the server thread. */
public final class WorldCompaction implements AutoCloseable {
    public record Input(Path root,JournalArchive.State state,JournalArchive previous,Map<UUID,OperationPlan> plans,Map<UUID,Long> sequences,Set<UUID> known){public Input{plans=Map.copyOf(plans);sequences=Map.copyOf(sequences);known=Set.copyOf(known);}}
    public record Prepared(JournalArchive archive,DurableJournal journal,long copied){}
    public record Status(boolean running,long copied,long failures,String lastFailure){}
    private final ExecutorService worker=Executors.newSingleThreadExecutor(task->{var thread=new Thread(task,"NMG checkpoint I/O");thread.setDaemon(true);return thread;});
    private Future<Prepared> pending;private Prepared prepared;private long failures;private String lastFailure="";
    public boolean running(){return pending!=null||prepared!=null;}
    public Status status(){return new Status(running(),prepared==null?0:prepared.copied,failures,lastFailure);}
    public void start(Input input){if(running())throw new IllegalStateException("Checkpoint already running");pending=worker.submit(()->{var stamps=new HashMap<UUID,JournalArchive.Stamp>();for(var plan:input.plans.values()){long n=input.sequences.get(plan.id());stamps.put(plan.id(),new JournalArchive.Stamp(plan.id(),plan.actor(),plan.kind(),n,net.nicomar.nomoregrids.network.SnapshotAssembler.sha256(OperationCodec.encode(plan))));}var archive=JournalArchive.prepare(input.root,input.state,input.previous,stamps,input.known);archive.seal();return new Prepared(archive,new DurableJournal(archive.activeLog()),input.state.sequence());});}
    public Optional<Prepared> poll()throws IOException{if(pending!=null&&pending.isDone())try{prepared=pending.get();pending=null;}catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IOException("Checkpoint interrupted",ex);}catch(ExecutionException ex){pending=null;failures++;lastFailure=Objects.toString(ex.getCause());throw new IOException("Checkpoint preparation failed; active journal retained",ex.getCause());}return pending==null?Optional.ofNullable(prepared):Optional.empty();}
    public void copy(List<OperationPlan> tail){if(prepared==null||pending!=null||tail.isEmpty())throw new IllegalStateException("Checkpoint tail state");var current=prepared;prepared=null;pending=worker.submit(()->{try{for(var plan:tail)current.journal.commit(plan);return new Prepared(current.archive,current.journal,Math.addExact(current.copied,tail.size()));}catch(IOException|RuntimeException ex){try{current.journal.close();}catch(IOException closing){ex.addSuppressed(closing);}throw ex;}});}
    public void adopted(){if(pending!=null||prepared==null)throw new IllegalStateException("Unfinished checkpoint");prepared=null;}
    public void prune(JournalArchive archive){worker.submit(()->{try{archive.prune();}catch(IOException ex){net.nicomar.nomoregrids.NoMoreGrids.LOGGER.warn("Checkpoint published; older files retained after cleanup failure",ex);}});}
    @Override public void close()throws IOException{worker.shutdown();try{while(!worker.awaitTermination(1,TimeUnit.SECONDS)){/* Close is called during dimension shutdown; do not abandon a locked prepared journal. */}}catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IOException("Interrupted checkpoint shutdown",ex);}var current=prepared;if(pending!=null)try{current=pending.get();}catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IOException(ex);}catch(ExecutionException ex){current=null;}if(current!=null)current.journal.close();}
}
