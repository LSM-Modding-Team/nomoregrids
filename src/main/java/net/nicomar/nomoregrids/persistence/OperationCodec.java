package net.nicomar.nomoregrids.persistence;

import java.io.*;
import java.util.*;
import net.nicomar.nomoregrids.core.BinaryData;
import net.nicomar.nomoregrids.core.PieceRecord;
import net.nicomar.nomoregrids.operation.OperationPlan;

/** Bounded journal-only encoding. Never sent to clients (contains private resource data). */
public final class OperationCodec {
    public static final int MAX_BYTES = 16*1024*1024;
    private OperationCodec() {}
    public static byte[] encode(OperationPlan plan) throws IOException {
        var bytes = new ByteArrayOutputStream();
        var out = new DataOutputStream(bytes);
        out.writeInt(2); uuid(out,plan.id()); uuid(out,plan.actor()); out.writeUTF(plan.kind().name());
        out.writeInt(plan.changes().size());
        for (var c : plan.changes()) {
            uuid(out,c.id()); piece(out,c.before()); piece(out,c.after());
            if (bytes.size()>MAX_BYTES-2*BinaryData.MAX_BYTES-8) throw new IOException("Operation too large");
        }
        blob(out,plan.resourcesBefore().copy()); blob(out,plan.resourcesAfter().copy());
        out.writeInt(plan.effects().size());for(var e:plan.effects()){
            out.writeUTF(e.key());out.writeLong(e.owner().x());out.writeLong(e.owner().y());out.writeLong(e.owner().z());
            data(out,e.before());data(out,e.after());if(bytes.size()>MAX_BYTES)throw new IOException("Operation too large");
        }
        if (bytes.size()>MAX_BYTES) throw new IOException("Operation too large");
        return bytes.toByteArray();
    }
    public static OperationPlan decode(byte[] bytes) throws IOException {
        if (bytes.length>MAX_BYTES) throw new IOException("Operation too large");
        var in = new DataInputStream(new ByteArrayInputStream(bytes));
        try {
            int schema=in.readInt();if(schema!=1&&schema!=2) throw new IOException("Unsupported operation schema");
            UUID id=uuid(in), actor=uuid(in);
            String kind=in.readUTF();
            int count=in.readInt();
            if (count<0 || count>128) throw new IOException("Invalid operation count");
            var changes=new ArrayList<OperationPlan.Change>(count);
            for (int i=0;i<count;i++) changes.add(new OperationPlan.Change(uuid(in),piece(in),piece(in)));
            var before=new BinaryData(blob(in,BinaryData.MAX_BYTES));
            var after=new BinaryData(blob(in,BinaryData.MAX_BYTES));
            var effects=new ArrayList<OperationPlan.Effect>();if(schema>=2){int n=in.readInt();if(n<0||n>512)throw new IOException("Effect budget");for(int i=0;i<n;i++)effects.add(new OperationPlan.Effect(in.readUTF(),new net.nicomar.nomoregrids.core.CellPos(in.readLong(),in.readLong(),in.readLong()),data(in),data(in)));}
            if (in.available()!=0) throw new IOException("Trailing operation bytes");
            return new OperationPlan(id,actor,OperationPlan.Kind.valueOf(kind),changes,before,after,effects);
        } catch (IllegalArgumentException ex) { throw new IOException("Invalid operation",ex); }
    }
    private static void data(DataOutput out,Optional<BinaryData> data)throws IOException{out.writeBoolean(data.isPresent());if(data.isPresent())blob(out,data.orElseThrow().copy());}
    private static Optional<BinaryData> data(DataInputStream in)throws IOException{int marker=in.readUnsignedByte();if(marker>1)throw new IOException("Effect marker");return marker==0?Optional.empty():Optional.of(new BinaryData(blob(in,BinaryData.MAX_BYTES)));}
    public static void uuid(DataOutput out,UUID id) throws IOException { out.writeLong(id.getMostSignificantBits());out.writeLong(id.getLeastSignificantBits()); }
    public static UUID uuid(DataInput in) throws IOException { return new UUID(in.readLong(),in.readLong()); }
    public static void blob(DataOutput out,byte[] bytes) throws IOException { out.writeInt(bytes.length);out.write(bytes); }
    public static byte[] blob(DataInputStream in,int limit) throws IOException {
        int length=in.readInt();
        if (length<0 || length>limit || length>in.available()) throw new IOException("Invalid blob length");
        return in.readNBytes(length);
    }
    private static void piece(DataOutput out,Optional<PieceRecord> piece) throws IOException {
        out.writeBoolean(piece.isPresent());if (piece.isPresent()) blob(out,BoxPieceCodec.encode(piece.orElseThrow()));
    }
    private static Optional<PieceRecord> piece(DataInputStream in) throws IOException {
        int marker=in.readUnsignedByte();if(marker>1)throw new IOException("Invalid optional marker");
        if(marker==0)return Optional.empty();
        var result=BoxPieceCodec.decode(blob(in,BoxPieceCodec.MAX_BYTES));
        if (!(result instanceof BoxPieceCodec.Decoded decoded)) throw new IOException("Future piece schema; preserve journal");
        return Optional.of(decoded.piece());
    }
}
