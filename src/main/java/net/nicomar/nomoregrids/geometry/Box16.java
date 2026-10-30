package net.nicomar.nomoregrids.geometry;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.TreeSet;
import net.nicomar.nomoregrids.core.CellPos;
import net.nicomar.nomoregrids.core.GridPoint;

/** Nonempty half-open box in exact 1/16 units. Only for integral box geometry. */
public record Box16(GridPoint min, GridPoint max) {
    public Box16 {
        if (min.x() >= max.x() || min.y() >= max.y() || min.z() >= max.z()) {
            throw new IllegalArgumentException("Box must have positive extent on every axis");
        }
    }

    public static Box16 of(long x0, long y0, long z0, long x1, long y1, long z1) {
        return new Box16(new GridPoint(x0, y0, z0), new GridPoint(x1, y1, z1));
    }

    public boolean overlaps(Box16 other) {
        return min.x() < other.max.x() && max.x() > other.min.x()
            && min.y() < other.max.y() && max.y() > other.min.y()
            && min.z() < other.max.z() && max.z() > other.min.z();
    }

    public Box16 transform(Rotation rotation, GridPoint pivot, GridPoint origin16) {
        long x0 = Long.MAX_VALUE, y0 = Long.MAX_VALUE, z0 = Long.MAX_VALUE;
        long x1 = Long.MIN_VALUE, y1 = Long.MIN_VALUE, z1 = Long.MIN_VALUE;
        for (long x : new long[] {min.x(), max.x()}) {
            for (long y : new long[] {min.y(), max.y()}) {
                for (long z : new long[] {min.z(), max.z()}) {
                    GridPoint p = rotation.transform(new GridPoint(x,y,z), pivot, origin16);
                    x0 = Math.min(x0,p.x()); y0 = Math.min(y0,p.y()); z0 = Math.min(z0,p.z());
                    x1 = Math.max(x1,p.x()); y1 = Math.max(y1,p.y()); z1 = Math.max(z1,p.z());
                }
            }
        }
        return of(x0,y0,z0,x1,y1,z1);
    }

    /** Checks allocation budget before enumerating, including malformed huge boxes. */
    public NavigableSet<CellPos> cells(int limit) {
        if (limit < 1) throw new IllegalArgumentException("Cell budget must be positive");
        CellPos first = min.cell();
        CellPos last = new GridPoint(max.x()-1, max.y()-1, max.z()-1).cell();
        long nx = Math.addExact(Math.subtractExact(last.x(), first.x()), 1);
        long ny = Math.addExact(Math.subtractExact(last.y(), first.y()), 1);
        long nz = Math.addExact(Math.subtractExact(last.z(), first.z()), 1);
        if (nx > limit || ny > limit / nx || nz > limit / (nx * ny)) {
            throw new IllegalArgumentException("Cell budget exceeded");
        }
        TreeSet<CellPos> result = new TreeSet<>();
        for (long x = first.x(); x <= last.x(); x++) {
            for (long y = first.y(); y <= last.y(); y++) {
                for (long z = first.z(); z <= last.z(); z++) result.add(new CellPos(x,y,z));
            }
        }
        return Collections.unmodifiableNavigableSet(result);
    }
}
