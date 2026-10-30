package net.nicomar.nomoregrids.compat.vanilla;

import java.util.*;
import net.nicomar.nomoregrids.api.adapter.PieceAdapter;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.ConvexVolume;

/** Explicit vanilla container/cooking profile. Inventories are private world effects, absent from replicas. */
public final class FunctionalAdapter implements PieceAdapter {
    @Override public ResourceId id(){return new ResourceId("nomoregrids:functional");}
    @Override public int dataVersion(){return 1;}
    @Override public Set<Capability> capabilities(){return Set.of(Capability.OFFSET,Capability.ROTATE,Capability.COMPOSE,Capability.INTERACT,Capability.MOVE);}
    @Override public List<ConvexVolume> occupation(PieceRecord p){return p.geometry().occupation().boxes().stream().map(b->ConvexVolume.box(b.min().x(),b.min().y(),b.min().z(),b.max().x(),b.max().y(),b.max().z())).toList();}
}
