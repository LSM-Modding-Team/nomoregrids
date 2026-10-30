package net.nicomar.nomoregrids.network;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Instantiate per connection. Size validation precedes assembly allocation. Clock is monotonic nanos. */
public final class SnapshotAssembler {
    public static final int CHUNK=64*1024, MAX=2*1024*1024, IN_FLIGHT=4;
    private static final long TTL=10_000_000_000L;
    public record Fragment(UUID message,int totalBytes,int index,byte[] digest,byte[] data) {
        public Fragment {
            if(totalBytes<1 || totalBytes>MAX || index<0 || index>=(totalBytes+CHUNK-1)/CHUNK
                || digest.length!=32 || data.length!=Math.min(CHUNK,totalBytes-index*CHUNK)) {
                throw new IllegalArgumentException("Invalid snapshot fragment");
            }
            digest=digest.clone();data=data.clone();
        }
        @Override public byte[] digest(){return digest.clone();}
        @Override public byte[] data(){return data.clone();}
    }
    private static final class Pending {
        final int total;final byte[] hash,bytes;final BitSet received=new BitSet();final long started;
        Pending(Fragment f,long now){total=f.totalBytes;hash=f.digest();bytes=new byte[total];started=now;}
    }
    private final Map<UUID,Pending> pending=new HashMap<>();
    public synchronized Optional<byte[]> accept(Fragment fragment,long now) throws IOException {
        pending.values().removeIf(p -> now-p.started>=TTL);
        Pending p=pending.get(fragment.message);
        if(p==null){if(pending.size()>=IN_FLIGHT)throw new IOException("Assembly budget exceeded");p=new Pending(fragment,now);pending.put(fragment.message,p);}
        if(p.total!=fragment.totalBytes || !Arrays.equals(p.hash,fragment.digest)) {
            pending.remove(fragment.message);throw new IOException("Conflicting snapshot header");
        }
        int offset=fragment.index*CHUNK;
        if(p.received.get(fragment.index)) {
            if(!Arrays.equals(p.bytes,offset,offset+fragment.data.length,fragment.data,0,fragment.data.length)) {
                pending.remove(fragment.message);throw new IOException("Conflicting duplicate snapshot");
            }
        } else {System.arraycopy(fragment.data,0,p.bytes,offset,fragment.data.length);p.received.set(fragment.index);}
        if(p.received.cardinality()!=(p.total+CHUNK-1)/CHUNK)return Optional.empty();
        pending.remove(fragment.message);
        if(!MessageDigest.isEqual(p.hash,sha256(p.bytes)))throw new IOException("Snapshot hash mismatch");
        return Optional.of(p.bytes);
    }
    public synchronized void clear(){pending.clear();}
    public static byte[] sha256(byte[] bytes){try{return MessageDigest.getInstance("SHA-256").digest(bytes);}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
    public static List<Fragment> split(UUID message,byte[] bytes){
        if(bytes.length<1 || bytes.length>MAX)throw new IllegalArgumentException("Snapshot size");
        byte[] hash=sha256(bytes);var list=new ArrayList<Fragment>();
        for(int i=0;i<(bytes.length+CHUNK-1)/CHUNK;i++)list.add(new Fragment(message,bytes.length,i,hash,
            Arrays.copyOfRange(bytes,i*CHUNK,Math.min(bytes.length,(i+1)*CHUNK))));
        return List.copyOf(list);
    }
}
