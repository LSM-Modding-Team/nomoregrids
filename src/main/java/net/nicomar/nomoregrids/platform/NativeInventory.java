package net.nicomar.nomoregrids.platform;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.nicomar.nomoregrids.core.BinaryData;

/** Plans on copied stacks. Complete before/after images are private journal receipts, not client payloads. */
public final class NativeInventory {
    private NativeInventory(){}
    public static List<ItemStack> copy(ServerPlayer player){var slots=new ArrayList<ItemStack>();for(int i=0;i<player.getInventory().getContainerSize();i++)slots.add(player.getInventory().getItem(i).copy());return slots;}
    public static BinaryData encode(ServerPlayer player,List<ItemStack> slots){
        var ops=player.registryAccess().createSerializationContext(JsonOps.INSTANCE);JsonArray array=new JsonArray();
        for(var stack:slots)array.add(ItemStack.OPTIONAL_CODEC.encodeStart(ops,stack).getOrThrow());
        byte[] bytes=canonical(array).toString().getBytes(StandardCharsets.UTF_8);return new BinaryData(bytes);
    }
    private static JsonElement canonical(JsonElement value){
        if(value.isJsonObject()){var result=new JsonObject();var keys=new TreeSet<>(value.getAsJsonObject().keySet());for(var key:keys)result.add(key,canonical(value.getAsJsonObject().get(key)));return result;}
        if(value.isJsonArray()){var result=new JsonArray();for(var v:value.getAsJsonArray())result.add(canonical(v));return result;}return value;
    }
    public static List<ItemStack> decode(ServerPlayer player,BinaryData image){
        var array=JsonParser.parseString(new String(image.copy(),StandardCharsets.UTF_8)).getAsJsonArray();
        if(array.size()!=player.getInventory().getContainerSize())throw new IllegalArgumentException("Inventory layout mismatch");
        var ops=player.registryAccess().createSerializationContext(JsonOps.INSTANCE);var slots=new ArrayList<ItemStack>();
        for(var value:array)slots.add(ItemStack.OPTIONAL_CODEC.parse(ops,value).getOrThrow());return slots;
    }
    public static boolean matches(ServerPlayer player,BinaryData image){
        var slots=decode(player,image);for(int i=0;i<slots.size();i++)if(!ItemStack.matches(slots.get(i),player.getInventory().getItem(i)))return false;return true;
    }
    public static void install(ServerPlayer player,BinaryData before,BinaryData after){
        if(matches(player,after))return;if(!matches(player,before))throw new IllegalArgumentException("Inventory receipt conflict");
        var slots=decode(player,after);for(int i=0;i<slots.size();i++)player.getInventory().setItem(i,slots.get(i));player.getInventory().setChanged();player.containerMenu.broadcastChanges();
    }
    public static void insert(List<ItemStack> slots,ItemStack loot){
        ItemStack remaining=loot.copy();
        for(int i=0;i<Math.min(36,slots.size())&&!remaining.isEmpty();i++){
            ItemStack slot=slots.get(i);if(!slot.isEmpty()&&ItemStack.isSameItemSameComponents(slot,remaining)){
                int n=Math.min(remaining.getCount(),slot.getMaxStackSize()-slot.getCount());if(n>0){slot.grow(n);remaining.shrink(n);}
            }
        }
        for(int i=0;i<Math.min(36,slots.size())&&!remaining.isEmpty();i++)if(slots.get(i).isEmpty()){
            int n=Math.min(remaining.getCount(),remaining.getMaxStackSize());slots.set(i,remaining.copyWithCount(n));remaining.shrink(n);
        }
        if(!remaining.isEmpty())throw new IllegalArgumentException("INVENTORY_MISSING: no room for conserved loot");
    }
    public static void takeFragment(List<ItemStack> slots,net.minecraft.world.level.block.Block block,int length){
        int selected=-1,best=17;var indices=new ArrayList<Integer>();for(int i=0;i<Math.min(36,slots.size());i++)indices.add(i);
        if(net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND<slots.size())indices.add(net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND);
        for(int slot:indices)if(net.nicomar.nomoregrids.material.FragmentItem.material(slots.get(slot),block)){
            int size=net.nicomar.nomoregrids.material.FragmentItem.length(slots.get(slot));if(size>=length&&size<best){best=size;selected=slot;}
        }
        if(selected<0)throw new IllegalArgumentException("INVENTORY_MISSING");slots.get(selected).shrink(1);
        if(best>length)insert(slots,net.nicomar.nomoregrids.material.FragmentItem.create(block,best-length));
    }
}
