package net.nicomar.nomoregrids.platform;

import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.*;
import net.nicomar.nomoregrids.compat.paintableblocks.PaintableAdapter;

/** Exact adapter selection in world coordinates. Unavailable adapters retain conservative substitute queries. */
public final class PieceQueries {
    private PieceQueries(){}
    private static final class OptionalPaintable {
        static final PaintableAdapter INSTANCE=create();
        private static PaintableAdapter create(){try{return new PaintableAdapter(PieceQueries.class.getClassLoader());}catch(ReflectiveOperationException|RuntimeException ex){return null;}}
    }
    public record Hit(double distance16,Vec3 point,Direction face){}
    public static Optional<Hit> ray(PieceRecord p,Vec3 start,Vec3 end){
        Vec3 delta=end.subtract(start);double reach=delta.length()*16;if(reach<1e-12)return Optional.empty();
        try{if(p.adapterId().value().equals("nomoregrids:paintableblocks")&&!p.flags().contains(PieceRecord.Flag.SUBSTITUTE)&&OptionalPaintable.INSTANCE!=null){
            Hit closest=null;
            for(var local:OptionalPaintable.INSTANCE.occupation(p)){
                var volume=local.transformed(p.orientation(),p.pivot16(),p.origin16());
                var hit=volume.ray(new ConvexVolume.Vec(start.x*16,start.y*16,start.z*16),new ConvexVolume.Vec(delta.x,delta.y,delta.z),reach);
                if(hit.isEmpty())continue;var h=hit.orElseThrow();if(closest!=null&&closest.distance16<=h.distance())continue;
                var normal=volume.planes().get(h.plane()).normal();Direction6 face=Arrays.stream(Direction6.values()).max(Comparator.comparingDouble(d->d.x*normal.x()+d.y*normal.y()+d.z*normal.z())).orElseThrow();
                closest=new Hit(h.distance(),new Vec3(h.point().x()/16,h.point().y()/16,h.point().z()/16),Direction.valueOf(face.name()));
            }return Optional.ofNullable(closest);
        }}catch(RuntimeException ex){/* Preserve an unavailable/invalid design behind its conservative snapshot. */}
        return BoxRaycast.cast(p.geometry().selection().transform(Rotation.of(p.orientation()),p.pivot16(),p.origin16()),new BoxRaycast.Vector(start.x*16,start.y*16,start.z*16),new BoxRaycast.Vector(delta.x,delta.y,delta.z),reach)
            .map(h->new Hit(h.distance16(),new Vec3(h.point16().x()/16,h.point16().y()/16,h.point16().z()/16),Direction.valueOf(h.face().name())));
    }
}
