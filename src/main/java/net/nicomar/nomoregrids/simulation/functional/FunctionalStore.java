package net.nicomar.nomoregrids.simulation.functional;

import java.util.*;
import net.nicomar.nomoregrids.core.*;

/** Single authoritative state per functional piece. Cell references never own inventories. */
public final class FunctionalStore {
    public record State(UUID piece,long revision,List<BinaryData> slots,long energy,BinaryData machine){
        public State{slots=List.copyOf(slots);Objects.requireNonNull(piece);Objects.requireNonNull(machine);if(revision<0||slots.size()>256||energy<0)throw new IllegalArgumentException("Invalid machine state");
            long bytes=machine.size();for(var s:slots)bytes+=s.size();if(bytes>BinaryData.MAX_BYTES)throw new IllegalArgumentException("Machine payload budget");}
        public State edited(List<BinaryData> slots,long energy,BinaryData machine){return new State(piece,Math.incrementExact(revision),slots,energy,machine);}
    }
    public record Port(UUID piece,String name,GridPoint position,Direction6 normal,ResourceId capability,boolean input,boolean output){}
    private final Map<UUID,State> states=new HashMap<>();
    public State require(UUID id){return Objects.requireNonNull(states.get(id),"Functional piece absent");}
    public void restore(State state){if(states.putIfAbsent(state.piece,state)!=null)throw new IllegalArgumentException("Duplicate functional authority");}
    public void replace(State expected,State next){
        if(!expected.piece.equals(next.piece)||next.revision!=expected.revision+1||!states.replace(expected.piece,expected,next))throw new IllegalArgumentException("STALE_REVISION");
    }
    public void remove(State expected){if(!states.remove(expected.piece,expected))throw new IllegalArgumentException("STALE_REVISION");}
    public Map<UUID,State> snapshot(){return Map.copyOf(states);}
    /** A cell-only query is valid solely when one exact exposed port matches. */
    public static Optional<Port> route(Collection<Port> ports,GridPoint contact,Direction6 outward,ResourceId capability){
        var matches=ports.stream().filter(p->p.position.equals(contact)&&p.normal==outward&&p.capability.equals(capability)).toList();
        return matches.size()==1?Optional.of(matches.getFirst()):Optional.empty();
    }
}
