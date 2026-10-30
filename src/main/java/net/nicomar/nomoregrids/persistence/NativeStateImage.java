package net.nicomar.nomoregrids.persistence;

import java.io.*;
import java.util.*;
import net.nicomar.nomoregrids.core.*;

/** Original native state of an under-demand conversion; does not encode a second authority. */
public record NativeStateImage(ResourceId block,Map<String,String> properties){
    private static final int MAGIC=0x4e4d474e;
    public NativeStateImage{Objects.requireNonNull(block);properties=Map.copyOf(properties);if(properties.size()>64)throw new IllegalArgumentException("Native state budget");}
    public BinaryData encode(){
        try{var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeInt(MAGIC);out.writeByte(1);out.writeUTF(block.value());out.writeByte(properties.size());for(var e:new TreeMap<>(properties).entrySet()){out.writeUTF(e.getKey());out.writeUTF(e.getValue());}out.flush();if(bytes.size()>16384)throw new IOException("Native state budget");return new BinaryData(bytes.toByteArray());}
        catch(IOException ex){throw new IllegalArgumentException("Native state budget",ex);}
    }
    public static NativeStateImage decode(BinaryData data)throws IOException{
        if(data.size()>16384)throw new IOException("Native state budget");var in=new DataInputStream(new ByteArrayInputStream(data.copy()));if(in.readInt()!=MAGIC||in.readUnsignedByte()!=1)throw new IOException("Unknown native state image");
        var block=new ResourceId(in.readUTF());int size=in.readUnsignedByte();if(size>64)throw new IOException("Native property budget");Map<String,String> properties=new HashMap<>();for(int i=0;i<size;i++)if(properties.putIfAbsent(in.readUTF(),in.readUTF())!=null)throw new IOException("Duplicate native property");if(in.available()!=0)throw new IOException("Trailing native image data");return new NativeStateImage(block,properties);
    }
}
