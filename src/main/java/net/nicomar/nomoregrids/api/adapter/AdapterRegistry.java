package net.nicomar.nomoregrids.api.adapter;

import java.util.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.ConvexVolume;

/** Registration is frozen before opening worlds. Unknown versions remain unavailable, never guessed. */
public final class AdapterRegistry {
    private final Map<ResourceId,PieceAdapter> adapters=new HashMap<>();private boolean frozen;
    public void register(PieceAdapter adapter){if(frozen)throw new IllegalStateException("Registry frozen");if(adapters.putIfAbsent(adapter.id(),adapter)!=null)throw new IllegalArgumentException("Duplicate adapter");}
    public void freeze(){frozen=true;}
    public PieceAdapter require(PieceRecord piece){
        var a=adapters.get(piece.adapterId());if(a==null||a.dataVersion()!=piece.adapterVersion())throw new IllegalArgumentException("UNSUPPORTED_ADAPTER: "+piece.adapterId());return a;
    }
    public Map<ResourceId,Integer> versions(){var result=new HashMap<ResourceId,Integer>();adapters.forEach((id,a)->result.put(id,a.dataVersion()));return Map.copyOf(result);}
    public boolean overlap(PieceRecord a,PieceRecord b){
        if(!a.worldOccupation().bounds().overlaps(b.worldOccupation().bounds()))return false;
        for(var x:require(a).occupation(a))for(var y:require(b).occupation(b)){
            var contact=x.transformed(a.orientation(),a.pivot16(),a.origin16()).contact(y.transformed(b.orientation(),b.pivot16(),b.origin16()));
            if(contact==ConvexVolume.Contact.INDETERMINATE)throw new IllegalArgumentException("Indeterminate geometric contact");
            if(contact==ConvexVolume.Contact.OVERLAP)return true;
        }return false;
    }
}
