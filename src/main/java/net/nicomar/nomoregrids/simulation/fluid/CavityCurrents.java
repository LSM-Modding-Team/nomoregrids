package net.nicomar.nomoregrids.simulation.fluid;

import java.util.*;
import java.util.function.Function;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.ConvexVolume;

/** Contact-weighted flow follows the same distance field as cavity relaxation, never a host-wide level. */
public final class CavityCurrents {
    private CavityCurrents(){}
    public static ConvexVolume.Vec flow(CellPos origin,CavityCell cavity,CavityContacts.Body body,CavityCell.Kind kind,Function<CellPos,CavityCell> neighborhood){
        double x=0,y=0,z=0,weight=0;long bx=origin.x()*16,by=origin.y()*16,bz=origin.z()*16;
        int x0=(int)Math.max(0,Math.floor(body.x0()-bx)),y0=(int)Math.max(0,Math.floor(body.y0()-by)),z0=(int)Math.max(0,Math.floor(body.z0()-bz)),x1=(int)Math.min(16,Math.ceil(body.x1()-bx)),y1=(int)Math.min(16,Math.ceil(body.y1()-by)),z1=(int)Math.min(16,Math.ceil(body.z1()-bz));
        for(int a=x0;a<x1;a++)for(int b=y0;b<y1;b++)for(int c=z0;c<z1;c++){int index=a+16*(b+16*c);if(cavity.at(index)!=kind)continue;double volume=(Math.min(body.x1(),bx+a+1)-Math.max(body.x0(),bx+a))*(Math.min(body.y1(),by+b+1)-Math.max(body.y0(),by+b))*(Math.min(body.z1(),bz+c+1)-Math.max(body.z0(),bz+c));if(volume<=0)continue;int distance=cavity.distance(index);double dx=0,dy=0,dz=0;
            for(var direction:List.of(Direction6.NORTH,Direction6.SOUTH,Direction6.EAST,Direction6.WEST)){var point=new GridPoint(bx+a+direction.x,by+b,bz+c+direction.z);var other=point.cell().equals(origin)?cavity:neighborhood.apply(point.cell());if(other==null)continue;var local=point.offsetInCell();int at=(int)(local.x()+16*(local.y()+16*local.z()));if(!other.free(at)||other.at(at)!=kind&&other.at(at)!=CavityCell.Kind.EMPTY)continue;double gradient=other.at(at)==kind?other.distance(at)-distance:1;if(gradient>0){dx+=direction.x*gradient;dz+=direction.z*gradient;}}
            var below=new GridPoint(bx+a,by+b-1,bz+c);var lower=below.cell().equals(origin)?cavity:neighborhood.apply(below.cell());if(cavity.source()==CavityCell.Kind.EMPTY&&lower!=null){var local=below.offsetInCell();int at=(int)(local.x()+16*(local.y()+16*local.z()));if(lower.free(at)&&lower.at(at)==CavityCell.Kind.EMPTY)dy=-6;}
            double length=Math.hypot(Math.hypot(dx,dy),dz);if(length>0){x+=dx/length*volume;y+=dy/length*volume;z+=dz/length*volume;}weight+=volume;
        }
        return weight==0?new ConvexVolume.Vec(0,0,0):new ConvexVolume.Vec(x/weight,y/weight,z/weight);
    }
}
