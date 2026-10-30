package net.nicomar.nomoregrids.compat.paintableblocks;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.nicomar.nomoregrids.api.adapter.PieceAdapter;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.*;

/** Optional bridge for the inspected PB BlockDesign v2 API. No PB classes in static linkage. */
public final class PaintableAdapter implements PieceAdapter {
    public static final String REFERENCE_COMMIT="e93bc03f350dfb03b4031f1e479f4a37f28b556e";
    private final Class<?> designClass;
    private final Codec<Object> codec;
    private final Method physical,resolution,occupied,slope,paints;
    @SuppressWarnings("unchecked")
    public PaintableAdapter(ClassLoader loader) throws ReflectiveOperationException {
        designClass=Class.forName("net.nicomar.paintableblocks.design.BlockDesign",true,loader);
        if(designClass.getField("VERSION").getInt(null)!=2)throw new IllegalArgumentException("Unsupported PB schema; preserve designs");
        codec=(Codec<Object>)designClass.getField("CODEC").get(null);
        physical=designClass.getMethod("physical");resolution=designClass.getMethod("resolution");occupied=designClass.getMethod("occupied",int.class);
        slope=designClass.getMethod("slope");paints=designClass.getMethod("paints");
    }
    @Override public ResourceId id(){return new ResourceId("nomoregrids:paintableblocks");}
    @Override public int dataVersion(){return 1;}
    @Override public Set<Capability> capabilities(){return Set.of(Capability.OFFSET,Capability.ROTATE,Capability.COMPOSE,Capability.INTERACT,Capability.MOVE);}
    public BinaryData capture(Object nativeDesign){
        if(!designClass.isInstance(nativeDesign))throw new IllegalArgumentException("Not PB design");
        String json=codec.encodeStart(JsonOps.INSTANCE,nativeDesign).getOrThrow().toString();
        if(json.length()>BinaryData.MAX_BYTES)throw new IllegalArgumentException("PB payload exceeds budget");
        return new BinaryData(json.getBytes(StandardCharsets.UTF_8));
    }
    public Object restore(BinaryData data){return codec.parse(JsonOps.INSTANCE,JsonParser.parseString(new String(data.copy(),StandardCharsets.UTF_8))).getOrThrow();}
    public record Primitive(int cell,ConvexVolume geometry,Map<Integer,Integer> paint){public Primitive{paint=Map.copyOf(paint);}}
    /** physical() applies PB's own orientation exactly once. NMG applies only its external transform. */
    @SuppressWarnings("unchecked")
    public List<Primitive> primitives(BinaryData data){
        try{
            Object design=physical.invoke(restore(data));int n=(int)resolution.invoke(design);
            if(n<1||n>16||16%n!=0)throw new IllegalArgumentException("Unsupported PB resolution");
            Object plane=slope.invoke(design);ConvexVolume.Plane clip=null;
            if(plane!=null){Class<?> type=plane.getClass();clip=new ConvexVolume.Plane(new ConvexVolume.Vec(
                (int)type.getMethod("a").invoke(plane),(int)type.getMethod("b").invoke(plane),(int)type.getMethod("c").invoke(plane)),16.0*(int)type.getMethod("offset").invoke(plane));}
            Map<Integer,Integer> paint=(Map<Integer,Integer>)paints.invoke(design);var result=new ArrayList<Primitive>();double size=16.0/n;
            for(int cell=0;cell<n*n*n;cell++)if((boolean)occupied.invoke(design,cell)){
                double x=cell%n*size,y=cell/n%n*size,z=cell/(n*n)*size;
                ConvexVolume volume=ConvexVolume.box(x,y,z,x+size,y+size,z+size);if(clip!=null)volume=volume.clipped(clip);
                if(!volume.hasVolume())throw new IllegalArgumentException("Degenerate PB primitive");
                Map<Integer,Integer> surfaces=new HashMap<>();for(int face=0;face<6;face++)if(paint.containsKey(cell*6+face))surfaces.put(face,paint.get(cell*6+face));
                if(paint.containsKey(24576+cell))surfaces.put(6,paint.get(24576+cell));result.add(new Primitive(cell,volume,surfaces));
            }
            if(result.isEmpty()||result.size()>16384)throw new IllegalArgumentException("PB geometry budget");return List.copyOf(result);
        }catch(ReflectiveOperationException ex){throw new IllegalArgumentException("PB API mismatch; original payload retained",ex);}
    }
    @Override public List<ConvexVolume> occupation(PieceRecord piece){return primitives(piece.geometryPayload()).stream().map(Primitive::geometry).toList();}
    /** Both painting and geometry edits are candidates; no native costs or setters run during validation. */
    public BinaryData prepareEdit(PieceRecord old,Object candidate,java.util.function.Predicate<List<ConvexVolume>> validate){
        if(old.flags().contains(PieceRecord.Flag.SUBSTITUTE))throw new IllegalArgumentException("Uneditable substitute");
        BinaryData data=capture(candidate);var geometry=primitives(data).stream().map(Primitive::geometry).toList();
        if(!validate.test(geometry))throw new IllegalArgumentException("OVERLAP or NO_SUPPORT");return data;
    }
}
