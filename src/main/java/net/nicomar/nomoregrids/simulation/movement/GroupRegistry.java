package net.nicomar.nomoregrids.simulation.movement;

import java.util.*;

/** Explicit inseparable groups, separate from incidental cell sharing. */
public final class GroupRegistry {
    private final Map<UUID,Set<UUID>> groups=new HashMap<>();private final Map<UUID,UUID> memberToGroup=new HashMap<>();
    public void register(UUID group,Set<UUID> members){
        Objects.requireNonNull(group);members=Set.copyOf(members);
        if(members.size()<2||members.size()>128||groups.containsKey(group)||members.stream().anyMatch(memberToGroup::containsKey))throw new IllegalArgumentException("Invalid group");
        groups.put(group,Set.copyOf(members));members.forEach(p->memberToGroup.put(p,group));
    }
    public Set<UUID> members(UUID piece){UUID group=memberToGroup.get(piece);return group==null?Set.of(piece):groups.get(group);}
    public Optional<UUID> groupOf(UUID piece){return Optional.ofNullable(memberToGroup.get(piece));}
    public Set<UUID> closure(Set<UUID> touched){var result=new HashSet<UUID>();for(UUID id:touched)result.addAll(members(id));return Set.copyOf(result);}
    public void requireComplete(Set<UUID> changed){if(!closure(changed).equals(changed))throw new IllegalArgumentException("Incomplete group operation");}
    public void remove(UUID group){var removed=groups.remove(group);if(removed!=null)removed.forEach(memberToGroup::remove);}
}
