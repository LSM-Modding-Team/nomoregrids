package net.nicomar.nomoregrids.event;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;
import net.nicomar.nomoregrids.operation.OperationPlan;

/** Informational, non-cancellable notification of an already durable operation. */
public final class AfterOperationEvent extends Event {
    private final ServerLevel level;private final OperationPlan plan;
    public AfterOperationEvent(ServerLevel level,OperationPlan plan){this.level=level;this.plan=plan;}
    public ServerLevel level(){return level;}public OperationPlan plan(){return plan;}
}
