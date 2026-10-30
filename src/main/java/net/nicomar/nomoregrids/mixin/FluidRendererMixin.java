package net.nicomar.nomoregrids.mixin;

import net.minecraft.client.renderer.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.nicomar.nomoregrids.world.CompositeCellBlock;

/** The cell's cavity mesh owns fluid surfaces; suppress the full vanilla cell mesh for this block only. */
@Mixin(FluidRenderer.class)
public abstract class FluidRendererMixin {
    @Inject(method="tesselate",at=@At("HEAD"),cancellable=true)
    private void nmgCavity(BlockAndTintGetter world,BlockPos pos,FluidRenderer.Output output,BlockState state,FluidState fluid,CallbackInfo callback){if(state.getBlock() instanceof CompositeCellBlock)callback.cancel();}
}
