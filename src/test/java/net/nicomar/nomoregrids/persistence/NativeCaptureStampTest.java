package net.nicomar.nomoregrids.persistence;

import java.io.*;
import java.util.*;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.ConstructionShapes;
import net.nicomar.nomoregrids.operation.OperationPlan;

class NativeCaptureStampTest {
    @Test void nativeReceiptAndCanonicalObjectOrderRoundTrip()throws Exception{var stamp=new NativeCaptureStamp(new ResourceId("minecraft:barrel"),new BinaryData(new byte[32]));assertEquals(stamp,NativeCaptureStamp.decode(stamp.encode()));assertArrayEquals(CanonicalJson.bytes(JsonParser.parseString("{\"b\":2,\"a\":{\"d\":4,\"c\":3}}")),CanonicalJson.bytes(JsonParser.parseString("{\"a\":{\"c\":3,\"d\":4},\"b\":2}")));var raw=stamp.encode().copy();assertThrows(IOException.class,()->NativeCaptureStamp.decode(new BinaryData(Arrays.copyOf(raw,raw.length-1))));}
    @Test void operationPersistsTheOriginalCellOfAnAlreadyMovedNativeCapture()throws Exception{var piece=TestPieces.of(UUID.randomUUID(),ConstructionShapes.CUBE,GridPoint.ZERO);var source=new CellPos(5,0,0);var plan=new OperationPlan(UUID.randomUUID(),OperationPlan.WORLD_ACTOR,OperationPlan.Kind.SYSTEM,List.of(new OperationPlan.Change(piece.pieceId(),Optional.empty(),Optional.of(piece),Optional.of(source))),BinaryData.EMPTY,BinaryData.EMPTY);assertEquals(plan,OperationCodec.decode(OperationCodec.encode(plan)));assertTrue(plan.pieceCells().contains(source));}
    @Test void oldOperationSchemaStillHasNoNativeSourceField()throws Exception{var piece=TestPieces.of(UUID.randomUUID(),ConstructionShapes.CUBE,GridPoint.ZERO);var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeInt(3);OperationCodec.uuid(out,UUID.randomUUID());OperationCodec.uuid(out,OperationPlan.WORLD_ACTOR);out.writeUTF("SYSTEM");out.writeInt(1);OperationCodec.uuid(out,piece.pieceId());out.writeBoolean(false);out.writeBoolean(true);OperationCodec.blob(out,BoxPieceCodec.encode(piece));OperationCodec.blob(out,new byte[0]);OperationCodec.blob(out,new byte[0]);out.writeInt(0);var decoded=OperationCodec.decode(bytes.toByteArray());assertEquals(Optional.empty(),decoded.changes().getFirst().nativeSource());assertEquals(piece,decoded.changes().getFirst().after().orElseThrow());}
}
