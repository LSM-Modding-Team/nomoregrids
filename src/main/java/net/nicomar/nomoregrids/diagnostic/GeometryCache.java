package net.nicomar.nomoregrids.diagnostic;

import java.util.*;
import net.nicomar.nomoregrids.geometry.*;

/** Shared immutable union meshes, weighted by retained faces and key geometry. No world/private machine data. */
public final class GeometryCache {
    private static final WeightedLru<BoxUnion,List<BoxMesh.Face>> FACES=new WeightedLru<>(16*1024*1024);
    private GeometryCache(){}
    public static List<BoxMesh.Face> faces(BoxUnion shape,int limit){
        var cached=FACES.get(shape);if(cached.isPresent()){var value=cached.orElseThrow();if(value.size()>limit)throw new IllegalArgumentException("Mesh complexity limit");return value;}
        var built=BoxMesh.extract(shape,limit);FACES.put(shape,built,128L+shape.boxes().size()*112L+built.size()*80L);return built;
    }
    public static WeightedLru.Stats stats(){return FACES.stats();}
    public static void clear(){FACES.clear();}
}
