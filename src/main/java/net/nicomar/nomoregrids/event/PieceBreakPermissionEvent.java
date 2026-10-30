package net.nicomar.nomoregrids.event;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/** Carries the source material and piece identity; does not request another container break. */
public final class PieceBreakPermissionEvent extends BreakBlockEvent {
    private final UUID piece;
    public PieceBreakPermissionEvent(ServerLevel level,BlockPos pos,BlockState source,ServerPlayer actor,UUID piece){super(level,pos,source,actor);this.piece=piece;}
    public UUID piece(){return piece;}
}
