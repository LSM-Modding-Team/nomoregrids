package net.nicomar.nomoregrids.simulation.fluid;

import java.util.*;
import net.nicomar.nomoregrids.geometry.ConvexVolume;

/** Incremental conservative fluid mask for exact external convex geometry. Never changes its geometry. */
public final class CavityMaskJob {
    private record Bounds(double x0,double y0,double z0,double x1,double y1,double z1){}
    private final List<ConvexVolume> solids;
    private final List<Bounds> bounds;
    private final BitSet mask=new BitSet(4096);
    private int cursor;
    public CavityMaskJob(List<ConvexVolume> local){
        if(local.size()>4096)throw new IllegalArgumentException("Fluid geometry budget");solids=List.copyOf(local);var boxes=new ArrayList<Bounds>();
        for(var solid:solids){
            var vertices=solid.vertices();if(vertices.isEmpty())throw new IllegalArgumentException("Unbounded/empty cavity solid");
            double x0=Double.POSITIVE_INFINITY,y0=x0,z0=x0,x1=Double.NEGATIVE_INFINITY,y1=x1,z1=x1;
            for(var p:vertices){x0=Math.min(x0,p.x());x1=Math.max(x1,p.x());y0=Math.min(y0,p.y());y1=Math.max(y1,p.y());z0=Math.min(z0,p.z());z1=Math.max(z1,p.z());}
            if(x0< -ConvexVolume.EPS||y0< -ConvexVolume.EPS||z0< -ConvexVolume.EPS||x1>16+ConvexVolume.EPS||y1>16+ConvexVolume.EPS||z1>16+ConvexVolume.EPS)throw new IllegalArgumentException("Fluid geometry must be clipped to its cell");
            boxes.add(new Bounds(x0,y0,z0,x1,y1,z1));
        }bounds=List.copyOf(boxes);
    }
    public boolean advance(int microcells){
        if(microcells<1||microcells>4096)throw new IllegalArgumentException("Fluid mask work budget");int end=Math.min(4096,cursor+microcells);
        for(;cursor<end;cursor++){
            int x=cursor%16,y=cursor/16%16,z=cursor/256;ConvexVolume cube=null;
            for(int j=0;j<solids.size();j++){
                var b=bounds.get(j);if(x>=b.x1||x+1<=b.x0||y>=b.y1||y+1<=b.y0||z>=b.z1||z+1<=b.z0)continue;
                if(cube==null)cube=ConvexVolume.box(x,y,z,x+1,y+1,z+1);var contact=solids.get(j).contact(cube);
                if(contact==ConvexVolume.Contact.OVERLAP||contact==ConvexVolume.Contact.INDETERMINATE){mask.set(cursor);break;}
            }
        }return cursor==4096;
    }
    public BitSet result(){if(cursor!=4096)throw new IllegalStateException("Cavity mask pending");return (BitSet)mask.clone();}
}
