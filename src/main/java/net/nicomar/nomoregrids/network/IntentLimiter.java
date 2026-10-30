package net.nicomar.nomoregrids.network;

/** Token bucket per player/connection: 20/s, burst 40, no wall-clock dependency. */
public final class IntentLimiter {
    private double tokens=40;
    private long previous;
    private boolean started;
    public synchronized boolean allow(long nowNanos) {
        if(!started){previous=nowNanos;started=true;}
        long elapsed=nowNanos-previous;
        if(elapsed<0)throw new IllegalArgumentException("Clock moved backwards");
        tokens=Math.min(40,tokens+elapsed/1_000_000_000.0*20);previous=nowNanos;
        if(tokens<1)return false;
        tokens-=1;return true;
    }
}
