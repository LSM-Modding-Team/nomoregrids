package net.nicomar.nomoregrids.compat.vanilla;

import java.util.*;
import net.nicomar.nomoregrids.api.adapter.PieceAdapter;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.ConvexVolume;

public final class BoxAdapter implements PieceAdapter {
    @Override public ResourceId id(){return new ResourceId("nomoregrids:box");}
    @Override public int dataVersion(){return 1;}
    @Override public Set<Capability> capabilities(){return Set.of(Capability.OFFSET,Capability.ROTATE,Capability.COMPOSE,Capability.MOVE);}
    @Override public List<ConvexVolume> occupation(PieceRecord piece){return piece.geometry().occupation().boxes().stream().map(b->
        ConvexVolume.box(b.min().x(),b.min().y(),b.min().z(),b.max().x(),b.max().y(),b.max().z())).toList();}
}
