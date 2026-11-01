package net.nicomar.nomoregrids.api.adapter;

import java.util.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.ConvexVolume;

/** Versioned opt-in capabilities. An adapter is not certified by merely registering it. */
public interface PieceAdapter {
    enum Capability { OFFSET,ROTATE,COMPOSE,RESIZE,INTERACT,MOVE }
    ResourceId id();
    int dataVersion();
    Set<Capability> capabilities();
    /** Immutable local occupation reconstructed from authoritative adapter data, not a rounded snapshot. */
    List<ConvexVolume> occupation(PieceRecord piece);
}
