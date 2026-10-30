package net.nicomar.nomoregrids.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import net.nicomar.nomoregrids.world.*;

public final class NmgContent {
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("nomoregrids");
    public static final DeferredItem<net.nicomar.nomoregrids.material.FragmentItem> FRAGMENT=ITEMS.registerItem("fragment",net.nicomar.nomoregrids.material.FragmentItem::new);
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPES=DeferredRegister.create(Registries.RECIPE_SERIALIZER,"nomoregrids");
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>,net.minecraft.world.item.crafting.RecipeSerializer<net.nicomar.nomoregrids.material.FragmentRecipe>> FRAGMENT_JOIN=RECIPES.register("fragment_join",()->recipe(true));
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>,net.minecraft.world.item.crafting.RecipeSerializer<net.nicomar.nomoregrids.material.FragmentRecipe>> FRAGMENT_HALVES=RECIPES.register("fragment_halves",()->recipe(false));
    private static net.minecraft.world.item.crafting.RecipeSerializer<net.nicomar.nomoregrids.material.FragmentRecipe> recipe(boolean join){var recipe=new net.nicomar.nomoregrids.material.FragmentRecipe(join);return new net.minecraft.world.item.crafting.RecipeSerializer<>(com.mojang.serialization.MapCodec.unit(recipe),net.minecraft.network.codec.StreamCodec.unit(recipe));}
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("nomoregrids");
    public static final DeferredBlock<CompositeCellBlock> CELL=BLOCKS.registerBlock("composite_cell",CompositeCellBlock::new,
        p->p.dynamicShape().noOcclusion().lightLevel(state->state.getValue(CompositeCellBlock.EMISSION)).strength(-1,3600000).noLootTable().pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"nomoregrids");
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<CompositeCellEntity>> CELL_ENTITY=ENTITIES.register("composite_cell",()->new BlockEntityType<>(CompositeCellEntity::new,java.util.Set.of(CELL.get())));
    private NmgContent(){}
    public static void register(IEventBus bus){BLOCKS.register(bus);ENTITIES.register(bus);ITEMS.register(bus);RECIPES.register(bus);}
}
