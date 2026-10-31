package net.nicomar.nomoregrids.geometry;

import java.util.*;

/** Bounded broad phase for point containment in local 16-cube geometry. */
public final class ConvexPointIndex {
    private static final double PAD=ConvexVolume.EPS*8;
    private static final int MAX_REFERENCES=262144;
    private final List<ConvexVolume> volumes;
    private final List<List<Integer>> bins;

    public ConvexPointIndex(List<ConvexVolume> volumes){
        this.volumes=List.copyOf(volumes);
        if(volumes.size()<=8){bins=List.of();return;}
        var buckets=new ArrayList<List<Integer>>(Collections.nCopies(4096,null));int references=0;
        for(int i=0;i<volumes.size();i++){
            var vertices=volumes.get(i).vertices();if(vertices.isEmpty()){bins=List.of();return;}
            double x0=Double.POSITIVE_INFINITY,y0=x0,z0=x0,x1=Double.NEGATIVE_INFINITY,y1=x1,z1=x1;
            for(var p:vertices){x0=Math.min(x0,p.x());y0=Math.min(y0,p.y());z0=Math.min(z0,p.z());x1=Math.max(x1,p.x());y1=Math.max(y1,p.y());z1=Math.max(z1,p.z());}
            if(x0 < -PAD||y0 < -PAD||z0 < -PAD||x1 > 16+PAD||y1 > 16+PAD||z1 > 16+PAD){bins=List.of();return;}
            int minX=bin(x0-PAD),minY=bin(y0-PAD),minZ=bin(z0-PAD),maxX=bin(x1+PAD),maxY=bin(y1+PAD),maxZ=bin(z1+PAD);
            for(int z=minZ;z<=maxZ;z++)for(int y=minY;y<=maxY;y++)for(int x=minX;x<=maxX;x++){
                if(++references>MAX_REFERENCES){bins=List.of();return;}
                int cell=x+16*(y+16*z);var bucket=buckets.get(cell);if(bucket==null){bucket=new ArrayList<>();buckets.set(cell,bucket);}bucket.add(i);
            }
        }
        bins=buckets;
    }
    private static int bin(double n){return Math.max(0,Math.min(15,(int)Math.floor(n)));}
    public long estimatedBytes(){long bytes=128L+volumes.size()*8L+bins.size()*8L;for(var volume:volumes)bytes+=128L+volume.planes().size()*64L+volume.vertices().size()*48L;for(var bucket:bins)if(bucket!=null)bytes+=64L+bucket.size()*24L;return bytes;}
    /** Uses the same exact plane comparisons as an exhaustive query. */
    public boolean covered(ConvexVolume.Vec point,int excluded){
        if(!bins.isEmpty()&&(point.x() < -PAD||point.y() < -PAD||point.z() < -PAD||point.x() > 16+PAD||point.y() > 16+PAD||point.z() > 16+PAD))return false;
        if(bins.isEmpty()){for(int i=0;i<volumes.size();i++)if(i!=excluded&&contains(volumes.get(i),point))return true;return false;}
        var candidates=bins.get(bin(point.x())+16*(bin(point.y())+16*bin(point.z())));if(candidates==null)return false;
        for(int i:candidates)if(i!=excluded&&contains(volumes.get(i),point))return true;return false;
    }
    private static boolean contains(ConvexVolume volume,ConvexVolume.Vec point){for(var plane:volume.planes())if(plane.normal().dot(point)>plane.distance())return false;return true;}
}
