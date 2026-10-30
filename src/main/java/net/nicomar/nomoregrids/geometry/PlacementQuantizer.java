package net.nicomar.nomoregrids.geometry;

/** Quantizes block coordinates. Does not invent contact across rounding gaps. */
public final class PlacementQuantizer {
    private PlacementQuantizer() {}

    public static long tangential(double blocks) {
        return checked(Math.floor(units(blocks) + 0.5));
    }

    public static long outward(double blocks, boolean positiveNormal) {
        double q = units(blocks);
        return checked(positiveNormal ? Math.ceil(q) : Math.floor(q));
    }

    private static double units(double blocks) {
        double q = blocks * 16;
        if (!Double.isFinite(q)) throw new IllegalArgumentException("Non-finite coordinate");
        return q;
    }

    private static long checked(double integral) {
        if (integral < -0x1.0p63 || integral >= 0x1.0p63) throw new IllegalArgumentException("Coordinate overflow");
        return (long) integral;
    }
}
