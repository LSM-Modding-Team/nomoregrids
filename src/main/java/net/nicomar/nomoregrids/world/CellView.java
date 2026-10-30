package net.nicomar.nomoregrids.world;

import java.io.*;
import java.util.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.persistence.*;

/** Read-only public replicas. No item payload, private material payload or authority lives in a cell. */
public record CellView(List<PieceRecord> replicas,Optional<net.nicomar.nomoregrids.simulation.fluid.CavityCell> cavity) {
    public CellView(List<PieceRecord> replicas){this(replicas,Optional.empty());}
    public boolean occupied(){return !replicas.isEmpty()||cavity.isPresent();}
    public static final int MAX_BYTES=2*1024*1024;
    public CellView{Objects.requireNonNull(cavity);replicas=replicas.stream().sorted(Comparator.comparing(PieceRecord::pieceId)).toList();if(replicas.size()>64)throw new IllegalArgumentException("Cell reference limit");
        Set<UUID> ids=new HashSet<>();for(var p:replicas)if(!ids.add(p.pieceId())||p.itemPayload().size()!=0||p.materialPayload().size()!=0)throw new IllegalArgumentException("Invalid public replica");}
    public static CellView from(CellPos cell,net.nicomar.nomoregrids.world.PieceIndex index){
        return from(cell,index,p->p.flags().contains(PieceRecord.Flag.SUBSTITUTE));
    }
    public static CellView from(CellPos cell,net.nicomar.nomoregrids.world.PieceIndex index,java.util.function.Predicate<PieceRecord> unavailable){
        var pieces=new ArrayList<PieceRecord>();
        for(UUID id:index.at(cell).stream().sorted().toList()){
            var p=index.records().get(id);
            var flags=new HashSet<>(p.flags());if(unavailable.test(p))flags.add(PieceRecord.Flag.SUBSTITUTE);
            boolean publicGeometry=Set.of("nomoregrids:box","nomoregrids:paintableblocks","nomoregrids:functional","nomoregrids:decoration").contains(p.adapterId().value());
            if(!publicGeometry&&!flags.contains(PieceRecord.Flag.SUBSTITUTE))throw new IllegalArgumentException("Adapter needs a public-data projection");
            pieces.add(new PieceRecord(p.pieceId(),p.revision(),p.adapterId(),p.adapterVersion(),p.sourceBlockId(),p.sourceState(),p.origin16(),
                p.orientation(),p.pivot16(),p.geometry(),publicGeometry?p.geometryPayload():BinaryData.EMPTY,BinaryData.EMPTY,BinaryData.EMPTY,p.groupId(),flags));
        }return new CellView(pieces);
    }
    public byte[] encode() throws IOException{
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeInt(2);out.writeInt(replicas.size());
        for(var p:replicas){byte[] record=BoxPieceCodec.encode(p);if(bytes.size()+record.length+4>MAX_BYTES)throw new IOException("Cell snapshot budget");OperationCodec.blob(out,record);}out.writeBoolean(cavity.isPresent());if(cavity.isPresent())OperationCodec.blob(out,CavityCodec.encode(cavity.orElseThrow()).copy());if(bytes.size()>MAX_BYTES)throw new IOException("Cell snapshot budget");return bytes.toByteArray();
    }
    public static CellView decode(byte[] bytes)throws IOException{
        if(bytes.length>MAX_BYTES)throw new IOException("Cell snapshot budget");var in=new DataInputStream(new ByteArrayInputStream(bytes));
        int schema=in.readInt();if(schema!=1&&schema!=2)throw new IOException("Future cell schema; retain original");int count=in.readInt();if(count<0||count>64)throw new IOException("Cell reference limit");
        var replicas=new ArrayList<PieceRecord>();for(int i=0;i<count;i++){
            var p=BoxPieceCodec.decode(OperationCodec.blob(in,BoxPieceCodec.MAX_BYTES));if(!(p instanceof BoxPieceCodec.Decoded d))throw new IOException("Future piece; retain original");replicas.add(d.piece());}
        Optional<net.nicomar.nomoregrids.simulation.fluid.CavityCell> cavity=Optional.empty();if(schema>=2){int marker=in.readUnsignedByte();if(marker>1)throw new IOException("Cavity marker");if(marker==1)cavity=Optional.of(CavityCodec.decode(new BinaryData(OperationCodec.blob(in,CavityCodec.MAX_BYTES))));}
        if(in.available()!=0)throw new IOException("Trailing cell bytes");try{return new CellView(replicas,cavity);}catch(IllegalArgumentException ex){throw new IOException("Invalid replica",ex);}
    }
}
