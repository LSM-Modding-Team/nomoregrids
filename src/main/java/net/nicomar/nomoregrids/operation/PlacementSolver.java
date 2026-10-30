package net.nicomar.nomoregrids.operation;

import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.*;

/** Pure precise-placement algorithm. Surface point is in blocks; shape stays in local q units. */
public final class PlacementSolver {
    private PlacementSolver(){}
    public static GridPoint origin(BoxUnion shape,GridPoint pivot,int orientation,Direction6 normal,
            double hitX,double hitY,double hitZ,GridPoint manualOffset) {
        Box16 bounds=shape.transform(Rotation.of(orientation),pivot,GridPoint.ZERO).bounds();
        long x=PlacementQuantizer.tangential(hitX)-8,y=PlacementQuantizer.tangential(hitY)-8,z=PlacementQuantizer.tangential(hitZ)-8;
        long face=PlacementQuantizer.outward(normal.axis()==0?hitX:normal.axis()==1?hitY:hitZ,normal.sign()>0);
        if(normal.axis()==0)x=Math.subtractExact(face,normal.sign()>0?bounds.min().x():bounds.max().x());
        if(normal.axis()==1)y=Math.subtractExact(face,normal.sign()>0?bounds.min().y():bounds.max().y());
        if(normal.axis()==2)z=Math.subtractExact(face,normal.sign()>0?bounds.min().z():bounds.max().z());
        return new GridPoint(x,y,z).add(manualOffset);
    }
    /** Length to the NEXT cell boundary along transformed local +Y; never produces zero. */
    public static int adjustedLength(long start16,Direction6 growth){
        int remainder=(int)Math.floorMod(start16,16);
        return growth.sign()>0?16-remainder:remainder==0?16:remainder;
    }
    public static int sixFronts(double x,double y,double z){
        if(!Double.isFinite(x+y+z) || x==0&&y==0&&z==0)throw new IllegalArgumentException("Invalid look");
        double ax=Math.abs(x),ay=Math.abs(y),az=Math.abs(z);
        if(ay>=az && ay>=ax)return y>=0?8:10;
        if(az>=ax)return z>=0?2:0;
        return x>=0?1:3;
    }
    public static int stairOrientation(Direction6 support,double lookX,double lookY,double lookZ,int lastHorizontal){
        GridPoint up=support.vector();double dot=lookX*up.x()+lookY*up.y()+lookZ*up.z();
        double px=lookX-dot*up.x(),py=lookY-dot*up.y(),pz=lookZ-dot*up.z();
        if(px*px+py*py+pz*pz<1e-12){GridPoint fallback=Direction6.NORTH.rotated(lastHorizontal).vector();px=fallback.x();py=0;pz=fallback.z();}
        int best=-1;double score=-Double.MAX_VALUE;
        // Normative ID order is a stable final tie-breaker.
        for(var r:Rotation.all())if(r.apply(new GridPoint(0,1,0)).equals(up)){
            GridPoint front=r.apply(new GridPoint(0,0,-1));double s=front.x()*px+front.y()*py+front.z()*pz;
            if(s>score){score=s;best=r.id();}
        }
        if(best<0)throw new IllegalArgumentException("Unsupported support");return best;
    }
}
