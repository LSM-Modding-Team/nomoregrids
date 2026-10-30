package net.nicomar.nomoregrids.simulation.redstone;

import java.util.*;

/** Server-tick circuit, explicit port links only. Never infers an edge from a shared cell. */
public final class SignalNetwork {
    public enum Kind { SOURCE,DUST,REPEATER,COMPARATOR }
    public enum Input { REAR,SIDE }
    public record Node(UUID id,Kind kind,int delayTicks,boolean subtract){
        public Node{if(kind==Kind.REPEATER&&(delayTicks<2||delayTicks>8||delayTicks%2!=0)||kind==Kind.COMPARATOR&&delayTicks!=2)throw new IllegalArgumentException("Native redstone delay required");}
    }
    /** qLength follows actual wire path; contact has length zero and no extra attenuation. */
    public record Edge(UUID from,UUID to,Input input,int qLength){public Edge{if(qLength<0||qLength>4096)throw new IllegalArgumentException("Wire length");}}
    private record Scheduled(long tick,UUID id,long generation,int value){}
    private final Map<UUID,Node> nodes;
    private final List<Edge> edges;
    private final Map<UUID,Integer> values=new HashMap<>(),sources=new HashMap<>();
    private final Map<UUID,Long> generations=new HashMap<>();
    private final Map<UUID,Integer> requested=new HashMap<>();
    private final PriorityQueue<Scheduled> queue=new PriorityQueue<>(Comparator.comparingLong(Scheduled::tick).thenComparing(Scheduled::id).thenComparingLong(Scheduled::generation));
    private long now;
    public SignalNetwork(Collection<Node> nodes,Collection<Edge> edges){
        var map=new HashMap<UUID,Node>();for(var n:nodes)if(map.putIfAbsent(n.id,n)!=null)throw new IllegalArgumentException("Duplicate circuit node");
        this.nodes=Map.copyOf(map);this.edges=List.copyOf(edges);
        for(var e:edges)if(!map.containsKey(e.from)||!map.containsKey(e.to))throw new IllegalArgumentException("Missing port node");
    }
    public int value(UUID id){return values.getOrDefault(id,0);}
    public void source(UUID id,int value){if(value<0||value>15||nodes.get(id)==null||nodes.get(id).kind!=Kind.SOURCE)throw new IllegalArgumentException("Signal source");sources.put(id,value);schedule(id);}
    private void schedule(UUID id){
        Node n=nodes.get(id);int rear=0,side=0;
        for(var e:edges)if(e.to.equals(id)){
            int strength=value(e.from);if(n.kind==Kind.DUST)strength=Math.max(0,strength-(e.qLength+15)/16);
            if(e.input==Input.REAR)rear=Math.max(rear,strength);else side=Math.max(side,strength);
        }
        int desired=switch(n.kind){case SOURCE->sources.getOrDefault(id,0);case DUST->Math.max(rear,side);case REPEATER->rear>0?15:0;case COMPARATOR->n.subtract?Math.max(0,rear-side):rear>=side?rear:0;};
        if(requested.getOrDefault(id,-1)==desired)return;requested.put(id,desired);
        long generation=generations.merge(id,1L,Long::sum);queue.add(new Scheduled(Math.addExact(now,n.kind==Kind.REPEATER||n.kind==Kind.COMPARATOR?n.delayTicks:0),id,generation,desired));
    }
    public int advance(long tick,int budget){
        if(tick<now||budget<1)throw new IllegalArgumentException("Circuit clock/budget");now=tick;int processed=0;
        while(processed<budget&&!queue.isEmpty()&&queue.peek().tick<=tick){
            Scheduled s=queue.remove();processed++;if(generations.getOrDefault(s.id,0L)!=s.generation)continue;
            if(value(s.id)==s.value)continue;values.put(s.id,s.value);
            for(var e:edges)if(e.from.equals(s.id))schedule(e.to);
        }return processed;
    }
    public int pending(){return queue.size();}
}
