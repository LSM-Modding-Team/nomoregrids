package net.nicomar.nomoregrids.simulation.fluid;

import java.util.*;
import net.nicomar.nomoregrids.core.CellPos;

/** Exact body/eye contact with the conservative fluid microvolumes, independent of Minecraft classes. */
public final class CavityContacts {
    public record Body(double x0,double y0,double z0,double x1,double y1,double z1){
        public Body{if(!Double.isFinite(x0)||!Double.isFinite(y0)||!Double.isFinite(z0)||!Double.isFinite(x1)||!Double.isFinite(y1)||!Double.isFinite(z1)||x0>x1||y0>y1||z0>z1)throw new IllegalArgumentException("Fluid body bounds");}
    }
    public record Contact(double top16,boolean eyesInside,double volume16){}
    private CavityContacts(){}
    public static Map<CavityCell.Kind,Contact> query(CellPos origin,CavityCell cavity,Body body,double eyeX,double eyeY,double eyeZ){
        long bx=Math.multiplyExact(origin.x(),16),by=Math.multiplyExact(origin.y(),16),bz=Math.multiplyExact(origin.z(),16);var output=new EnumMap<CavityCell.Kind,Contact>(CavityCell.Kind.class);
        int x0=(int)Math.max(0,Math.floor(body.x0-bx)),y0=(int)Math.max(0,Math.floor(body.y0-by)),z0=(int)Math.max(0,Math.floor(body.z0-bz));int x1=(int)Math.min(16,Math.ceil(body.x1-bx)),y1=(int)Math.min(16,Math.ceil(body.y1-by)),z1=(int)Math.min(16,Math.ceil(body.z1-bz));
        for(int z=z0;z<z1;z++)for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++){
            var kind=cavity.at(x+16*(y+16*z));if(kind==CavityCell.Kind.EMPTY)continue;
            double dx=Math.min(body.x1,bx+x+1)-Math.max(body.x0,bx+x),dy=Math.min(body.y1,by+y+1)-Math.max(body.y0,by+y),dz=Math.min(body.z1,bz+z+1)-Math.max(body.z0,bz+z);if(dx<=0||dy<=0||dz<=0)continue;
            boolean eyes=eyeX>=bx+x&&eyeX<bx+x+1&&eyeY>=by+y&&eyeY<by+y+1&&eyeZ>=bz+z&&eyeZ<bz+z+1;var previous=output.get(kind);output.put(kind,new Contact(Math.max(previous==null?Double.NEGATIVE_INFINITY:previous.top16,by+y+1),eyes||previous!=null&&previous.eyesInside,(previous==null?0:previous.volume16)+dx*dy*dz));
        }return Map.copyOf(output);
    }
}
