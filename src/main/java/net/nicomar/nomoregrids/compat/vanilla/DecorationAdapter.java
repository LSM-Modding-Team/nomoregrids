package net.nicomar.nomoregrids.compat.vanilla;

import java.util.*;
import net.nicomar.nomoregrids.api.adapter.PieceAdapter;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.ConvexVolume;

/** Explicit data-bearing vanilla signs, banners and skulls. No container inventories in this profile. */
public final class DecorationAdapter implements PieceAdapter {
    @Override public ResourceId id(){return new ResourceId("nomoregrids:decoration");}
    @Override public int dataVersion(){return 1;}
    @Override public Set<Capability> capabilities(){return Set.of(Capability.OFFSET,Capability.ROTATE,Capability.COMPOSE,Capability.INTERACT,Capability.MOVE);}
    @Override public List<ConvexVolume> occupation(PieceRecord p){return boxes(p,"selection");}
    public static List<ConvexVolume> boxes(PieceRecord p,String role){var json=com.google.gson.JsonParser.parseString(new String(p.geometryPayload().copy(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();var volumes=new ArrayList<ConvexVolume>();for(var entry:json.getAsJsonArray(role)){var b=entry.getAsJsonArray();volumes.add(ConvexVolume.box(b.get(0).getAsDouble(),b.get(1).getAsDouble(),b.get(2).getAsDouble(),b.get(3).getAsDouble(),b.get(4).getAsDouble(),b.get(5).getAsDouble()));}return List.copyOf(volumes);}
}

