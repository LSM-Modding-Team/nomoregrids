package net.nicomar.nomoregrids.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import net.nicomar.nomoregrids.world.*;

public final class NmgContent {
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("nomoregrids");
    public static final DeferredBlock<CompositeCellBlock> CELL=BLOCKS.registerBlock("composite_cell",CompositeCellBlock::new,
        p->p.dynamicShape().noOcclusion().strength(-1,3600000).noLootTable());
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"nomoregrids");
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<CompositeCellEntity>> CELL_ENTITY=ENTITIES.register("composite_cell",()->new BlockEntityType<>(CompositeCellEntity::new,java.util.Set.of(CELL.get())));
    private NmgContent(){}
    public static void register(IEventBus bus){BLOCKS.register(bus);ENTITIES.register(bus);}
}
