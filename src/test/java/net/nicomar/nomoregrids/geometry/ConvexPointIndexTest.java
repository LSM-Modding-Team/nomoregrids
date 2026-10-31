package net.nicomar.nomoregrids.geometry;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ConvexPointIndexTest {
    private static boolean exhaustive(List<ConvexVolume> volumes,ConvexVolume.Vec point,int excluded){
        for(int i=0;i<volumes.size();i++)if(i!=excluded&&volumes.get(i).planes().stream().allMatch(p->p.normal().dot(point)<=p.distance()))return true;
        return false;
    }
    @Test void bucketsAgreeWithExhaustiveContainmentAtFacesAndSlopes(){
        var volumes=new ArrayList<ConvexVolume>();for(int x=0;x<4;x++)for(int y=0;y<4;y++)for(int z=0;z<4;z++){
            var box=ConvexVolume.box(4*x,4*y,4*z,4*x+4,4*y+4,4*z+4);
            if(x==1&&y==1&&z==1)box=box.clipped(new ConvexVolume.Plane(new ConvexVolume.Vec(1,1,0),12));volumes.add(box);
        }
        var index=new ConvexPointIndex(volumes);var random=new Random(405);
        for(int i=0;i<4000;i++){
            double x=i%3==0?4*random.nextInt(5)+((i%2==0)?ConvexVolume.EPS*4:-ConvexVolume.EPS*4):random.nextDouble()*18-1;
            var point=new ConvexVolume.Vec(x,random.nextDouble()*18-1,random.nextDouble()*18-1);int excluded=random.nextInt(volumes.size());
            assertEquals(exhaustive(volumes,point,excluded),index.covered(point,excluded));
        }
    }
    @Test void outOfRangeGeometryFallsBackToTheFullQuery(){
        var volumes=new ArrayList<ConvexVolume>();for(int i=0;i<9;i++)volumes.add(ConvexVolume.box(20+i,0,0,21+i,1,1));
        var index=new ConvexPointIndex(volumes);var point=new ConvexVolume.Vec(25.5,0.5,0.5);
        assertTrue(index.covered(point,0));assertFalse(index.covered(point,5));
    }
}
