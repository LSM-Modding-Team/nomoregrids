package net.nicomar.nomoregrids.platform;

import java.io.IOException;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.nicomar.nomoregrids.core.CellPos;

/** Isolated Level#setBlock hook: release native projections before external writes; coalesce topology afterward. */
public final class WorldMutationHooks {
    private static final Map<ServerLevel,LinkedHashSet<CellPos>> DIRTY=new IdentityHashMap<>();
    private static final Map<ServerLevel,Iterator<CellPos>> SCANS=new IdentityHashMap<>();
    private WorldMutationHooks(){}
    public static boolean before(ServerLevel level,BlockPos pos,BlockState state){
        var service=NmgRuntime.existing(level);if(service==null||service.writing()||level.getBlockState(pos).equals(state))return true;
        var cell=new CellPos(pos.getX(),pos.getY(),pos.getZ());
        if(service.nativeGoal(cell).isPresent())try{service.releaseNativeGoal(cell);}catch(IOException|RuntimeException ex){NmgRuntime.failed(level,ex);return false;}
        if(level.getBlockState(pos).getBlock() instanceof net.nicomar.nomoregrids.world.CompositeCellBlock&&!state.getFluidState().isEmpty()){
            try{service.fluids().incoming(cell,state.getFluidState());}catch(IOException|RuntimeException ex){if(service.quarantined())NmgRuntime.failed(level,ex);}return false;
        }
        if(service.snapshot().at(cell).isEmpty()&&service.fluids().at(cell).isEmpty())return true;
        if(service.quarantined())return false;
        if(service.nativeProjection(cell).isEmpty())return false; // External edits cannot erase a composite authority.
        try{service.releaseNative(cell);return true;}catch(IOException|RuntimeException ex){NmgRuntime.failed(level,ex);return false;}
    }
    public static void changed(ServerLevel level,BlockPos pos){
        var service=NmgRuntime.existing(level);if(service==null||service.writing())return;
        service.fluids().changed(new CellPos(pos.getX(),pos.getY(),pos.getZ()));
        var dirty=DIRTY.computeIfAbsent(level,k->new LinkedHashSet<>());if(dirty.size()<4096)dirty.add(new CellPos(pos.getX(),pos.getY(),pos.getZ()));
        else SCANS.putIfAbsent(level,service.snapshot().records().values().stream().flatMap(p->p.worldOccupation().cells(8).stream()).distinct().iterator());
    }
    public static void tick(ServerLevel level){
        var service=NmgRuntime.existing(level);if(service==null||service.quarantined())return;long start=System.nanoTime();var dirty=DIRTY.computeIfAbsent(level,k->new LinkedHashSet<>());var scan=SCANS.get(level);
        if(scan!=null){for(int i=0;i<16&&scan.hasNext();i++)dirty.add(scan.next());if(!scan.hasNext())SCANS.remove(level);}
        int count=0;for(var cell:List.copyOf(dirty)){
            if(++count>16||System.nanoTime()-start>=2_000_000)break;dirty.remove(cell);if(!level.isLoaded(WorldService.pos(cell)))continue;
            try{service.externalChanged(Set.of(cell));}catch(IOException|RuntimeException ex){
                if(service.quarantined()){NmgRuntime.failed(level,ex);return;}
                if(Objects.toString(ex.getMessage(),"").contains("UNLOADED_REGION"))continue;
                net.nicomar.nomoregrids.NoMoreGrids.LOGGER.debug("NMG neighbor update deferred at {}: {}",cell,ex.toString());
            }
        }
    }
    public static void clear(ServerLevel level){DIRTY.remove(level);SCANS.remove(level);}
}
