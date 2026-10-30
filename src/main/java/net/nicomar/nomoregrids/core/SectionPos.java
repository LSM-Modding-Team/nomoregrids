package net.nicomar.nomoregrids.core;

/** A 16 x 16 x 16 cell section; not a chunk column. */
public record SectionPos(long x, long y, long z) implements Comparable<SectionPos> {
    @Override
    public int compareTo(SectionPos other) {
        int c = Long.compare(x, other.x);
        if (c == 0) c = Long.compare(y, other.y);
        return c == 0 ? Long.compare(z, other.z) : c;
    }
}
