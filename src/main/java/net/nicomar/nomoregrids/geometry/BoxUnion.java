package net.nicomar.nomoregrids.geometry;

import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import net.nicomar.nomoregrids.core.CellPos;
import net.nicomar.nomoregrids.core.GridPoint;

/** Immutable union of boxes, not a conversion target for arbitrary external shapes. */
public record BoxUnion(List<Box16> boxes) {
    public static final int MAX_BOXES = 16384;

    public BoxUnion {
        if (boxes.size() > MAX_BOXES) throw new IllegalArgumentException("Invalid box count");
        boxes = List.copyOf(boxes);
    }

    public Box16 bounds() {
        if(boxes.isEmpty())throw new IllegalStateException("Empty query shape has no bounds");
        long x0 = Long.MAX_VALUE, y0 = Long.MAX_VALUE, z0 = Long.MAX_VALUE;
        long x1 = Long.MIN_VALUE, y1 = Long.MIN_VALUE, z1 = Long.MIN_VALUE;
        for (Box16 b : boxes) {
            x0 = Math.min(x0,b.min().x()); y0 = Math.min(y0,b.min().y()); z0 = Math.min(z0,b.min().z());
            x1 = Math.max(x1,b.max().x()); y1 = Math.max(y1,b.max().y()); z1 = Math.max(z1,b.max().z());
        }
        return Box16.of(x0,y0,z0,x1,y1,z1);
    }

    public boolean overlaps(BoxUnion other) {
        if(boxes.isEmpty()||other.boxes.isEmpty())return false;
        if (!bounds().overlaps(other.bounds())) return false;
        for (Box16 a : boxes) for (Box16 b : other.boxes) if (a.overlaps(b)) return true;
        return false;
    }

    public BoxUnion transform(Rotation rotation, GridPoint pivot, GridPoint origin16) {
        return new BoxUnion(boxes.stream().map(b -> b.transform(rotation, pivot, origin16)).toList());
    }

    public NavigableSet<CellPos> cells(int limit) {
        TreeSet<CellPos> result = new TreeSet<>();
        for (Box16 box : boxes) {
            result.addAll(box.cells(limit));
            if (result.size() > limit) throw new IllegalArgumentException("Cell budget exceeded");
        }
        return Collections.unmodifiableNavigableSet(result);
    }
}
