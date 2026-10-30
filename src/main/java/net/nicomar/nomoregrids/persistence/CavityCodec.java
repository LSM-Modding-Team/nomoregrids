package net.nicomar.nomoregrids.persistence;

import java.io.*;
import java.util.*;
import net.nicomar.nomoregrids.core.BinaryData;
import net.nicomar.nomoregrids.simulation.fluid.CavityCell;

/** Fixed-size public cavity image. Solid precision remains in the piece adapters. */
public final class CavityCodec {
    private CavityCodec(){}
    public static BinaryData encode(CavityCell cell){
        try{var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeByte(1);out.writeByte(CavityCell.code(cell.source()));long[] mask=cell.blocked().toLongArray();for(int i=0;i<64;i++)out.writeLong(i<mask.length?mask[i]:0);byte[] fluid=cell.fluids();for(int i=0;i<4096;i+=4)out.writeByte(fluid[i]|fluid[i+1]<<2|fluid[i+2]<<4|fluid[i+3]<<6);return new BinaryData(bytes.toByteArray());}catch(IOException ex){throw new UncheckedIOException(ex);}
    }
    public static CavityCell decode(BinaryData data)throws IOException{
        if(data.size()!=1538)throw new IOException("Cavity image length");var in=new DataInputStream(new ByteArrayInputStream(data.copy()));if(in.readUnsignedByte()!=1)throw new IOException("Cavity schema");
        try{var source=CavityCell.kind(in.readByte());long[] mask=new long[64];for(int i=0;i<64;i++)mask[i]=in.readLong();byte[] fluids=new byte[4096];for(int i=0;i<4096;i+=4){int b=in.readUnsignedByte();for(int j=0;j<4;j++)fluids[i+j]=(byte)((b>>>(j*2))&3);}return new CavityCell(BitSet.valueOf(mask),fluids,source);}catch(IllegalArgumentException ex){throw new IOException("Invalid cavity image",ex);}
    }
}
