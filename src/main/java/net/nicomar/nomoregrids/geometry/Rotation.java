package net.nicomar.nomoregrids.geometry;

import java.util.List;
import java.util.stream.IntStream;
import net.nicomar.nomoregrids.core.GridPoint;

/** Version 1 normative proper rotations. IDs match Tabla de rotaciones.md. */
public final class Rotation {
    // U = world direction of local +Y; F = world direction of local -Z.
    private static final int[][] FRAMES = {
        {0,1,0, 0,0,-1}, {0,1,0, 1,0,0}, {0,1,0, 0,0,1}, {0,1,0, -1,0,0},
        {0,-1,0, 0,0,-1}, {0,-1,0, -1,0,0}, {0,-1,0, 0,0,1}, {0,-1,0, 1,0,0},
        {0,0,-1, 0,1,0}, {0,0,-1, 1,0,0}, {0,0,-1, 0,-1,0}, {0,0,-1, -1,0,0},
        {0,0,1, 0,1,0}, {0,0,1, -1,0,0}, {0,0,1, 0,-1,0}, {0,0,1, 1,0,0},
        {-1,0,0, 0,0,-1}, {-1,0,0, 0,1,0}, {-1,0,0, 0,0,1}, {-1,0,0, 0,-1,0},
        {1,0,0, 0,0,-1}, {1,0,0, 0,-1,0}, {1,0,0, 0,0,1}, {1,0,0, 0,1,0}
    };
    private static final List<Rotation> ALL = IntStream.range(0, 24).mapToObj(Rotation::new).toList();
    private final int id;
    private final int[][] matrix;

    private Rotation(int id) {
        this.id = id;
        int[] a = FRAMES[id];
        int ux = a[0], uy = a[1], uz = a[2], fx = a[3], fy = a[4], fz = a[5];
        matrix = new int[][] {
            {fy * uz - fz * uy, ux, -fx},
            {fz * ux - fx * uz, uy, -fy},
            {fx * uy - fy * ux, uz, -fz}
        };
    }

    public static Rotation of(int id) {
        if (id < 0 || id >= ALL.size()) throw new IllegalArgumentException("Invalid rotation ID: " + id);
        return ALL.get(id);
    }

    public static List<Rotation> all() { return ALL; }
    public int id() { return id; }

    public GridPoint apply(GridPoint p) {
        return new GridPoint(row(0, p), row(1, p), row(2, p));
    }

    private long row(int i, GridPoint p) {
        return Math.addExact(Math.addExact(Math.multiplyExact(matrix[i][0], p.x()),
            Math.multiplyExact(matrix[i][1], p.y())), Math.multiplyExact(matrix[i][2], p.z()));
    }

    /** this after before; composition does not depend on external enum ordinals. */
    public Rotation compose(Rotation before) {
        GridPoint x = apply(before.apply(new GridPoint(1, 0, 0)));
        GridPoint y = apply(before.apply(new GridPoint(0, 1, 0)));
        GridPoint z = apply(before.apply(new GridPoint(0, 0, 1)));
        return ALL.stream().filter(r -> r.apply(new GridPoint(1,0,0)).equals(x)
            && r.apply(new GridPoint(0,1,0)).equals(y)
            && r.apply(new GridPoint(0,0,1)).equals(z)).findFirst().orElseThrow();
    }

    public Rotation inverse() {
        return ALL.stream().filter(r -> compose(r).id == 0).findFirst().orElseThrow();
    }

    public GridPoint transform(GridPoint local, GridPoint pivot, GridPoint origin16) {
        return apply(local.subtract(pivot)).add(pivot).add(origin16);
    }
}
