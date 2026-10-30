package net.nicomar.nomoregrids.core;

/** Exact coordinates in units of 1/16 block. Arithmetic never wraps silently. */
public record GridPoint(long x, long y, long z) {
    public static final GridPoint ZERO = new GridPoint(0, 0, 0);
    public static final GridPoint BLOCK_CENTER = new GridPoint(8, 8, 8);

    public GridPoint add(GridPoint other) {
        return new GridPoint(Math.addExact(x, other.x), Math.addExact(y, other.y), Math.addExact(z, other.z));
    }

    public GridPoint subtract(GridPoint other) {
        return new GridPoint(Math.subtractExact(x, other.x), Math.subtractExact(y, other.y), Math.subtractExact(z, other.z));
    }

    public CellPos cell() {
        return new CellPos(Math.floorDiv(x, 16), Math.floorDiv(y, 16), Math.floorDiv(z, 16));
    }

    public GridPoint offsetInCell() {
        return new GridPoint(Math.floorMod(x, 16), Math.floorMod(y, 16), Math.floorMod(z, 16));
    }
}
