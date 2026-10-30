package net.nicomar.nomoregrids.simulation.redstone;

import java.util.*;

/** Server-tick circuit, explicit port links only. Never infers an edge from a shared cell. */
public final class SignalNetwork {
    public enum Kind { SOURCE,DUST,REPEATER,COMPARATOR }
    public enum Input { REAR,SIDE }
    public record Node(UUID id,Kind kind,int delayTicks,boolean subtract){
        public Node{Objects.requireNonNull(id);Objects.requireNonNull(kind);if(kind==Kind.REPEATER&&(delayTicks<2||delayTicks>8||delayTicks%2!=0)||kind==Kind.COMPARATOR&&delayTicks!=2||(kind==Kind.SOURCE||kind==Kind.DUST)&&delayTicks!=0)throw new IllegalArgumentException("Native redstone delay required");}
    }
    /** qLength follows actual wire path; contact has length zero and no extra attenuation. */
    public record Edge(UUID from,UUID to,Input input,int qLength){public Edge{Objects.requireNonNull(from);Objects.requireNonNull(to);Objects.requireNonNull(input);if(qLength<0||qLength>4096)throw new IllegalArgumentException("Wire length");}}
    private record Scheduled(long tick,UUID id,int value){}
    private final Map<UUID,Node> nodes;
    private final Map<UUID,List<Edge>> incoming=new HashMap<>(),outgoing=new HashMap<>();
    private final Map<UUID,Integer> values=new HashMap<>(),sources=new HashMap<>();
    private final Map<UUID,Scheduled> pending=new HashMap<>();
    private final NavigableSet<Scheduled> queue=new TreeSet<>(Comparator.comparingLong(Scheduled::tick).thenComparing(Scheduled::id));
    private long now;
    public SignalNetwork(Collection<Node> nodes,Collection<Edge> edges){
        if(nodes.size()>65536||edges.size()>262144)throw new IllegalArgumentException("Circuit budget");
        var map=new HashMap<UUID,Node>();for(var n:nodes)if(map.putIfAbsent(n.id,n)!=null)throw new IllegalArgumentException("Duplicate circuit node");
        this.nodes=Map.copyOf(map);
        for(var e:edges){if(!map.containsKey(e.from)||!map.containsKey(e.to))throw new IllegalArgumentException("Missing port node");incoming.computeIfAbsent(e.to,key->new ArrayList<>()).add(e);outgoing.computeIfAbsent(e.from,key->new ArrayList<>()).add(e);}
    }
    public int value(UUID id){return values.getOrDefault(id,0);}
    public void source(UUID id,int value){if(value<0||value>15||nodes.get(id)==null||nodes.get(id).kind!=Kind.SOURCE)throw new IllegalArgumentException("Signal source");sources.put(id,value);schedule(id,now);}
    private void schedule(UUID id,long logicalTick){
        Node n=nodes.get(id);int rear=0,side=0;
        for(var e:incoming.getOrDefault(id,List.of())){
            int strength=value(e.from);if(n.kind==Kind.DUST)strength=Math.max(0,strength-(e.qLength+15)/16);
            if(e.input==Input.REAR)rear=Math.max(rear,strength);else side=Math.max(side,strength);
        }
        int desired=switch(n.kind){case SOURCE->sources.getOrDefault(id,0);case DUST->Math.max(rear,side);case REPEATER->rear>0?15:0;case COMPARATOR->n.subtract?Math.max(0,rear-side):rear>=side?rear:0;};
        Scheduled old=pending.get(id);if(old!=null&&old.value==desired)return;
        if(old!=null){queue.remove(old);pending.remove(id);}
        if(value(id)==desired)return;
        var next=new Scheduled(Math.addExact(logicalTick,n.delayTicks),id,desired);pending.put(id,next);queue.add(next);
    }
    public int advance(long tick,int budget){
        if(tick<now||budget<1)throw new IllegalArgumentException("Circuit clock/budget");now=tick;int processed=0;
        while(processed<budget&&!queue.isEmpty()&&queue.first().tick<=tick){
            Scheduled s=queue.pollFirst();pending.remove(s.id);processed++;values.put(s.id,s.value);
            // A deferred due event propagates from its logical time, not the later frame that drains the queue.
            for(var e:outgoing.getOrDefault(s.id,List.of()))schedule(e.to,s.tick);
        }return processed;
    }
    public int pending(){return pending.size();}
}
