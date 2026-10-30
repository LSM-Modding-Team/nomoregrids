package net.nicomar.nomoregrids.persistence;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32C;
import net.nicomar.nomoregrids.operation.OperationPlan;

/** One append-only log per dimension. A durable commit precedes every world/resource change. */
public final class DurableJournal implements AutoCloseable {
    private static final int MAGIC=0x4e4d474a, HEADER=9;
    private final FileChannel channel;
    private final FileLock lock;
    private final Map<UUID,byte[]> prepared=new LinkedHashMap<>();
    private final LinkedHashSet<UUID> committed=new LinkedHashSet<>();
    private boolean failed;

    public DurableJournal(Path path) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        channel=FileChannel.open(path,StandardOpenOption.CREATE,StandardOpenOption.READ,StandardOpenOption.WRITE);
        FileLock acquired;
        try { acquired=channel.tryLock(); } catch (RuntimeException | IOException ex) { channel.close();throw ex; }
        if(acquired==null){channel.close();throw new IOException("Journal already locked");}
        lock=acquired;
        try { scan();channel.position(channel.size()); }
        catch(IOException | RuntimeException ex){lock.release();channel.close();throw ex;}
    }
    private void scan() throws IOException {
        channel.position(0);
        while(channel.position()<channel.size()) {
            long start=channel.position();
            ByteBuffer head=ByteBuffer.allocate(HEADER);
            // A torn tail is preserved, not silently truncated. Recovery must export/repair it.
            readFully(head);head.flip();
            if(head.getInt()!=MAGIC)throw new IOException("Journal magic at "+start);
            byte type=head.get();int length=head.getInt();
            if(length<0 || length>OperationCodec.MAX_BYTES || length>channel.size()-channel.position()-4) {
                throw new IOException("Journal torn/invalid frame at "+start+"; original retained");
            }
            ByteBuffer data=ByteBuffer.allocate(length+4);readFully(data);data.flip();
            byte[] payload=new byte[length];data.get(payload);
            CRC32C crc=new CRC32C();crc.update(head.array());crc.update(payload);
            if((int)crc.getValue()!=data.getInt())throw new IOException("Journal checksum at "+start);
            if(type==1) {
                OperationPlan plan=OperationCodec.decode(payload);
                byte[] previous=prepared.putIfAbsent(plan.id(),payload);
                if(previous!=null && !Arrays.equals(previous,payload))throw new IOException("Operation ID reused");
            } else if(type==2 && length==16) {
                UUID id=new UUID(ByteBuffer.wrap(payload).getLong(),ByteBuffer.wrap(payload).getLong(8));
                if(!prepared.containsKey(id))throw new IOException("Commit without intent");
                committed.add(id);
            } else throw new IOException("Unknown journal frame; original retained");
        }
    }
    private void readFully(ByteBuffer buffer) throws IOException {
        while(buffer.hasRemaining())if(channel.read(buffer)<0)throw new EOFException("Torn journal; original retained");
    }
    private void append(byte type,byte[] payload) throws IOException {
        if(failed)throw new IOException("Journal quarantined after failed append");
        ByteBuffer head=ByteBuffer.allocate(HEADER).putInt(MAGIC).put(type).putInt(payload.length);
        CRC32C crc=new CRC32C();crc.update(head.array());crc.update(payload);
        ByteBuffer frame=ByteBuffer.allocate(HEADER+payload.length+4).put(head.array()).put(payload).putInt((int)crc.getValue());
        frame.flip();
        try {while(frame.hasRemaining())channel.write(frame);channel.force(true);}
        catch(IOException ex){failed=true;throw ex;}
    }
    public synchronized void commit(OperationPlan plan) throws IOException {
        byte[] bytes=OperationCodec.encode(plan);
        byte[] previous=prepared.get(plan.id());
        if(previous!=null && !Arrays.equals(previous,bytes))throw new IOException("Operation ID reused with different intent");
        if(previous==null){append((byte)1,bytes);prepared.put(plan.id(),bytes);}
        if(!committed.contains(plan.id())) {
            ByteBuffer id=ByteBuffer.allocate(16).putLong(plan.id().getMostSignificantBits()).putLong(plan.id().getLeastSignificantBits());
            append((byte)2,id.array());committed.add(plan.id());
        }
    }
    public synchronized List<OperationPlan> committed() throws IOException {
        var result=new ArrayList<OperationPlan>();
        for(UUID id:committed)result.add(OperationCodec.decode(prepared.get(id)));
        return List.copyOf(result);
    }
    public synchronized boolean contains(UUID id){return committed.contains(id);}
    @Override public synchronized void close() throws IOException {try{lock.release();}finally{channel.close();}}
}
