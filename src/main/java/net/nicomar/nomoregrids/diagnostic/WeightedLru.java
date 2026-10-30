package net.nicomar.nomoregrids.diagnostic;

import java.util.*;

/** Bounded weighted LRU. Oversized values are returned to the caller but never retained. */
public final class WeightedLru<K,V> {
    private record Entry<V>(V value,long bytes){}
    private final long capacity;
    private long used,hits,misses;
    private final LinkedHashMap<K,Entry<V>> values=new LinkedHashMap<>(16,0.75f,true);
    public WeightedLru(long capacity){if(capacity<0)throw new IllegalArgumentException("Negative capacity");this.capacity=capacity;}
    public synchronized Optional<V> get(K key){var e=values.get(key);if(e==null){misses++;return Optional.empty();}hits++;return Optional.of(e.value);}
    public synchronized void put(K key,V value,long bytes){
        Objects.requireNonNull(key);Objects.requireNonNull(value);if(bytes<0)throw new IllegalArgumentException("Negative weight");
        var old=values.remove(key);if(old!=null)used-=old.bytes;if(bytes>capacity)return;
        while(used>capacity-bytes){var it=values.entrySet().iterator();var first=it.next();used-=first.getValue().bytes;it.remove();}
        values.put(key,new Entry<>(value,bytes));used+=bytes;
    }
    public synchronized void remove(K key){var old=values.remove(key);if(old!=null)used-=old.bytes;}
    public synchronized void clear(){values.clear();used=0;}
    public record Stats(long bytes,int entries,long hits,long misses){}
    public synchronized Stats stats(){return new Stats(used,values.size(),hits,misses);}
}
