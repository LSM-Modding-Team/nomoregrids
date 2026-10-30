package net.nicomar.nomoregrids.event;

import net.neoforged.bus.api.*;
import net.minecraft.server.level.ServerLevel;
import net.nicomar.nomoregrids.operation.OperationPlan;

/** One cancellable plan with every affected cell; protection adapters must check the whole footprint. */
public final class BeforeOperationEvent extends Event implements ICancellableEvent {
    private final ServerLevel level;private final OperationPlan plan;private final boolean preview;
    public BeforeOperationEvent(ServerLevel level,OperationPlan plan){this(level,plan,false);}
    public BeforeOperationEvent(ServerLevel level,OperationPlan plan,boolean preview){this.level=level;this.plan=plan;this.preview=preview;}
    /** Preview observers must only validate; no charges, loot, break callbacks or world mutations. */
    public boolean preview(){return preview;}
    public ServerLevel level(){return level;}public OperationPlan plan(){return plan;}
}
