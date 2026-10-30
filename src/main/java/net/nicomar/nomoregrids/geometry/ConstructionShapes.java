package net.nicomar.nomoregrids.geometry;

import java.util.List;
import net.nicomar.nomoregrids.core.GridPoint;

/** Canonical local geometry only. Does not register or enable any block family. */
public final class ConstructionShapes {
    public static final BoxUnion CUBE = new BoxUnion(List.of(Box16.of(0,0,0,16,16,16)));
    public static final BoxUnion LOWER_SLAB = new BoxUnion(List.of(Box16.of(0,0,0,16,8,16)));
    // Canonical ascent towards north (-Z); corners/connections belong to a later adapter.
    public static final BoxUnion STRAIGHT_STAIR = new BoxUnion(List.of(
        Box16.of(0,0,0,16,8,16), Box16.of(0,8,0,16,16,8)));

    private ConstructionShapes() {}

    public static BoxUnion slab(int orientation) {
        // Stable IDs for the six directions of local down, not six arbitrary offsets.
        if (orientation != 0 && orientation != 4 && orientation != 8
            && orientation != 12 && orientation != 16 && orientation != 20) {
            throw new IllegalArgumentException("Not a canonical slab orientation");
        }
        return LOWER_SLAB.transform(Rotation.of(orientation), GridPoint.BLOCK_CENTER, GridPoint.ZERO);
    }
}
