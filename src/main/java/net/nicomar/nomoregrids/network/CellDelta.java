package net.nicomar.nomoregrids.network;

import java.io.*;
import java.util.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.persistence.*;
import net.nicomar.nomoregrids.world.CellView;

/** Revision-checked public projection; private material and inventory data never enter this codec. */
public final class CellDelta {
    private CellDelta(){}
    public static byte[] encode(CellView before,CellView after)throws IOException{
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
        out.writeInt(1);out.write(SnapshotAssembler.sha256(before.encode()));out.write(SnapshotAssembler.sha256(after.encode()));
        var previous=new HashMap<UUID,PieceRecord>();before.replicas().forEach(p->previous.put(p.pieceId(),p));
        var current=new HashMap<UUID,PieceRecord>();after.replicas().forEach(p->current.put(p.pieceId(),p));
        var removed=previous.keySet().stream().filter(id->!current.containsKey(id)).sorted().toList();out.writeInt(removed.size());
        for(var id:removed)OperationCodec.uuid(out,id);
        var changed=after.replicas().stream().filter(p->!p.equals(previous.get(p.pieceId()))).toList();out.writeInt(changed.size());
        for(var p:changed)OperationCodec.blob(out,BoxPieceCodec.encode(p));

        if(bytes.size()>SnapshotAssembler.MAX)throw new IOException("Public delta budget");return bytes.toByteArray();
    }
    public static CellView apply(CellView before,byte[] bytes)throws IOException{
        if(bytes.length>SnapshotAssembler.MAX)throw new IOException("Public delta budget");
        var in=new DataInputStream(new ByteArrayInputStream(bytes));if(in.readInt()!=1)throw new IOException("Delta schema");
        if(!Arrays.equals(in.readNBytes(32),SnapshotAssembler.sha256(before.encode())))throw new IOException("Delta base mismatch; resync required");
        byte[] expected=in.readNBytes(32);var records=new TreeMap<UUID,PieceRecord>();before.replicas().forEach(p->records.put(p.pieceId(),p));
        int removed=count(in);var seen=new HashSet<UUID>();for(int i=0;i<removed;i++){var id=OperationCodec.uuid(in);if(!seen.add(id)||records.remove(id)==null)throw new IOException("Invalid delta removal");}
        int changed=count(in);for(int i=0;i<changed;i++){
            var decoded=BoxPieceCodec.decode(OperationCodec.blob(in,BoxPieceCodec.MAX_BYTES));
            if(!(decoded instanceof BoxPieceCodec.Decoded d)||!seen.add(d.piece().pieceId()))throw new IOException("Invalid delta record");
            var old=records.get(d.piece().pieceId());if(old!=null&&d.piece().revision()<=old.revision())throw new IOException("Stale delta revision");records.put(d.piece().pieceId(),d.piece());
        }
        if(in.available()!=0)throw new IOException("Trailing delta data");
        try{var next=new CellView(List.copyOf(records.values()));if(!Arrays.equals(expected,SnapshotAssembler.sha256(next.encode())))throw new IOException("Delta target mismatch");return next;}
        catch(IllegalArgumentException ex){throw new IOException("Invalid public delta",ex);}
    }
    private static int count(DataInputStream in)throws IOException{int n=in.readInt();if(n<0||n>64)throw new IOException("Delta reference budget");return n;}
}
