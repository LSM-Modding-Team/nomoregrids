package net.nicomar.nomoregrids.simulation.fluid;

import java.util.*;
import net.nicomar.nomoregrids.core.*;

/** Bounded spatial relaxation with persisted boundary distances; positive horizontal cost prevents wet cycles after source removal. */
public final class CavityRelaxation {
    private final Map<CellPos,CavityCell> boundary;private Map<CellPos,CavityCell> work;private final List<CellPos> targets;private final CavityCell.Kind kind;private final int reach;
    private int pass,cellIndex,micro;private final Map<CellPos,byte[]> nextFluids=new HashMap<>(),nextDistance=new HashMap<>();
    public CavityRelaxation(Map<CellPos,CavityCell> input,Set<CellPos> targets,CavityCell.Kind kind){
        if(kind==CavityCell.Kind.EMPTY||targets.size()>32||!input.keySet().containsAll(targets))throw new IllegalArgumentException("Fluid relaxation region");boundary=Map.copyOf(input);work=boundary;this.targets=targets.stream().sorted().toList();this.kind=kind;reach=kind==CavityCell.Kind.WATER?112:48;beginPass();
    }
    private void beginPass(){nextFluids.clear();nextDistance.clear();for(var c:targets){nextFluids.put(c,work.get(c).fluids());nextDistance.put(c,work.get(c).distances());}cellIndex=0;micro=0;}
    private int from(CellPos c,int x,int y,int z){var p=new CellPos(c.x()+Math.floorDiv(x,16),c.y()+Math.floorDiv(y,16),c.z()+Math.floorDiv(z,16));var state=work.get(p);if(state==null)return 127;int i=Math.floorMod(x,16)+16*(Math.floorMod(y,16)+16*Math.floorMod(z,16));return state.at(i)==kind?state.distance(i):127;}
    public boolean advance(int budget){if(budget<1)throw new IllegalArgumentException("Fluid work budget");while(budget-->0&&pass<16){if(targets.isEmpty()){pass=16;break;}var c=targets.get(cellIndex);var state=work.get(c);int i=micro,x=i%16,y=i/16%16,z=i/256;int best=127;
            if(state.free(i)&&(state.at(i)==kind||state.at(i)==CavityCell.Kind.EMPTY)){
                if(state.source()==kind)best=0;else{best=from(c,x,y+1,z);for(var d:List.of(Direction6.NORTH,Direction6.SOUTH,Direction6.WEST,Direction6.EAST))best=Math.min(best,from(c,x+d.x,y,z+d.z)+1);}
                if(best>reach)best=127;nextFluids.get(c)[i]=best==127?0:CavityCell.code(kind);nextDistance.get(c)[i]=(byte)best;
            }
            if(++micro==4096){micro=0;if(++cellIndex==targets.size()){var updated=new HashMap<>(work);for(var p:targets){var old=work.get(p);updated.put(p,new CavityCell(old.blocked(),nextFluids.get(p),old.source(),nextDistance.get(p)));}work=Map.copyOf(updated);pass++;if(pass<16)beginPass();}}
        }return pass==16;}
    public Map<CellPos,CavityCell> result(){if(pass<16)throw new IllegalStateException("Fluid work pending");var result=new TreeMap<CellPos,CavityCell>();targets.forEach(c->result.put(c,work.get(c)));return Map.copyOf(result);}
}
