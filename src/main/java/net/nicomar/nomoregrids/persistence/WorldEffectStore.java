package net.nicomar.nomoregrids.persistence;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32C;
import net.nicomar.nomoregrids.core.*;

/** Private section projections of journaled effects. Unknown/corrupt data are never overwritten. */
public final class WorldEffectStore {
    public record Value(CellPos owner,BinaryData data){}
    private final Path root;
    public WorldEffectStore(Path root)throws IOException{this.root=root;DurableDirectories.create(root);}
    private Path file(SectionPos s){return root.resolve(s.x()+"_"+s.y()+"_"+s.z()+".effects");}
    public static byte[] encode(long checkpoint,Map<String,Value> values)throws IOException{
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeInt(1);out.writeLong(checkpoint);out.writeInt(values.size());
        for(var key:new TreeSet<>(values.keySet())){var v=values.get(key);out.writeUTF(key);out.writeLong(v.owner.x());out.writeLong(v.owner.y());out.writeLong(v.owner.z());OperationCodec.blob(out,v.data.copy());if(bytes.size()>SimulationCodec.MAX_BYTES-4)throw new IOException("Section effects budget");}
        var crc=new CRC32C();crc.update(bytes.toByteArray());out.writeInt((int)crc.getValue());return bytes.toByteArray();
    }
    public long checkpoint(SectionPos section)throws IOException{
        Path p=file(section);if(!Files.exists(p))return 0;if(Files.size(p)>SimulationCodec.MAX_BYTES)throw new IOException("Effects checkpoint budget");byte[] bytes=Files.readAllBytes(p);if(bytes.length<20)throw new IOException("Effects checkpoint length");
        var crc=new CRC32C();crc.update(bytes,0,bytes.length-4);if((int)crc.getValue()!=java.nio.ByteBuffer.wrap(bytes).getInt(bytes.length-4))throw new IOException("Effects checksum; original retained");
        var in=new DataInputStream(new ByteArrayInputStream(bytes,0,bytes.length-4));if(in.readInt()!=1)throw new IOException("Future effects schema; original retained");long checkpoint=in.readLong();if(checkpoint<0)throw new IOException("Effects sequence");int n=in.readInt();if(n<0||n>65536)throw new IOException("Effects count");var keys=new HashSet<String>();
        for(int i=0;i<n;i++){String key=in.readUTF();if(!keys.add(key))throw new IOException("Duplicate effects key");var owner=new CellPos(in.readLong(),in.readLong(),in.readLong());if(!owner.section().equals(section))throw new IOException("Effects owner");OperationCodec.blob(in,BinaryData.MAX_BYTES);}if(in.available()!=0)throw new IOException("Trailing effects data");return checkpoint;
    }
    public void write(SectionPos section,long sequence,Map<String,Value> values)throws IOException{
        long old=checkpoint(section);if(old>sequence)throw new IOException("Effects checkpoint rollback");SectionStore.atomic(file(section),encode(sequence,values));
    }
}
