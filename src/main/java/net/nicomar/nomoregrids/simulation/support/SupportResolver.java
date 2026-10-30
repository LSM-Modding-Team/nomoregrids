package net.nicomar.nomoregrids.simulation.support;

import java.util.*;
import net.nicomar.nomoregrids.core.Direction6;

/** Exact integral rectangular patches; equality of cell alone never establishes support. */
public final class SupportResolver {
    public record Patch(Direction6 normal,long plane,long u0,long v0,long u1,long v1){
        public Patch{if(u0>=u1||v0>=v1)throw new IllegalArgumentException("Empty support patch");}
    }
    private SupportResolver(){}
    public static boolean covered(Patch required,Collection<Patch> surfaces){
        var candidates=surfaces.stream().filter(p->p.normal==required.normal.opposite()&&p.plane==required.plane
            &&p.u0<required.u1&&p.u1>required.u0&&p.v0<required.v1&&p.v1>required.v0).toList();
        var cuts=new TreeSet<Long>(List.of(required.u0,required.u1));
        for(var p:candidates){cuts.add(Math.max(required.u0,p.u0));cuts.add(Math.min(required.u1,p.u1));}
        var x=new ArrayList<>(cuts);
        for(int i=0;i+1<x.size();i++){
            long a=x.get(i),b=x.get(i+1),covered=required.v0;
            var strip=candidates.stream().filter(p->p.u0<=a&&p.u1>=b).sorted(Comparator.comparingLong(Patch::v0)).toList();
            for(var p:strip){if(p.v0>covered)break;covered=Math.max(covered,p.v1);}
            if(covered<required.v1)return false;
        }
        return true;
    }
    /** All contributors to required coverage must be rooted; unsupported cycles never bootstrap themselves. */
    public static Set<UUID> unsupported(Map<UUID,Set<UUID>> dependencies,Set<UUID> stableRoots){
        Set<UUID> stable=new HashSet<>(stableRoots);boolean changed;
        do{changed=false;for(var e:dependencies.entrySet())if(!stable.contains(e.getKey())&&!e.getValue().isEmpty()&&stable.containsAll(e.getValue()))changed|=stable.add(e.getKey());}while(changed);
        Set<UUID> invalid=new HashSet<>(dependencies.keySet());invalid.removeAll(stable);return Set.copyOf(invalid);
    }
}
