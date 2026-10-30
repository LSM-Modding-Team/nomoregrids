package net.nicomar.nomoregrids.persistence;

import java.util.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.simulation.fluid.CavityCell;
import net.nicomar.nomoregrids.simulation.functional.FunctionalStore;
import net.nicomar.nomoregrids.simulation.movement.GroupRegistry;
import net.nicomar.nomoregrids.simulation.redstone.SignalNetwork;

/** One checkpoint region. Inventory exists only in FunctionalStore.State, never in references or groups. */
public record SimulationSnapshot(SectionPos section,long checkpoint,Map<CellPos,CavityCell> cavities,Map<UUID,FunctionalStore.State> functionals,Map<UUID,Set<UUID>> groups,Map<UUID,SignalNetwork.Snapshot> circuits){
    public SimulationSnapshot{
        Objects.requireNonNull(section);if(checkpoint<0||cavities.size()>256||functionals.size()>4096||groups.size()>4096||circuits.size()>256)throw new IllegalArgumentException("Simulation region budget");
        cavities=Map.copyOf(cavities);functionals=Map.copyOf(functionals);circuits=Map.copyOf(circuits);Map<UUID,Set<UUID>> copy=new HashMap<>();groups.forEach((id,members)->copy.put(id,Set.copyOf(members)));groups=Map.copyOf(copy);if(groups.values().stream().mapToInt(Set::size).sum()>16384||functionals.values().stream().mapToInt(state->state.slots().size()).sum()>65536)throw new IllegalArgumentException("Aggregate simulation reference budget");GroupRegistry.restored(groups);
        for(var pos:cavities.keySet())if(!pos.section().equals(section))throw new IllegalArgumentException("Cavity owner section");
        functionals.forEach((id,state)->{if(!id.equals(state.piece()))throw new IllegalArgumentException("Functional authority mismatch");});
        circuits.values().forEach(SignalNetwork::restored);
    }
}
