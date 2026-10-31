package net.nicomar.nomoregrids.diagnostic;

/** Application body counts at enqueue time; excludes headers, compression and socket acknowledgement. */
public final class TrafficMeter {
    private long messages,deltas,encodedBodyBytes,fragmentCopies,recipientBodyBytes;
    public synchronized void prepared(int bytes,boolean delta){if(bytes<1)throw new IllegalArgumentException("Empty body");messages++;if(delta)deltas++;encodedBodyBytes+=bytes;}
    public synchronized void enqueued(int bytes,int recipients){if(bytes<1||recipients<0)throw new IllegalArgumentException("Traffic observation");fragmentCopies+=recipients;recipientBodyBytes+=(long)bytes*recipients;}
    public record Snapshot(long messages,long deltas,long encodedBodyBytes,long fragmentCopies,long recipientBodyBytes){}
    public synchronized Snapshot snapshot(){return new Snapshot(messages,deltas,encodedBodyBytes,fragmentCopies,recipientBodyBytes);}
}
