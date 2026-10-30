package net.nicomar.nomoregrids.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.zip.CRC32C;
import net.nicomar.nomoregrids.core.BinaryData;
import net.nicomar.nomoregrids.core.BoxGeometrySnapshot;
import net.nicomar.nomoregrids.core.GridPoint;
import net.nicomar.nomoregrids.core.PieceRecord;
import net.nicomar.nomoregrids.core.ResourceId;
import net.nicomar.nomoregrids.core.TestPieces;
import net.nicomar.nomoregrids.geometry.ConstructionShapes;
import org.junit.jupiter.api.Test;

class BoxPieceCodecTest {
    private static final UUID PIECE_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private static PieceRecord example() {
        return new PieceRecord(PIECE_ID,7,new ResourceId("nomoregrids:box"),1,
            new ResourceId("minecraft:stone_stairs"),Map.of("facing","north","half","bottom"),
            new GridPoint(-1,8,-257),8,GridPoint.BLOCK_CENTER,
            new BoxGeometrySnapshot(ConstructionShapes.CUBE,ConstructionShapes.LOWER_SLAB,ConstructionShapes.STRAIGHT_STAIR),
            new BinaryData(new byte[] {1,2,3}),new BinaryData(new byte[] {4,5}),new BinaryData(new byte[] {6}),
            Optional.of(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")),
            Set.of(PieceRecord.Flag.CONNECTIONS_LOCKED,PieceRecord.Flag.SUBSTITUTE));
    }

    @Test
    void roundTripRetainsEveryFieldAndHasDeterministicBytes() throws IOException {
        PieceRecord piece = example();
        byte[] encoded = BoxPieceCodec.encode(piece);
        var decoded = assertInstanceOf(BoxPieceCodec.Decoded.class,BoxPieceCodec.decode(encoded));
        assertEquals(piece,decoded.piece());
        assertArrayEquals(encoded,BoxPieceCodec.encode(decoded.piece()));
        assertEquals(piece.ownerSection(),decoded.piece().ownerSection());
    }

    @Test
    void allOrientationsRetainIdentityAndExactOrigin() throws IOException {
        PieceRecord initial = TestPieces.of(PIECE_ID,ConstructionShapes.STRAIGHT_STAIR,new GridPoint(-1,-1,-1));
        for (int orientation = 0; orientation < 24; orientation++) {
            PieceRecord piece = initial.moved(initial.origin16(),orientation);
            assertEquals(piece,assertInstanceOf(BoxPieceCodec.Decoded.class,
                BoxPieceCodec.decode(BoxPieceCodec.encode(piece))).piece());
        }
    }

    @Test
    void unknownVersionIsPreservedVerbatimWithoutInterpretingBody() throws IOException {
        byte[] bytes = BoxPieceCodec.encode(example());
        ByteBuffer.wrap(bytes).putInt(4,2);
        updateChecksum(bytes);
        var preserved = assertInstanceOf(BoxPieceCodec.Preserved.class,BoxPieceCodec.decode(bytes));
        assertEquals(2,preserved.version());
        assertArrayEquals(bytes,preserved.original().copy());
        bytes[20] ^= 1;
        assertNotEquals(bytes[20],preserved.original().copy()[20]);
    }

    @Test
    void truncatedCorruptedAndOversizedFramesAreRejected() throws IOException {
        byte[] bytes = BoxPieceCodec.encode(example());
        for (int length = 0; length < bytes.length; length++) {
            byte[] truncated = Arrays.copyOf(bytes,length);
            assertThrows(IOException.class, () -> BoxPieceCodec.decode(truncated));
        }
        for (int index = 0; index < bytes.length; index++) {
            byte[] changed = bytes.clone();
            changed[index] ^= 1;
            assertThrows(IOException.class, () -> BoxPieceCodec.decode(changed));
        }
        assertThrows(IOException.class, () -> BoxPieceCodec.decode(new byte[BoxPieceCodec.MAX_BYTES+1]));
    }

    @Test
    void encodedFrameBudgetIncludesMetadataAndGeometry() {
        PieceRecord base = example();
        PieceRecord large = new PieceRecord(base.pieceId(),base.revision(),base.adapterId(),base.adapterVersion(),
            base.sourceBlockId(),base.sourceState(),base.origin16(),base.orientation(),base.pivot16(),base.geometry(),
            new BinaryData(new byte[BinaryData.MAX_BYTES]),BinaryData.EMPTY,BinaryData.EMPTY,base.groupId(),base.flags());
        assertThrows(IOException.class, () -> BoxPieceCodec.encode(large));
    }

    @Test
    void checksumDoesNotAuthorizeInvalidSchemaData() throws IOException {
        byte[] bytes = BoxPieceCodec.encode(example());
        // Body starts at byte 12, then UUID (16), then revision (8).
        ByteBuffer.wrap(bytes).putLong(28,-1);
        updateChecksum(bytes);
        assertThrows(IOException.class, () -> BoxPieceCodec.decode(bytes));
        byte[] enormousLength = BoxPieceCodec.encode(example());
        ByteBuffer.wrap(enormousLength).putInt(8,Integer.MAX_VALUE);
        updateChecksum(enormousLength);
        assertThrows(IOException.class, () -> BoxPieceCodec.decode(enormousLength));
    }

    private static void updateChecksum(byte[] bytes) {
        CRC32C crc = new CRC32C();
        crc.update(bytes,0,bytes.length-4);
        ByteBuffer.wrap(bytes).putInt(bytes.length-4,(int)crc.getValue());
    }
}
