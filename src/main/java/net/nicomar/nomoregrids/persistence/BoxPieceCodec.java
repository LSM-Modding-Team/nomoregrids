package net.nicomar.nomoregrids.persistence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.CRC32C;
import net.nicomar.nomoregrids.core.BinaryData;
import net.nicomar.nomoregrids.core.BoxGeometrySnapshot;
import net.nicomar.nomoregrids.core.GridPoint;
import net.nicomar.nomoregrids.core.PieceRecord;
import net.nicomar.nomoregrids.core.ResourceId;
import net.nicomar.nomoregrids.geometry.Box16;
import net.nicomar.nomoregrids.geometry.BoxUnion;

/**
 * Bounded experimental codec for integral box-family records. No world/file access.
 * Not an external adapter geometry codec, network payload, or durable journal.
 */
public final class BoxPieceCodec {
    private static final int MAGIC = 0x4e4d4742; // NMGB
    private static final int VERSION = 1;
    private static final int FRAME_OVERHEAD = 16;
    public static final int MAX_BYTES = BinaryData.MAX_BYTES;

    public sealed interface ReadResult permits Decoded, Preserved {}
    public record Decoded(PieceRecord piece) implements ReadResult {}
    /** Unknown positive versions are retained verbatim; no downgrade encoder exists. */
    public record Preserved(int version, BinaryData original) implements ReadResult {}

    private BoxPieceCodec() {}

    public static byte[] encode(PieceRecord piece) throws IOException {
        BudgetOutput budget = new BudgetOutput(MAX_BYTES - FRAME_OVERHEAD);
        DataOutputStream out = new DataOutputStream(budget);
        writeUuid(out, piece.pieceId());
        out.writeLong(piece.revision());
        writeString(out, piece.adapterId().value());
        out.writeInt(piece.adapterVersion());
        writeString(out, piece.sourceBlockId().value());
        out.writeShort(piece.sourceState().size());
        for (var property : new TreeMap<>(piece.sourceState()).entrySet()) {
            writeString(out, property.getKey());
            writeString(out, property.getValue());
        }
        writePoint(out, piece.origin16());
        out.writeByte(piece.orientation());
        writePoint(out, piece.pivot16());
        out.writeBoolean(piece.groupId().isPresent());
        if (piece.groupId().isPresent()) writeUuid(out, piece.groupId().orElseThrow());
        int flags = (piece.flags().contains(PieceRecord.Flag.CONNECTIONS_LOCKED) ? 1 : 0)
            | (piece.flags().contains(PieceRecord.Flag.SUBSTITUTE) ? 2 : 0);
        out.writeByte(flags);
        writeShape(out, piece.geometry().occupation());
        writeShape(out, piece.geometry().collision());
        writeShape(out, piece.geometry().selection());
        writeData(out, piece.geometryPayload());
        writeData(out, piece.materialPayload());
        writeData(out, piece.itemPayload());
        out.flush();
        byte[] body = budget.bytes();
        ByteBuffer frame = ByteBuffer.allocate(body.length + FRAME_OVERHEAD);
        frame.putInt(MAGIC).putInt(VERSION).putInt(body.length).put(body);
        CRC32C crc = new CRC32C();
        crc.update(frame.array(), 0, frame.position());
        frame.putInt((int) crc.getValue());
        return frame.array();
    }

    public static ReadResult decode(byte[] input) throws IOException {
        if (input.length < FRAME_OVERHEAD || input.length > MAX_BYTES) throw new IOException("Invalid piece frame size");
        byte[] bytes = input.clone();
        ByteBuffer frame = ByteBuffer.wrap(bytes);
        if (frame.getInt() != MAGIC) throw new IOException("Not an NMGB frame");
        int version = frame.getInt(), length = frame.getInt();
        if (version < 1 || length != bytes.length - FRAME_OVERHEAD) throw new IOException("Invalid piece frame header");
        CRC32C crc = new CRC32C();
        crc.update(bytes, 0, bytes.length - Integer.BYTES);
        if ((int) crc.getValue() != frame.getInt(bytes.length - Integer.BYTES)) throw new IOException("Piece checksum mismatch");
        if (version != VERSION) return new Preserved(version, new BinaryData(bytes));

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes, 12, length));
        try {
            UUID id = readUuid(in);
            long revision = in.readLong();
            ResourceId adapter = new ResourceId(readString(in,256));
            int adapterVersion = in.readInt();
            ResourceId source = new ResourceId(readString(in,256));
            int properties = in.readUnsignedShort();
            if (properties > 64) throw new IOException("Too many source properties");
            var state = new HashMap<String,String>();
            for (int i = 0; i < properties; i++) {
                String key = readString(in,128), value = readString(in,128);
                if (state.putIfAbsent(key,value) != null) throw new IOException("Duplicate source property");
            }
            GridPoint origin = readPoint(in);
            int orientation = in.readUnsignedByte();
            GridPoint pivot = readPoint(in);
            int hasGroup = in.readUnsignedByte();
            if (hasGroup > 1) throw new IOException("Invalid group marker");
            Optional<UUID> group = hasGroup == 1 ? Optional.of(readUuid(in)) : Optional.empty();
            int flagBits = in.readUnsignedByte();
            if ((flagBits & ~3) != 0) throw new IOException("Unknown flags in box schema 1");
            var flags = new HashSet<PieceRecord.Flag>();
            if ((flagBits & 1) != 0) flags.add(PieceRecord.Flag.CONNECTIONS_LOCKED);
            if ((flagBits & 2) != 0) flags.add(PieceRecord.Flag.SUBSTITUTE);
            var geometry = new BoxGeometrySnapshot(readShape(in), readShape(in), readShape(in));
            BinaryData geometryData = readData(in), materialData = readData(in), itemData = readData(in);
            if (in.available() != 0) throw new IOException("Trailing piece data");
            return new Decoded(new PieceRecord(id, revision, adapter, adapterVersion, source, state,
                origin, orientation, pivot, geometry, geometryData, materialData, itemData, group, flags));
        } catch (IllegalArgumentException | ArithmeticException exc) {
            throw new IOException("Invalid box piece data", exc);
        }
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] ascii = value.getBytes(StandardCharsets.US_ASCII);
        out.writeShort(ascii.length);
        out.write(ascii);
    }

    private static String readString(DataInputStream in, int limit) throws IOException {
        int count = in.readUnsignedShort();
        if (count < 1 || count > limit || count > in.available()) throw new IOException("Invalid string length");
        byte[] ascii = in.readNBytes(count);
        for (byte b : ascii) if (b < 0) throw new IOException("Non-ASCII registry/property data");
        return new String(ascii, StandardCharsets.US_ASCII);
    }

    private static void writeUuid(DataOutputStream out, UUID id) throws IOException {
        out.writeLong(id.getMostSignificantBits()); out.writeLong(id.getLeastSignificantBits());
    }

    private static UUID readUuid(DataInputStream in) throws IOException { return new UUID(in.readLong(),in.readLong()); }

    private static void writePoint(DataOutputStream out, GridPoint point) throws IOException {
        out.writeLong(point.x()); out.writeLong(point.y()); out.writeLong(point.z());
    }

    private static GridPoint readPoint(DataInputStream in) throws IOException {
        return new GridPoint(in.readLong(),in.readLong(),in.readLong());
    }

    private static void writeShape(DataOutputStream out, BoxUnion shape) throws IOException {
        out.writeInt(shape.boxes().size());
        for (Box16 box : shape.boxes()) { writePoint(out,box.min()); writePoint(out,box.max()); }
    }

    private static BoxUnion readShape(DataInputStream in) throws IOException {
        int count = in.readInt();
        if (count < 1 || count > BoxUnion.MAX_BOXES || count > in.available()/48) throw new IOException("Invalid box count");
        var boxes = new ArrayList<Box16>(count);
        for (int i = 0; i < count; i++) boxes.add(new Box16(readPoint(in),readPoint(in)));
        return new BoxUnion(boxes);
    }

    private static void writeData(DataOutputStream out, BinaryData data) throws IOException {
        out.writeInt(data.size()); out.write(data.copy());
    }

    private static BinaryData readData(DataInputStream in) throws IOException {
        int count = in.readInt();
        if (count < 0 || count > BinaryData.MAX_BYTES || count > in.available()) throw new IOException("Invalid adapter payload length");
        return new BinaryData(in.readNBytes(count));
    }

    private static final class BudgetOutput extends OutputStream {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final int limit;
        BudgetOutput(int limit) { this.limit = limit; }
        private void reserve(int count) throws IOException {
            if (count < 0 || count > limit-buffer.size()) throw new IOException("Piece frame budget exceeded");
        }
        @Override
        public void write(int value) throws IOException { reserve(1); buffer.write(value); }
        @Override
        public void write(byte[] data, int offset, int count) throws IOException {
            reserve(count); buffer.write(data,offset,count);
        }
        byte[] bytes() { return buffer.toByteArray(); }
    }
}
