package net.nicomar.nomoregrids.persistence;

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.CRC32C;
import com.google.gson.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.operation.OperationPlan;
import net.nicomar.nomoregrids.network.SnapshotAssembler;
import net.nicomar.nomoregrids.diagnostic.WeightedLru;

/** Immutable checkpoint generations with disk-backed sorted operation/piece ledgers and an atomic manifest. */
public final class JournalArchive {
    private static final int MAX_STATE=64*1024*1024,HEADER=12,STAMP=77,PIECE=20;
    public record Stamp(UUID id,UUID actor,OperationPlan.Kind kind,long sequence,byte[] digest){public Stamp{digest=digest.clone();}@Override public byte[] digest(){return digest.clone();}}
    public record State(long sequence,Map<UUID,PieceRecord> records,Map<String,WorldEffectStore.Value> effects,Map<UUID,Long> receipts){public State{if(sequence<0)throw new IllegalArgumentException("Checkpoint sequence");records=Map.copyOf(records);effects=Map.copyOf(effects);receipts=Map.copyOf(receipts);}}
    private String obsolete;private boolean hadPrevious;
    private final Path root,directory;private final String generation;private final State state;
    private final WeightedLru<UUID,Optional<Stamp>> cache=new WeightedLru<>(4*1024*1024);
    private JournalArchive(Path root,String generation,State state){this.root=root;this.generation=generation;this.directory=root.resolve("generations").resolve(generation);this.state=state;}
    public State state(){return state;}
    public Path activeLog(){return directory.resolve("active.log");}
    public static Optional<JournalArchive> open(Path root)throws IOException{
        Path pointer=root.resolve("active.json");if(!Files.exists(pointer))return Optional.empty();if(Files.size(pointer)>4096)throw new IOException("Journal manifest budget");
        try{var manifest=JsonParser.parseString(Files.readString(pointer)).getAsJsonObject();if(manifest.get("schema").getAsInt()!=1)throw new IOException("Journal manifest schema");String generation=UUID.fromString(manifest.get("generation").getAsString()).toString();Path dir=root.resolve("generations").resolve(generation);
            for(String file:List.of("state.bin","operations.idx","pieces.idx"))if(!hash(dir.resolve(file)).equals(manifest.get(file).getAsString()))throw new IOException("Archive checksum mismatch; original retained: "+file);
            var archive=new JournalArchive(root,generation,decode(Files.readAllBytes(bounded(dir.resolve("state.bin"),MAX_STATE))));archive.verifyLedger("operations.idx",STAMP);archive.verifyLedger("pieces.idx",PIECE);if(!Files.isRegularFile(archive.activeLog()))throw new IOException("Missing archive journal; original retained");return Optional.of(archive);
        }catch(RuntimeException ex){throw new IOException("Invalid journal manifest; original retained",ex);}
    }
    private static Path bounded(Path file,long max)throws IOException{if(Files.size(file)>max)throw new IOException("Archive state budget; original retained");return file;}
    private static String hash(Path file)throws IOException{
        try{var digest=MessageDigest.getInstance("SHA-256");try(var stream=Files.newInputStream(file)){byte[] buffer=new byte[8192];for(int n;(n=stream.read(buffer))>=0;)digest.update(buffer,0,n);}return HexFormat.of().formatHex(digest.digest());}catch(NoSuchAlgorithmException ex){throw new AssertionError(ex);}
    }
    private long verifyLedger(String file,int size)throws IOException{
        try(var ch=FileChannel.open(directory.resolve(file),StandardOpenOption.READ)){var header=ByteBuffer.allocate(HEADER);read(ch,header,0);header.flip();if(header.getInt()!=1)throw new IOException("Archive ledger schema");long count=header.getLong();if(count<0||count>(Long.MAX_VALUE-HEADER)/size||ch.size()!=HEADER+count*size)throw new IOException("Archive ledger length");return count;}
    }
    public Optional<Stamp> operation(UUID id)throws IOException{
        var cached=cache.get(id);if(cached.isPresent())return cached.orElseThrow();var row=find("operations.idx",STAMP,id);var result=row==null?Optional.<Stamp>empty():Optional.of(stamp(row));cache.put(id,result,128);return result;
    }
    public boolean knownPiece(UUID id)throws IOException{return find("pieces.idx",PIECE,id)!=null;}
    private byte[] find(String file,int size,UUID id)throws IOException{
        long count=verifyLedger(file,size);try(var ch=FileChannel.open(directory.resolve(file),StandardOpenOption.READ)){long lo=0,hi=count-1;while(lo<=hi){long mid=lo+(hi-lo)/2;byte[] row=row(ch,mid,size);var found=new UUID(ByteBuffer.wrap(row).getLong(),ByteBuffer.wrap(row).getLong(8));int compare=found.compareTo(id);if(compare==0)return row;if(compare<0)lo=mid+1;else hi=mid-1;}return null;}
    }
    private static void read(FileChannel ch,ByteBuffer bytes,long position)throws IOException{while(bytes.hasRemaining()){int n=ch.read(bytes,position+bytes.position());if(n<0)throw new EOFException("Truncated archive ledger");}}
    private static byte[] row(FileChannel ch,long index,int size)throws IOException{var b=ByteBuffer.allocate(size);read(ch,b,HEADER+index*size);byte[] bytes=b.array();var crc=new CRC32C();crc.update(bytes,0,size-4);if((int)crc.getValue()!=ByteBuffer.wrap(bytes).getInt(size-4))throw new IOException("Archive ledger checksum");return bytes;}
    private static Stamp stamp(byte[] bytes)throws IOException{var in=new DataInputStream(new ByteArrayInputStream(bytes));UUID id=OperationCodec.uuid(in),actor=OperationCodec.uuid(in);int kind=in.readUnsignedByte();if(kind>=OperationPlan.Kind.values().length)throw new IOException("Archive operation kind");long sequence=in.readLong();if(sequence<1)throw new IOException("Archive operation sequence");return new Stamp(id,actor,OperationPlan.Kind.values()[kind],sequence,in.readNBytes(32));}
    private static byte[] stamp(Stamp value)throws IOException{var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);OperationCodec.uuid(out,value.id);OperationCodec.uuid(out,value.actor);out.writeByte(value.kind.ordinal());out.writeLong(value.sequence);out.write(value.digest);return checked(bytes.toByteArray());}
    private static byte[] piece(UUID id)throws IOException{var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);OperationCodec.uuid(out,id);return checked(bytes.toByteArray());}
    private static byte[] checked(byte[] bytes)throws IOException{var crc=new CRC32C();crc.update(bytes);var out=new ByteArrayOutputStream();out.write(bytes);new DataOutputStream(out).writeInt((int)crc.getValue());return out.toByteArray();}
    private static byte[] encode(State state)throws IOException{
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeInt(1);out.writeLong(state.sequence);out.writeInt(state.receipts.size());for(var id:new TreeSet<>(state.receipts.keySet())){OperationCodec.uuid(out,id);out.writeLong(state.receipts.get(id));}
        out.writeInt(state.records.size());for(var id:new TreeSet<>(state.records.keySet())){OperationCodec.blob(out,BoxPieceCodec.encode(state.records.get(id)));if(bytes.size()>MAX_STATE-4)throw new IOException("Archive state budget");}
        out.writeInt(state.effects.size());for(var key:new TreeSet<>(state.effects.keySet())){var value=state.effects.get(key);out.writeUTF(key);out.writeLong(value.owner().x());out.writeLong(value.owner().y());out.writeLong(value.owner().z());OperationCodec.blob(out,value.data().copy());if(bytes.size()>MAX_STATE-4)throw new IOException("Archive state budget");}return checked(bytes.toByteArray());
    }
    private static State decode(byte[] bytes)throws IOException{
        if(bytes.length<24||bytes.length>MAX_STATE)throw new IOException("Archive state length");var crc=new CRC32C();crc.update(bytes,0,bytes.length-4);if((int)crc.getValue()!=ByteBuffer.wrap(bytes).getInt(bytes.length-4))throw new IOException("Archive state CRC");var in=new DataInputStream(new ByteArrayInputStream(bytes,0,bytes.length-4));if(in.readInt()!=1)throw new IOException("Archive state schema");long sequence=in.readLong();var receipts=new HashMap<UUID,Long>();int n=count(in,1000000);for(int i=0;i<n;i++){var id=OperationCodec.uuid(in);long marker=in.readLong();if(marker<1||marker>sequence||receipts.putIfAbsent(id,marker)!=null)throw new IOException("Archive receipt");}
        var records=new HashMap<UUID,PieceRecord>();n=count(in,1000000);for(int i=0;i<n;i++){var decoded=BoxPieceCodec.decode(OperationCodec.blob(in,BoxPieceCodec.MAX_BYTES));if(!(decoded instanceof BoxPieceCodec.Decoded d)||records.putIfAbsent(d.piece().pieceId(),d.piece())!=null)throw new IOException("Archive piece");}
        var effects=new HashMap<String,WorldEffectStore.Value>();n=count(in,1000000);for(int i=0;i<n;i++){String key=in.readUTF();var owner=new CellPos(in.readLong(),in.readLong(),in.readLong());var value=new BinaryData(OperationCodec.blob(in,BinaryData.MAX_BYTES));if(effects.putIfAbsent(key,new WorldEffectStore.Value(owner,value))!=null)throw new IOException("Archive effect");}if(in.available()!=0)throw new IOException("Trailing archive state");try{return new State(sequence,records,effects,receipts);}catch(IllegalArgumentException ex){throw new IOException("Archive state invalid",ex);}
    }
    private static int count(DataInputStream in,int max)throws IOException{int n=in.readInt();if(n<0||n>max)throw new IOException("Archive collection budget");return n;}
    public static JournalArchive prepare(Path root,State state,JournalArchive previous,Map<UUID,Stamp> operations,Set<UUID> pieces)throws IOException{
        String generation=UUID.randomUUID().toString();var archive=new JournalArchive(root,generation,state);DurableDirectories.create(archive.directory);SectionStore.atomic(archive.directory.resolve("state.bin"),encode(state));
        var opRows=new TreeMap<UUID,byte[]>();for(var e:operations.entrySet())opRows.put(e.getKey(),stamp(e.getValue()));merge(previous,"operations.idx",STAMP,archive.directory.resolve("operations.idx"),opRows);
        var pieceRows=new TreeMap<UUID,byte[]>();for(var id:pieces)pieceRows.put(id,piece(id));merge(previous,"pieces.idx",PIECE,archive.directory.resolve("pieces.idx"),pieceRows);
        // The new active log must exist durably before publishing the generation pointer.
        SectionStore.atomic(archive.activeLog(),new byte[0]);return archive;
    }
    private static void merge(JournalArchive old,String file,int size,Path target,TreeMap<UUID,byte[]> additions)throws IOException{
        long count=old==null?0:old.verifyLedger(file,size);try(var previous=old==null?null:FileChannel.open(old.directory.resolve(file),StandardOpenOption.READ);var output=FileChannel.open(target,StandardOpenOption.CREATE_NEW,StandardOpenOption.READ,StandardOpenOption.WRITE)){
            write(output,ByteBuffer.allocate(HEADER).putInt(1).putLong(0).flip());long index=0,written=0;var iterator=additions.entrySet().iterator();var addition=iterator.hasNext()?iterator.next():null;
            while(index<count||addition!=null){byte[] oldRow=index<count?row(previous,index,size):null;UUID oldId=oldRow==null?null:new UUID(ByteBuffer.wrap(oldRow).getLong(),ByteBuffer.wrap(oldRow).getLong(8));byte[] next;
                if(oldId!=null&&(addition==null||oldId.compareTo(addition.getKey())<0)){next=oldRow;index++;}else if(oldId!=null&&oldId.equals(addition.getKey())){if(!Arrays.equals(oldRow,addition.getValue()))throw new IOException("Archive identity conflict");next=oldRow;index++;addition=iterator.hasNext()?iterator.next():null;}else{next=addition.getValue();addition=iterator.hasNext()?iterator.next():null;}
                write(output,ByteBuffer.wrap(next));written++;
            }output.position(0);write(output,ByteBuffer.allocate(HEADER).putInt(1).putLong(written).flip());output.force(true);
        }DurableDirectories.force(target.getParent());
    }
    private static void write(FileChannel channel,ByteBuffer bytes)throws IOException{while(bytes.hasRemaining())channel.write(bytes);}
    /** Retain the previous complete generation; remove only the older known manifest generation. */
    public void prune()throws IOException{
        if(obsolete!=null&&!obsolete.equals(generation)){
            Path old=root.resolve("generations").resolve(obsolete);if(Files.isDirectory(old)){
                try(var children=Files.list(old)){if(children.anyMatch(p->!Set.of("state.bin","operations.idx","pieces.idx","active.log").contains(p.getFileName().toString())))return;}
                for(String file:List.of("state.bin","operations.idx","pieces.idx","active.log"))Files.deleteIfExists(old.resolve(file));Files.deleteIfExists(old);
            }
        }
        if(hadPrevious)Files.deleteIfExists(root.resolve("operations.log"));DurableDirectories.force(root);
    }
    public void publish()throws IOException{
        if(!decode(Files.readAllBytes(bounded(directory.resolve("state.bin"),MAX_STATE))).equals(state))throw new IOException("Prepared archive state mismatch");verifyLedger("operations.idx",STAMP);verifyLedger("pieces.idx",PIECE);
        var manifest=new JsonObject();manifest.addProperty("schema",1);manifest.addProperty("generation",generation);for(String file:List.of("state.bin","operations.idx","pieces.idx"))manifest.addProperty(file,hash(directory.resolve(file)));
        Path previous=root.resolve("active.previous.json");if(Files.exists(previous))try{var json=JsonParser.parseString(Files.readString(bounded(previous,4096))).getAsJsonObject();if(json.get("schema").getAsInt()==1)obsolete=UUID.fromString(json.get("generation").getAsString()).toString();}catch(RuntimeException ex){obsolete=null;}
        Path pointer=root.resolve("active.json");hadPrevious=Files.exists(pointer);if(hadPrevious)SectionStore.atomic(root.resolve("active.previous.json"),Files.readAllBytes(bounded(pointer,4096)));
        SectionStore.atomic(pointer,manifest.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
