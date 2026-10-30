package net.nicomar.nomoregrids.event;

import net.neoforged.bus.api.*;
import net.minecraft.server.level.ServerLevel;
import net.nicomar.nomoregrids.operation.OperationPlan;

/** One cancellable plan with every affected cell; protection adapters must check the whole footprint. */
public final class BeforeOperationEvent extends Event implements ICancellableEvent {
    private final ServerLevel level;private final OperationPlan plan;
    public BeforeOperationEvent(ServerLevel level,OperationPlan plan){this.level=level;this.plan=plan;}
    public ServerLevel level(){return level;}public OperationPlan plan(){return plan;}
}
