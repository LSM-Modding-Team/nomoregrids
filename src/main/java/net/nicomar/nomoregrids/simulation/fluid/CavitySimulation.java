package net.nicomar.nomoregrids.simulation.fluid;

import java.util.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.simulation.fluid.CavityCell.Kind;

/** Loaded-region simulation. Missing cells are closed; no world/chunk loading occurs. */
public final class CavitySimulation {
    private static final List<Direction6> FLOW=List.of(Direction6.DOWN,Direction6.NORTH,Direction6.SOUTH,Direction6.WEST,Direction6.EAST);
    private record Step(GridPoint point,Kind kind,int horizontal){}
    private CavitySimulation(){}
    private static int local(GridPoint point){GridPoint p=point.offsetInCell();return (int)(p.x()+16*(p.y()+16*p.z()));}
    private static GridPoint point(CellPos cell,int index){return new GridPoint(Math.addExact(Math.multiplyExact(cell.x(),16),index%16),
        Math.addExact(Math.multiplyExact(cell.y(),16),index/16%16),Math.addExact(Math.multiplyExact(cell.z(),16),index/256));}

    /** Incremental flood recomputation. Publish only when complete, so a spent budget never drains fluid. */
    public static final class FlowJob {
        private final Map<CellPos,CavityCell> previous;
        private final Map<CellPos,byte[]> desired=new HashMap<>();
        private final Map<GridPoint,Integer> visited=new HashMap<>();
        private final ArrayDeque<Step> queue=new ArrayDeque<>();
        private boolean complete;
        public FlowJob(Map<CellPos,CavityCell> loaded){this(loaded,Set.of(Kind.WATER,Kind.LAVA));}
        public FlowJob(Map<CellPos,CavityCell> loaded,Set<Kind> active){
            previous=Map.copyOf(loaded);
            var active=new HashMap<SectionPos,Integer>();for(var pos:loaded.keySet())if(active.merge(pos.section(),1,Integer::sum)>256)throw new IllegalArgumentException("Fluid section budget");
            for(var cell:loaded.keySet().stream().sorted().toList()){
                CavityCell state=loaded.get(cell);byte[] retained=state.fluids();for(int i=0;i<4096;i++)if(active.contains(state.at(i)))retained[i]=0;desired.put(cell,retained);
                if(active.contains(state.source()))for(int i=0;i<4096;i++)queue.add(new Step(point(cell,i),state.source(),0));
            }
        }
        public boolean advance(int budget){
            if(budget<1||budget>8192)throw new IllegalArgumentException("Cavity step budget");
            for(int steps=0;steps<budget&&!queue.isEmpty();steps++){
                Step step=queue.removeFirst();CavityCell cell=previous.get(step.point.cell());if(cell==null||!cell.free(local(step.point)))continue;
                Integer old=visited.get(step.point);if(old!=null&&old<=step.horizontal)continue;
                byte[] output=desired.get(step.point.cell());int index=local(step.point);
                if(cell.at(index)!=Kind.EMPTY&&cell.at(index)!=step.kind)continue;
                if(output[index]!=0&&output[index]!=CavityCell.code(step.kind))continue; // reaction planner owns mixed contacts
                visited.put(step.point,step.horizontal);output[index]=CavityCell.code(step.kind);
                int reach=step.kind==Kind.WATER?7*16:3*16;
                for(var direction:FLOW){int distance=direction==Direction6.DOWN?step.horizontal:step.horizontal+1;
                    if(distance<=reach)queue.addLast(new Step(step.point.add(direction.vector()),step.kind,distance));}
            }
            complete=queue.isEmpty();return complete;
        }
        public Map<CellPos,CavityCell> result(){
            if(!complete)throw new IllegalStateException("Fluid work pending");
            var next=new HashMap<CellPos,CavityCell>();
            previous.forEach((p,c)->next.put(p,new CavityCell(c.blocked(),desired.get(p),c.source())));return Map.copyOf(next);
        }
    }

    /** Evacuate displaced microvolumes through previously open cavities within four loaded cells. */
    public static Map<CellPos,CavityCell> displace(Map<CellPos,CavityCell> old,Map<CellPos,BitSet> newMasks,int budget){
        if(!old.keySet().equals(newMasks.keySet())||budget<1)throw new IllegalArgumentException("Region/mask mismatch");
        var next=new HashMap<CellPos,byte[]>();old.forEach((p,c)->next.put(p,c.fluids()));
        int steps=0;var positions=old.keySet().stream().sorted().toList();
        for(var pos:positions){CavityCell state=old.get(pos);BitSet mask=newMasks.get(pos);
            if(mask.length()>4096)throw new IllegalArgumentException("Invalid mask");
            if(state.source()!=Kind.EMPTY&&!mask.isEmpty())throw new IllegalArgumentException("Cannot obstruct a complete source; relocate source first");
            for(int i=mask.nextSetBit(0);i>=0;i=mask.nextSetBit(i+1))if(next.get(pos)[i]!=0){
                byte kind=next.get(pos)[i];next.get(pos)[i]=0;GridPoint origin=point(pos,i);
                var queue=new ArrayDeque<GridPoint>();var seen=new HashSet<GridPoint>();queue.add(origin);boolean moved=false;
                while(!queue.isEmpty()){
                    if(++steps>budget)throw new IllegalArgumentException("Fluid redistribution budget exceeded");
                    GridPoint p=queue.removeFirst();if(!seen.add(p))continue;CellPos cell=p.cell();CavityCell previous=old.get(cell);
                    if(previous==null||Math.abs(cell.x()-pos.x())>4||Math.abs(cell.y()-pos.y())>4||Math.abs(cell.z()-pos.z())>4||!previous.free(local(p)))continue;
                    int at=local(p);byte fluid=next.get(cell)[at];if(fluid!=0&&fluid!=kind)continue;
                    if(!newMasks.get(cell).get(at)&&fluid==0){next.get(cell)[at]=kind;moved=true;break;}
                    for(var d:Direction6.values())queue.addLast(p.add(d.vector()));
                }
                if(!moved)throw new IllegalArgumentException("No connected space for displaced fluid");
            }
        }
        var result=new HashMap<CellPos,CavityCell>();for(var pos:positions)result.put(pos,new CavityCell(newMasks.get(pos),next.get(pos),old.get(pos).source()));
        return Map.copyOf(result);
    }

    public record Reaction(Map<CellPos,CavityCell> fluids,Map<CellPos,ResourceId> products){public Reaction{fluids=Map.copyOf(fluids);products=Map.copyOf(products);}}
    /** Full-cell products only. An unrepresentable partial reaction conserves both fluids. */
    public static Reaction react(Map<CellPos,CavityCell> cells){
        var result=new HashMap<>(cells);var products=new HashMap<CellPos,ResourceId>();
        for(var pos:cells.keySet().stream().sorted().toList()){
            CavityCell lava=cells.get(pos);if(!lava.blocked().isEmpty()||lava.amount(Kind.LAVA)!=4096)continue;
            boolean water=false;
            for(var d:Direction6.values()){
                CavityCell neighbor=cells.get(new CellPos(pos.x()+d.x,pos.y()+d.y,pos.z()+d.z));if(neighbor==null)continue;
                for(int i=0;i<4096&&!water;i++){
                    int x=i%16,y=i/16%16,z=i/256;
                    boolean face=d.x!=0?x==(d.x>0?0:15):d.y!=0?y==(d.y>0?0:15):z==(d.z>0?0:15);
                    if(face&&neighbor.at(i)==Kind.WATER)water=true;
                }
            }
            if(water){products.put(pos,new ResourceId(lava.source()==Kind.LAVA?"minecraft:obsidian":"minecraft:cobblestone"));
                BitSet solid=new BitSet(4096);solid.set(0,4096);result.put(pos,new CavityCell(solid,new byte[4096],Kind.EMPTY));}
        }
        return new Reaction(result,products);
    }
}
