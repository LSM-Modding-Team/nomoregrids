package net.nicomar.nomoregrids.diagnostic;

import java.util.Arrays;

/** Finite measurement window: warmup is excluded and no measured sample is evicted. */
public final class MeasurementWindow {
    public static final int MAX_SAMPLES=12000;
    private final int warmup;
    private final long[] samples;
    private int skipped,count;
    public MeasurementWindow(int warmup,int measured){
        if(warmup<0||warmup>MAX_SAMPLES||measured<1||measured>MAX_SAMPLES)throw new IllegalArgumentException("Measurement window budget");
        this.warmup=warmup;samples=new long[measured];
    }
    public synchronized boolean add(long nanos){
        if(nanos<0)throw new IllegalArgumentException("Negative duration");
        if(skipped<warmup){skipped++;return false;}
        if(count<samples.length)samples[count++]=nanos;
        return count==samples.length;
    }
    public synchronized boolean warmingUp(){return skipped<warmup;}
    public record Summary(int warmupSamples,int samples,int target,boolean complete,Long p50Nanos,Long p95Nanos,Long p99Nanos,Double meanNanos){}
    public synchronized Summary summary(){
        if(count==0)return new Summary(skipped,0,samples.length,false,null,null,null,null);
        var sorted=Arrays.copyOf(samples,count);Arrays.sort(sorted);double sum=0;for(long sample:sorted)sum+=sample;
        return new Summary(skipped,count,samples.length,count==samples.length,percentile(sorted,.5),percentile(sorted,.95),percentile(sorted,.99),sum/count);
    }
    private static long percentile(long[] sorted,double p){return sorted[Math.max(0,(int)Math.ceil(sorted.length*p)-1)];}
}
