package net.nicomar.nomoregrids.core;

/** World cell coordinate. Natural order is the documented X, Y, Z lock order. */
public record CellPos(long x, long y, long z) implements Comparable<CellPos> {
    public SectionPos section() {
        return new SectionPos(Math.floorDiv(x, 16), Math.floorDiv(y, 16), Math.floorDiv(z, 16));
    }

    @Override
    public int compareTo(CellPos other) {
        int c = Long.compare(x, other.x);
        if (c == 0) c = Long.compare(y, other.y);
        return c == 0 ? Long.compare(z, other.z) : c;
    }
}
