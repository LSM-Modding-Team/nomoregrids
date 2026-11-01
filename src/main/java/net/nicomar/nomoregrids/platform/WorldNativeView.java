package net.nicomar.nomoregrids.platform;

import java.io.IOException;
import java.util.*;
import net.minecraft.world.level.block.state.BlockState;
import net.nicomar.nomoregrids.core.CellPos;
import net.nicomar.nomoregrids.operation.OperationPlan;
import net.nicomar.nomoregrids.persistence.NativeStateImage;

/** Proposed native images for planning support. Does not intercept or mutate Minecraft queries. */
public final class WorldNativeView implements AutoCloseable {
    private static final ThreadLocal<Map<CellPos,BlockState>> CURRENT=new ThreadLocal<>();
    private final Map<CellPos,BlockState> previous;
    public WorldNativeView(List<OperationPlan.Effect> effects){previous=CURRENT.get();var values=new HashMap<CellPos,BlockState>();for(var e:effects)if(e.key().startsWith("native:")&&e.after().isPresent())try{var image=NativeStateImage.decode(e.after().orElseThrow());values.put(e.owner(),NativeStates.resolve(image.block(),image.properties()));}catch(IOException ex){throw new IllegalArgumentException("INVALID_DATA: native plan",ex);}CURRENT.set(values);}
    public static Optional<BlockState> at(CellPos c){return Optional.ofNullable(CURRENT.get()).map(m->m.get(c));}
    public void close(){if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
}
