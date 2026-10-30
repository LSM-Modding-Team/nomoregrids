package net.nicomar.nomoregrids.persistence;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32C;
import net.nicomar.nomoregrids.core.*;

/** Authoritative records by owner section. Read replicas remain in cell block entities. */
public final class SectionStore {
    private static final int MAX=2*1024*1024;
    private final Path root;
    public record Section(long checkpoint,List<PieceRecord> pieces){public Section{pieces=List.copyOf(pieces);}}
    public SectionStore(Path root)throws IOException{this.root=root;DurableDirectories.create(root);}
    private Path path(SectionPos section){return root.resolve(section.x()+"_"+section.y()+"_"+section.z()+".nmg");}
    public Section read(SectionPos pos)throws IOException{
        Path file=path(pos);if(!Files.exists(file))return new Section(0,List.of());
        long size=Files.size(file);if(size<20||size>MAX)throw new IOException("Section size; original retained");byte[] bytes=Files.readAllBytes(file);
        CRC32C crc=new CRC32C();crc.update(bytes,0,bytes.length-4);if((int)crc.getValue()!=ByteBuffer.wrap(bytes).getInt(bytes.length-4))throw new IOException("Section checksum; original retained");
        var in=new DataInputStream(new ByteArrayInputStream(bytes,0,bytes.length-4));
        if(in.readInt()!=1)throw new IOException("Future section schema; original retained");long checkpoint=in.readLong();int count=in.readInt();
        if(checkpoint<0||count<0||count>4096)throw new IOException("Section record budget/checkpoint");var pieces=new ArrayList<PieceRecord>();Set<UUID> ids=new HashSet<>();
        for(int i=0;i<count;i++){
            var result=BoxPieceCodec.decode(OperationCodec.blob(in,BoxPieceCodec.MAX_BYTES));if(!(result instanceof BoxPieceCodec.Decoded d))throw new IOException("Future piece; original retained");
            var p=d.piece();if(!p.ownerSection().equals(pos)||!ids.add(p.pieceId()))throw new IOException("Invalid section authority");pieces.add(p);
        }
        if(in.available()!=0)throw new IOException("Trailing section bytes");return new Section(checkpoint,pieces);
    }
    public void write(SectionPos pos,long checkpoint,Collection<PieceRecord> pieces)throws IOException{
        atomic(path(pos),encode(pos,checkpoint,pieces));
    }
    public static byte[] encode(SectionPos pos,long checkpoint,Collection<PieceRecord> pieces)throws IOException{
        if(checkpoint<0||pieces.size()>4096)throw new IOException("Section budget");
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeInt(1);out.writeLong(checkpoint);out.writeInt(pieces.size());Set<UUID> ids=new HashSet<>();
        for(var p:pieces.stream().sorted(Comparator.comparing(PieceRecord::pieceId)).toList()){
            if(!p.ownerSection().equals(pos)||!ids.add(p.pieceId()))throw new IOException("Wrong/duplicate owner section authority");byte[] record=BoxPieceCodec.encode(p);
            if(bytes.size()+record.length+8>MAX)throw new IOException("Section byte budget");OperationCodec.blob(out,record);
        }
        CRC32C crc=new CRC32C();crc.update(bytes.toByteArray());out.writeInt((int)crc.getValue());return bytes.toByteArray();
    }
    /** Atomic replacement requires actual filesystem support; never falls back to deleting old data. */
    public static void atomic(Path target,byte[] data)throws IOException{
        Path parent=target.toAbsolutePath().getParent();DurableDirectories.create(parent);Path tmp=Files.createTempFile(parent,"nmg-",".tmp");
        try{
            try(var channel=FileChannel.open(tmp,StandardOpenOption.WRITE)){ByteBuffer buffer=ByteBuffer.wrap(data);while(buffer.hasRemaining())channel.write(buffer);channel.force(true);}
            Files.move(tmp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            try(var directory=FileChannel.open(parent,StandardOpenOption.READ)){directory.force(true);}
        }finally{Files.deleteIfExists(tmp);}
    }
}
