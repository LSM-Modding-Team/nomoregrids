package net.nicomar.nomoregrids.world;

import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Sparse natural-load directory used for queries whose bounding volume exceeds a local cell scan. */
public final class LoadedCells {
    private static final Map<Level,Map<BlockPos,WeakReference<CompositeCellEntity>>> LEVELS=new WeakHashMap<>();
    private LoadedCells(){}
    public static synchronized void add(CompositeCellEntity cell){if(cell.getLevel()!=null)LEVELS.computeIfAbsent(cell.getLevel(),l->new HashMap<>()).put(cell.getBlockPos().immutable(),new WeakReference<>(cell));}
    public static synchronized void remove(CompositeCellEntity cell){var map=LEVELS.get(cell.getLevel());if(map!=null){var reference=map.get(cell.getBlockPos());if(reference!=null&&reference.get()==cell)map.remove(cell.getBlockPos());if(map.isEmpty())LEVELS.remove(cell.getLevel());}}
    public static Iterable<BlockPos> intersecting(Level level,AABB box){
        double count=(Math.ceil(box.maxX)-Math.floor(box.minX))*(Math.ceil(box.maxY)-Math.floor(box.minY))*(Math.ceil(box.maxZ)-Math.floor(box.minZ));
        if(count<=512)return BlockPos.betweenClosed((int)Math.floor(box.minX),(int)Math.floor(box.minY),(int)Math.floor(box.minZ),(int)Math.ceil(box.maxX)-1,(int)Math.ceil(box.maxY)-1,(int)Math.ceil(box.maxZ)-1);
        synchronized(LoadedCells.class){var positions=new ArrayList<BlockPos>();var map=LEVELS.get(level);if(map!=null){map.entrySet().removeIf(e->e.getValue().get()==null);for(var e:map.entrySet()){var p=e.getKey();if(p.getX()<box.maxX&&p.getX()+1>box.minX&&p.getY()<box.maxY&&p.getY()+1>box.minY&&p.getZ()<box.maxZ&&p.getZ()+1>box.minZ)positions.add(p);}}return positions;}
    }
}
