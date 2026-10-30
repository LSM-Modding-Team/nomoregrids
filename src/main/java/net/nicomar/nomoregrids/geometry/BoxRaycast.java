package net.nicomar.nomoregrids.geometry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

/** Ray selection of the actual box union, including rays starting inside it. Units: 1/16. */
public final class BoxRaycast {
    public enum Face { DOWN, UP, NORTH, SOUTH, WEST, EAST }

    public record Vector(double x, double y, double z) {
        public Vector {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("Non-finite ray vector");
            }
        }
    }

    public record Hit(double distance16, Vector point16, Face face, int primitive) {}
    private record Interval(double start, double end, Face entry, Face exit, int entryBox, int exitBox) {}
    private static final Face[] LOW = {Face.WEST, Face.DOWN, Face.NORTH};
    private static final Face[] HIGH = {Face.EAST, Face.UP, Face.SOUTH};

    private BoxRaycast() {}

    public static Optional<Hit> cast(BoxUnion shape, Vector origin16, Vector direction, double maxDistance16) {
        if (!Double.isFinite(maxDistance16) || maxDistance16 < 0) throw new IllegalArgumentException("Invalid reach");
        double length = Math.hypot(Math.hypot(direction.x, direction.y), direction.z);
        if (!Double.isFinite(length) || length == 0) throw new IllegalArgumentException("Invalid direction");
        double[] o = {origin16.x, origin16.y, origin16.z};
        double[] d = {direction.x/length, direction.y/length, direction.z/length};
        ArrayList<Interval> intervals = new ArrayList<>();
        for (int i = 0; i < shape.boxes().size(); i++) {
            Interval interval = intersect(shape.boxes().get(i), o, d, i);
            if (interval != null && interval.end >= 0 && interval.start <= maxDistance16) intervals.add(interval);
        }
        intervals.sort(Comparator.comparingDouble(Interval::start).thenComparingInt(Interval::entryBox));
        for (int i = 0; i < intervals.size(); i++) {
            Interval union = intervals.get(i);
            while (i + 1 < intervals.size() && intervals.get(i + 1).start <= union.end) {
                Interval next = intervals.get(++i);
                if (next.end > union.end) {
                    union = new Interval(union.start, next.end, union.entry, next.exit, union.entryBox, next.exitBox);
                }
            }
            boolean entering = union.start >= 0;
            double distance = entering ? union.start : union.end;
            if (distance > maxDistance16) return Optional.empty();
            return Optional.of(new Hit(distance,
                new Vector(o[0]+d[0]*distance, o[1]+d[1]*distance, o[2]+d[2]*distance),
                entering ? union.entry : union.exit, entering ? union.entryBox : union.exitBox));
        }
        return Optional.empty();
    }

    private static Interval intersect(Box16 box, double[] o, double[] d, int primitive) {
        double[] low = {box.min().x(), box.min().y(), box.min().z()};
        double[] high = {box.max().x(), box.max().y(), box.max().z()};
        double near = Double.NEGATIVE_INFINITY, far = Double.POSITIVE_INFINITY;
        Face entry = Face.DOWN, exit = Face.UP;
        // Deterministic ties: Y, Z, X.
        for (int axis : new int[] {1,2,0}) {
            if (d[axis] == 0) {
                if (o[axis] < low[axis] || o[axis] > high[axis]) return null;
                continue;
            }
            double a = (low[axis]-o[axis])/d[axis], b = (high[axis]-o[axis])/d[axis];
            Face faceA = LOW[axis], faceB = HIGH[axis];
            if (a > b) {
                double swap = a; a = b; b = swap;
                Face swapFace = faceA; faceA = faceB; faceB = swapFace;
            }
            if (a > near) { near = a; entry = faceA; }
            if (b < far) { far = b; exit = faceB; }
            if (near > far) return null;
        }
        return new Interval(near, far, entry, exit, primitive, primitive);
    }
}
