package net.nicomar.nomoregrids.diagnostic;

import java.util.Arrays;

/** Stores observations only. No fabricated benchmark result or universal performance guarantee. */
public final class TimingSamples {
    private final long[] ring;private int count,cursor;
    public TimingSamples(int capacity){if(capacity<1)throw new IllegalArgumentException("Sample capacity");ring=new long[capacity];}
    public synchronized void add(long nanos){if(nanos<0)throw new IllegalArgumentException("Negative duration");ring[cursor++]=nanos;cursor%=ring.length;count=Math.min(count+1,ring.length);}
    public synchronized long percentile(double percentile){
        if(count==0)throw new IllegalStateException("No measurements");if(percentile<0||percentile>1||!Double.isFinite(percentile))throw new IllegalArgumentException("Percentile");
        long[] sorted=Arrays.copyOf(ring,count);Arrays.sort(sorted);return sorted[Math.max(0,(int)Math.ceil(percentile*count)-1)];
    }
    public synchronized int count(){return count;}
}
