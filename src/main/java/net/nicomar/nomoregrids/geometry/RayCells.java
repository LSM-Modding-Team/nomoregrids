package net.nicomar.nomoregrids.geometry;

import java.util.*;
import net.nicomar.nomoregrids.core.CellPos;

/** Exact voxel traversal of a finite segment; edge-only zero-volume contacts do not add cells. */
public final class RayCells {
    private RayCells(){}
    public static List<CellPos> trace(ConvexVolume.Vec start,ConvexVolume.Vec end,int budget){
        if(budget<1)throw new IllegalArgumentException("Ray budget");double[] p={start.x(),start.y(),start.z()},d={end.x()-start.x(),end.y()-start.y(),end.z()-start.z()},next=new double[3],stepTime=new double[3];long[] cell={(long)Math.floor(p[0]),(long)Math.floor(p[1]),(long)Math.floor(p[2])};int[] direction=new int[3];
        for(int i=0;i<3;i++){direction[i]=d[i]>0?1:d[i]<0?-1:0;stepTime[i]=direction[i]==0?Double.POSITIVE_INFINITY:Math.abs(1/d[i]);next[i]=direction[i]==0?Double.POSITIVE_INFINITY:((direction[i]>0?cell[i]+1:cell[i])-p[i])/d[i];}
        var result=new ArrayList<CellPos>();while(true){if(result.size()==budget)throw new IllegalArgumentException("COMPLEXITY_LIMIT: ray traversal");result.add(new CellPos(cell[0],cell[1],cell[2]));double t=Math.min(next[0],Math.min(next[1],next[2]));if(t>1||!Double.isFinite(t))break;for(int i=0;i<3;i++)if(next[i]<=t+1e-12){cell[i]+=direction[i];next[i]+=stepTime[i];}}
        return List.copyOf(result);
    }
}
