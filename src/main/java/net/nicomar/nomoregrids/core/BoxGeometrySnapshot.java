package net.nicomar.nomoregrids.core;

import java.util.List;
import net.nicomar.nomoregrids.geometry.Box16;
import net.nicomar.nomoregrids.geometry.BoxUnion;

/** Integral box-family snapshot. Distinct physical/query shapes; not an external geometry codec. */
public record BoxGeometrySnapshot(BoxUnion occupation, BoxUnion collision, BoxUnion selection) {
    public BoxGeometrySnapshot {
        for (BoxUnion shape : List.of(occupation, collision, selection)) {
            Box16 b = shape.bounds();
            if (b.min().x() < 0 || b.min().y() < 0 || b.min().z() < 0
                || b.max().x() > 16 || b.max().y() > 16 || b.max().z() > 16) {
                throw new IllegalArgumentException("Atomic local geometry outside [0,16]");
            }
            // Occupation must conservatively reserve all physical/query geometry.
            // Per-box containment is sufficient for the supported canonical box families.
            if (shape != occupation) for (Box16 box : shape.boxes()) {
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
