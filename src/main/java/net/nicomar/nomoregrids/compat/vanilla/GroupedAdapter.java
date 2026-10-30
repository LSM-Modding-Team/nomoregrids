package net.nicomar.nomoregrids.compat.vanilla;

import java.util.*;
import net.nicomar.nomoregrids.api.adapter.PieceAdapter;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.ConvexVolume;

/** Explicit doors, trapdoors and beds; membership lives in the canonical records. */
public final class GroupedAdapter implements PieceAdapter {
    public ResourceId id(){return new ResourceId("nomoregrids:grouped");}
    public int dataVersion(){return 1;}
    public Set<Capability> capabilities(){return Set.of(Capability.OFFSET,Capability.ROTATE,Capability.COMPOSE,Capability.INTERACT,Capability.MOVE);}
    public List<ConvexVolume> occupation(PieceRecord p){return p.geometry().occupation().boxes().stream().map(b->ConvexVolume.box(b.min().x(),b.min().y(),b.min().z(),b.max().x(),b.max().y(),b.max().z())).toList();}
}
