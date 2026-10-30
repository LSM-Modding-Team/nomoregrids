package net.nicomar.nomoregrids.persistence;

import java.io.*;
import net.nicomar.nomoregrids.core.*;

/** Durable proof that a native BE image has already transferred its inventory/XP into NMG. */
public record NativeCaptureStamp(ResourceId block,BinaryData digest) {
    public NativeCaptureStamp{if(digest.size()!=32)throw new IllegalArgumentException("Native capture digest");}
    public BinaryData encode(){try{var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeInt(1);out.writeUTF(block.value());out.write(digest.copy());return new BinaryData(bytes.toByteArray());}catch(IOException ex){throw new AssertionError(ex);}}
    public static NativeCaptureStamp decode(BinaryData data)throws IOException{var in=new DataInputStream(new ByteArrayInputStream(data.copy()));if(in.readInt()!=1)throw new IOException("Native capture schema");var id=new ResourceId(in.readUTF());var hash=new BinaryData(in.readNBytes(32));if(in.available()!=0)throw new IOException("Native capture trailing bytes");try{return new NativeCaptureStamp(id,hash);}catch(IllegalArgumentException ex){throw new IOException("Native capture digest",ex);}}
}
