package net.nicomar.nomoregrids.simulation.contact;

import java.util.*;
import net.nicomar.nomoregrids.core.Direction6;
import net.nicomar.nomoregrids.geometry.BoxMesh;

/** Foot support chooses the largest real contact area, with a stable UUID tie breaker. */
public final class ContactResolver {
    public record Foot(double x0,double z0,double x1,double z1,double y){
        public Foot{if(!Double.isFinite(x0)||!Double.isFinite(z0)||!Double.isFinite(x1)||!Double.isFinite(z1)||!Double.isFinite(y)||x0>=x1||z0>=z1)throw new IllegalArgumentException("Invalid contact footprint");}
    }
    public record Contact(UUID piece,double area16){}
    private record Rect(double x0,double z0,double x1,double z1){}
    private ContactResolver(){}
    public static Optional<Contact> ground(Foot foot,Map<UUID,List<BoxMesh.Face>> surfaces){
        Contact selected=null;for(var entry:surfaces.entrySet()){
            List<Rect> patches=new ArrayList<>();for(var face:entry.getValue())if(face.normal()==Direction6.UP&&Math.abs(face.plane()-foot.y)<=16e-7){
                var r=face.rect();double x0=Math.max(foot.x0,r.u0()),x1=Math.min(foot.x1,r.u1()),z0=Math.max(foot.z0,r.v0()),z1=Math.min(foot.z1,r.v1());if(x0<x1&&z0<z1)patches.add(new Rect(x0,z0,x1,z1));
            }
            double area=area(patches);if(area>0&&(selected==null||area>selected.area16||area==selected.area16&&entry.getKey().compareTo(selected.piece)<0))selected=new Contact(entry.getKey(),area);
        }return Optional.ofNullable(selected);
    }
    private static double area(List<Rect> patches){
        var cuts=new TreeSet<Double>();for(var p:patches){cuts.add(p.x0);cuts.add(p.x1);}var x=new ArrayList<>(cuts);double area=0;
        for(int i=0;i+1<x.size();i++){
            double x0=x.get(i),x1=x.get(i+1),start=0,end=0,length=0;boolean found=false;
            for(var p:patches.stream().filter(p->p.x0<=x0&&p.x1>=x1).sorted(Comparator.comparingDouble(Rect::z0)).toList()){
                if(!found){start=p.z0;end=p.z1;found=true;}else if(p.z0<=end)end=Math.max(end,p.z1);else{length+=end-start;start=p.z0;end=p.z1;}
            }if(found)length+=end-start;area+=(x1-x0)*length;
        }return area;
    }
}
