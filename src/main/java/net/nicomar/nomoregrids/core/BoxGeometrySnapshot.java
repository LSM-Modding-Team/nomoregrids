package net.nicomar.nomoregrids.core;

import java.util.List;
import net.nicomar.nomoregrids.geometry.Box16;
import net.nicomar.nomoregrids.geometry.BoxUnion;

/** Integral box-family snapshot. Distinct physical/query shapes; not an external geometry codec. */
public record BoxGeometrySnapshot(BoxUnion occupation, BoxUnion collision, BoxUnion selection) {
    public BoxGeometrySnapshot {
        if(occupation.boxes().isEmpty()||selection.boxes().isEmpty())throw new IllegalArgumentException("Missing occupation/selection");
        var shapes=List.of(occupation, collision, selection);
        for (int index=0;index<shapes.size();index++) {
            BoxUnion shape=shapes.get(index);
            if(shape.boxes().isEmpty())continue;
            Box16 b = shape.bounds();
            long lower=index==1?-16:0,upper=index==1?32:16;
            if (b.min().x() < lower || b.min().y() < lower || b.min().z() < lower
                || b.max().x() > upper || b.max().y() > upper || b.max().z() > upper) {
                throw new IllegalArgumentException("Atomic local geometry outside [0,16]");
            }
            // Occupation must conservatively reserve all physical/query geometry.
            // Per-box containment is sufficient for the supported canonical box families.
            if (index==2) for (Box16 box : shape.boxes()) {
                if (occupation.boxes().stream().noneMatch(o -> contains(o, box))) {
                    throw new IllegalArgumentException("Query box outside reserved occupation");
                }
            }
        }
    }

    private static boolean contains(Box16 outer, Box16 inner) {
        return outer.min().x() <= inner.min().x() && outer.min().y() <= inner.min().y() && outer.min().z() <= inner.min().z()
            && outer.max().x() >= inner.max().x() && outer.max().y() >= inner.max().y() && outer.max().z() >= inner.max().z();
    }
}
