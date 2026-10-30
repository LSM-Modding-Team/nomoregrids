package net.nicomar.nomoregrids.diagnostic;

import java.util.*;
import java.util.function.LongSupplier;

/** Coalesced deferred work with a monotonic time budget and fair round-robin continuation. */
public final class TickWorkQueue<K> {
    public interface Work { boolean advance(int units); }
    private final LinkedHashMap<K,Work> pending=new LinkedHashMap<>();
    private final int maxJobs;
    public TickWorkQueue(int maxJobs){if(maxJobs<1)throw new IllegalArgumentException("Job budget");this.maxJobs=maxJobs;}
    public void submit(K key,Work newest){if(!pending.containsKey(key)&&pending.size()>=maxJobs)throw new IllegalArgumentException("Work queue full");pending.put(key,newest);}
    public int advance(long nanos,int unitsPerJob,LongSupplier clock){
        if(nanos<1||unitsPerJob<1)throw new IllegalArgumentException("Work budget");long start=clock.getAsLong();int completed=0;
        while(!pending.isEmpty()&&clock.getAsLong()-start<nanos){var first=pending.entrySet().iterator().next();K key=first.getKey();Work work=first.getValue();pending.remove(key);
            if(work.advance(unitsPerJob))completed++;else pending.put(key,work);}
        return completed;
    }
    public void cancel(K key){pending.remove(key);}public int pending(){return pending.size();}
}
