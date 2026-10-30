package net.nicomar.nomoregrids.core;

import net.nicomar.nomoregrids.geometry.Rotation;

/** Explicit axis directions; persist names, never ordinals. */
public enum Direction6 {
    DOWN(0,-1,0),UP(0,1,0),NORTH(0,0,-1),SOUTH(0,0,1),WEST(-1,0,0),EAST(1,0,0);
    public final int x,y,z;
    Direction6(int x,int y,int z){this.x=x;this.y=y;this.z=z;}
    public GridPoint vector(){return new GridPoint(x,y,z);}
    public int axis(){return x!=0?0:y!=0?1:2;}
    public int sign(){return x+y+z;}
    public Direction6 opposite(){return of(new GridPoint(-x,-y,-z));}
    public Direction6 rotated(int orientation){return of(Rotation.of(orientation).apply(vector()));}
    public static Direction6 of(GridPoint vector){for(var d:values())if(d.vector().equals(vector))return d;throw new IllegalArgumentException("Not cardinal");}
}
