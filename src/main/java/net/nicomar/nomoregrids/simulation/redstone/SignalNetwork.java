package net.nicomar.nomoregrids.simulation.redstone;

import java.util.*;

/** Server-tick circuit, explicit port links only. Never infers an edge from a shared cell. */
public final class SignalNetwork {
    public static final int MAX_PENDING=262144;
    public enum Kind { SOURCE,DUST,REPEATER,COMPARATOR,TORCH }
    public enum Input { REAR,SIDE }
    public record Node(UUID id,Kind kind,int delayTicks,boolean subtract){
        public Node{Objects.requireNonNull(id);Objects.requireNonNull(kind);if(kind==Kind.REPEATER&&(delayTicks<2||delayTicks>8||delayTicks%2!=0)||(kind==Kind.COMPARATOR||kind==Kind.TORCH)&&delayTicks!=2||(kind==Kind.SOURCE||kind==Kind.DUST)&&delayTicks!=0)throw new IllegalArgumentException("Native redstone delay required");}
    }
    /** qLength follows actual wire path; contact has length zero and no extra attenuation. */
    public record Edge(UUID from,UUID to,Input input,int qLength){public Edge{Objects.requireNonNull(from);Objects.requireNonNull(to);Objects.requireNonNull(input);if(qLength<0||qLength>4096)throw new IllegalArgumentException("Wire length");}}
    private record Scheduled(long tick,UUID id,int value){}
    public record Pending(long tick,UUID id,int value){public Pending{Objects.requireNonNull(id);if(tick<0||value<0||value>15)throw new IllegalArgumentException("Pending signal");}}
    public record Snapshot(long tick,List<Node> nodes,List<Edge> edges,Map<UUID,Integer> values,Map<UUID,Integer> sources,List<Pending> pending){
        public Snapshot{if(tick<0)throw new IllegalArgumentException("Circuit tick");nodes=List.copyOf(nodes);edges=List.copyOf(edges);values=Map.copyOf(values);sources=Map.copyOf(sources);pending=List.copyOf(pending);}
    }
    private final Map<UUID,Node> nodes;
    private final List<Edge> edges;
    private final Map<UUID,List<Edge>> incoming=new HashMap<>(),outgoing=new HashMap<>();
    private final Map<UUID,Integer> values=new HashMap<>(),sources=new HashMap<>();
    private final Map<UUID,Scheduled> pending=new HashMap<>();
    private final Map<UUID,NavigableMap<Long,Scheduled>> sourcePending=new HashMap<>();
    private int sourceEvents;
    private final NavigableSet<Scheduled> queue=new TreeSet<>(Comparator.comparingLong(Scheduled::tick).thenComparing(Scheduled::id));
    private long now;
    public SignalNetwork(Collection<Node> nodes,Collection<Edge> edges){
        if(nodes.size()>65536||edges.size()>262144)throw new IllegalArgumentException("Circuit budget");
        var map=new HashMap<UUID,Node>();for(var n:nodes)if(map.putIfAbsent(n.id,n)!=null)throw new IllegalArgumentException("Duplicate circuit node");
        this.nodes=Map.copyOf(map);this.edges=List.copyOf(edges);
        for(var e:edges){if(!map.containsKey(e.from)||!map.containsKey(e.to)||map.get(e.to).kind==Kind.SOURCE)throw new IllegalArgumentException("Invalid circuit edge or source input");incoming.computeIfAbsent(e.to,key->new ArrayList<>()).add(e);outgoing.computeIfAbsent(e.from,key->new ArrayList<>()).add(e);}
    }
    public int value(UUID id){return values.getOrDefault(id,0);}
    public void source(UUID id,int value){
        if(value<0||value>15||nodes.get(id)==null||nodes.get(id).kind!=Kind.SOURCE)throw new IllegalArgumentException("Signal source");
        var timeline=sourcePending.get(id);Scheduled atTick=timeline==null?null:timeline.get(now);var before=timeline==null?null:timeline.lowerEntry(now);int baseline=before==null?value(id):before.getValue().value;
        int delta=(value==baseline?0:1)-(atTick==null?0:1);if(sourceEvents+delta>MAX_PENDING-nodes.size())throw new IllegalStateException("Circuit input backlog; defer the input without dropping its logical tick");
        if(atTick!=null){queue.remove(atTick);timeline.remove(now);sourceEvents--;}
        sources.put(id,value);if(value!=baseline){var event=new Scheduled(now,id,value);sourcePending.computeIfAbsent(id,key->new TreeMap<>()).put(now,event);queue.add(event);sourceEvents++;}
        if(timeline!=null&&timeline.isEmpty())sourcePending.remove(id);
    }
    private void schedule(UUID id,long logicalTick){
        Node n=nodes.get(id);int rear=0,side=0;
        for(var e:incoming.getOrDefault(id,List.of())){
            int strength=value(e.from);if(n.kind==Kind.DUST)strength=Math.max(0,strength-(e.qLength+15)/16);
            if(e.input==Input.REAR)rear=Math.max(rear,strength);else side=Math.max(side,strength);
        }
        int desired=switch(n.kind){case SOURCE->sources.getOrDefault(id,0);case DUST->Math.max(rear,side);case REPEATER->side>0?value(id):rear>0?15:0;case COMPARATOR->n.subtract?Math.max(0,rear-side):rear>=side?rear:0;case TORCH->rear==0?15:0;};
        Scheduled old=pending.get(id);if(old!=null&&old.value==desired)return;
        if(old!=null){queue.remove(old);pending.remove(id);}
        if(value(id)==desired)return;
        var next=new Scheduled(Math.addExact(logicalTick,n.delayTicks),id,desired);pending.put(id,next);queue.add(next);
    }
    public void refresh(){for(var n:nodes.values())if(n.kind!=Kind.SOURCE)schedule(n.id,now);}
    public int advance(long tick,int budget){
        if(tick<now||budget<1)throw new IllegalArgumentException("Circuit clock/budget");now=tick;int processed=0;
        while(processed<budget&&!queue.isEmpty()&&queue.first().tick<=tick){
            Scheduled s=queue.pollFirst();if(nodes.get(s.id).kind==Kind.SOURCE){var timeline=sourcePending.get(s.id);timeline.remove(s.tick);sourceEvents--;if(timeline.isEmpty())sourcePending.remove(s.id);}else pending.remove(s.id);processed++;values.put(s.id,s.value);
            // A deferred due event propagates from its logical time, not the later frame that drains the queue.
            for(var e:outgoing.getOrDefault(s.id,List.of()))schedule(e.to,s.tick);
        }return processed;
    }
    public int pending(){return queue.size();}
    public Snapshot snapshot(){return new Snapshot(now,nodes.values().stream().sorted(Comparator.comparing(Node::id)).toList(),edges,values,sources,queue.stream().map(s->new Pending(s.tick,s.id,s.value)).toList());}
    public static SignalNetwork restored(Snapshot snapshot){
        var result=new SignalNetwork(snapshot.nodes,snapshot.edges);result.now=snapshot.tick;
        for(var e:snapshot.values.entrySet()){if(!result.nodes.containsKey(e.getKey())||e.getValue()<0||e.getValue()>15)throw new IllegalArgumentException("Invalid circuit value");result.values.put(e.getKey(),e.getValue());}
        for(var e:snapshot.sources.entrySet()){var node=result.nodes.get(e.getKey());if(node==null||node.kind!=Kind.SOURCE||e.getValue()<0||e.getValue()>15)throw new IllegalArgumentException("Invalid source value");result.sources.put(e.getKey(),e.getValue());}
        if(snapshot.pending.size()>MAX_PENDING)throw new IllegalArgumentException("Circuit pending budget");
        for(var event:snapshot.pending){
            if(!result.nodes.containsKey(event.id)||event.tick>snapshot.tick&&event.tick-snapshot.tick>8)throw new IllegalArgumentException("Invalid scheduled signal");var scheduled=new Scheduled(event.tick,event.id,event.value);
            if(result.nodes.get(event.id).kind==Kind.SOURCE){if(event.tick>snapshot.tick)throw new IllegalArgumentException("Future source input");if(result.sourcePending.computeIfAbsent(event.id,key->new TreeMap<>()).putIfAbsent(event.tick,scheduled)!=null)throw new IllegalArgumentException("Duplicate source input tick");result.sourceEvents++;}
            else if(result.pending.putIfAbsent(event.id,scheduled)!=null)throw new IllegalArgumentException("Duplicate pending signal");
            result.queue.add(scheduled);
        }
        if(result.sourceEvents>MAX_PENDING-result.nodes.size())throw new IllegalArgumentException("Circuit input backlog budget");
        result.sourcePending.forEach((id,timeline)->{if(timeline.lastEntry().getValue().value!=result.sources.getOrDefault(id,0))throw new IllegalArgumentException("Source input receipt conflicts with pending timeline");});
        return result;
    }
}
