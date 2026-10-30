package net.nicomar.nomoregrids.core;

import java.util.Arrays;
import java.util.Objects;

/** Bounded, defensively copied opaque adapter data with value equality. */
public final class BinaryData {
    public static final int MAX_BYTES = 256 * 1024;
    public static final BinaryData EMPTY = new BinaryData(new byte[0]);
    private final byte[] bytes;
    private final int hash;

    public BinaryData(byte[] bytes) {
        Objects.requireNonNull(bytes);
        if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("Adapter payload too large");
        this.bytes = bytes.clone();this.hash=Arrays.hashCode(this.bytes);
    }

    public int size() { return bytes.length; }
    public byte[] copy() { return bytes.clone(); }

    @Override
    public boolean equals(Object other) { return other instanceof BinaryData data && Arrays.equals(bytes, data.bytes); }

    @Override
    public int hashCode() { return hash; }
}
