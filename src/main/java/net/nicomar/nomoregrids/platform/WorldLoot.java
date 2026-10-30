package net.nicomar.nomoregrids.platform;

import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.material.FragmentItem;
import net.nicomar.nomoregrids.operation.OperationPlan;

/** Final loot results are journaled once. Nearby players claim through the same inventory receipt protocol. */
public final class WorldLoot {
    private WorldLoot(){}
    public static void queue(ServerLevel level,PieceRecord p,List<OperationPlan.Effect> effects){
        if(WorldGroups.piece(p)){var drops=WorldGroups.drops(p);if(!drops.isEmpty())effects.add(new OperationPlan.Effect("loot:"+UUID.randomUUID(),p.origin16().cell(),Optional.empty(),Optional.of(encode(level,drops))));return;}
        if(WorldDecorations.piece(p)){var drops=WorldDecorations.drops(p,level,null);if(!drops.isEmpty())effects.add(new OperationPlan.Effect("loot:"+UUID.randomUUID(),p.origin16().cell(),Optional.empty(),Optional.of(encode(level,drops))));return;}
        if(WorldFunctionals.piece(p)){var drops=NmgRuntime.existing(level).functionals().drops(p,null);if(!drops.isEmpty())effects.add(new OperationPlan.Effect("loot:"+UUID.randomUUID(),p.origin16().cell(),Optional.empty(),Optional.of(encode(level,drops))));return;}
        if(!p.adapterId().value().equals("nomoregrids:box")&&!WorldCircuits.piece(p)&&!WorldRails.piece(p))throw new IllegalArgumentException("UNSUPPORTED_ADAPTER: world loot");
        var state=NativeStates.resolve(p);var items=Block.getDrops(state,level,WorldService.pos(p.origin16().cell()),null);int length=WorldCircuits.piece(p)||WorldRails.piece(p)?16:FragmentItem.length(p.itemPayload());var drops=new ArrayList<ItemStack>();
        for(var stack:items)drops.add(length<16&&stack.getItem()==state.getBlock().asItem()?FragmentItem.create(state.getBlock(),length).copyWithCount(stack.getCount()):stack);
        if(!drops.isEmpty())effects.add(new OperationPlan.Effect("loot:"+UUID.randomUUID(),p.origin16().cell(),Optional.empty(),Optional.of(encode(level,drops))));
    }
    public static BinaryData encode(ServerLevel level,List<ItemStack> drops){var array=new JsonArray();var ops=level.registryAccess().createSerializationContext(JsonOps.INSTANCE);for(var stack:drops)array.add(ItemStack.CODEC.encodeStart(ops,stack).getOrThrow());return new BinaryData(array.toString().getBytes(StandardCharsets.UTF_8));}
    public static List<ItemStack> decode(ServerLevel level,BinaryData data){var array=JsonParser.parseString(new String(data.copy(),StandardCharsets.UTF_8)).getAsJsonArray();if(array.size()>256)throw new IllegalArgumentException("Loot budget");var ops=level.registryAccess().createSerializationContext(JsonOps.INSTANCE);var result=new ArrayList<ItemStack>();for(var value:array)result.add(ItemStack.CODEC.parse(ops,value).getOrThrow());return List.copyOf(result);}
}
