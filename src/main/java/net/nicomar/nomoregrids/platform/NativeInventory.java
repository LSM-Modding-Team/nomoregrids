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
        var ops=player.registryAccess().createSerializationContext(JsonOps.INSTANCE);var array=new JsonArray();for(var stack:slots)array.add(ItemStack.OPTIONAL_CODEC.encodeStart(ops,stack).getOrThrow());
        var image=new JsonObject();image.addProperty("schema",2);image.add("slots",array);image.addProperty("level",player.experienceLevel);image.addProperty("total",player.totalExperience);image.addProperty("progress",player.experienceProgress);
        boolean tracked=WorldFunctionals.managed(player.containerMenu)||!player.getPersistentData().getStringOr("nmg_carried","").isEmpty();image.addProperty("tracksCursor",tracked);if(tracked)image.add("carried",ItemStack.OPTIONAL_CODEC.encodeStart(ops,player.containerMenu.getCarried()).getOrThrow());return image(image);
    }
    private static BinaryData image(JsonElement json){return new BinaryData(canonical(json).toString().getBytes(StandardCharsets.UTF_8));}
    private static JsonElement json(BinaryData data){return JsonParser.parseString(new String(data.copy(),StandardCharsets.UTF_8));}
    public static BinaryData withCarried(ServerPlayer player,BinaryData data,ItemStack carried){var j=json(data).getAsJsonObject();j.addProperty("tracksCursor",true);j.add("carried",ItemStack.OPTIONAL_CODEC.encodeStart(player.registryAccess().createSerializationContext(JsonOps.INSTANCE),carried).getOrThrow());return image(j);}
    public static BinaryData experience(BinaryData data,int amount){
        var j=json(data).getAsJsonObject();var next=new net.nicomar.nomoregrids.simulation.functional.ExperienceProgress(j.get("level").getAsInt(),j.get("total").getAsInt(),j.get("progress").getAsFloat()).add(amount);j.addProperty("level",next.level());j.addProperty("total",next.total());j.addProperty("progress",next.progress());return image(j);
    }
    private static ItemStack cursor(ServerPlayer player){var live=player.containerMenu.getCarried();String pending=player.getPersistentData().getStringOr("nmg_carried","");return !live.isEmpty()||pending.isEmpty()?live:ItemStack.OPTIONAL_CODEC.parse(player.registryAccess().createSerializationContext(JsonOps.INSTANCE),JsonParser.parseString(pending)).getOrThrow();}
    public static void restoreCursor(ServerPlayer player){String saved=player.getPersistentData().getStringOr("nmg_carried","");if(!saved.isEmpty()&&player.containerMenu.getCarried().isEmpty())player.containerMenu.setCarried(ItemStack.OPTIONAL_CODEC.parse(player.registryAccess().createSerializationContext(JsonOps.INSTANCE),JsonParser.parseString(saved)).getOrThrow());}
    private static JsonElement canonical(JsonElement value){
        if(value.isJsonObject()){var result=new JsonObject();var keys=new TreeSet<>(value.getAsJsonObject().keySet());for(var key:keys)result.add(key,canonical(value.getAsJsonObject().get(key)));return result;}
        if(value.isJsonArray()){var result=new JsonArray();for(var v:value.getAsJsonArray())result.add(canonical(v));return result;}return value;
    }
    public static List<ItemStack> decode(ServerPlayer player,BinaryData image){
        var value=json(image);if(value.isJsonObject()&&value.getAsJsonObject().get("schema").getAsInt()!=2)throw new IllegalArgumentException("Future inventory receipt");var array=value.isJsonArray()?value.getAsJsonArray():value.getAsJsonObject().getAsJsonArray("slots");
        if(array.size()!=player.getInventory().getContainerSize())throw new IllegalArgumentException("Inventory layout mismatch");
        var ops=player.registryAccess().createSerializationContext(JsonOps.INSTANCE);var slots=new ArrayList<ItemStack>();
        for(var slotValue:array)slots.add(ItemStack.OPTIONAL_CODEC.parse(ops,slotValue).getOrThrow());return slots;
    }
    public static boolean matches(ServerPlayer player,BinaryData image){
        var slots=decode(player,image);for(int i=0;i<slots.size();i++)if(!ItemStack.matches(slots.get(i),player.getInventory().getItem(i)))return false;var value=json(image);if(value.isJsonArray())return true;var j=value.getAsJsonObject();
        return player.experienceLevel==j.get("level").getAsInt()&&player.totalExperience==j.get("total").getAsInt()&&Float.compare(player.experienceProgress,j.get("progress").getAsFloat())==0&&(!j.get("tracksCursor").getAsBoolean()||ItemStack.matches(cursor(player),ItemStack.OPTIONAL_CODEC.parse(player.registryAccess().createSerializationContext(JsonOps.INSTANCE),j.get("carried")).getOrThrow()));
    }
    /** Staging rollback only. Does not emit packets, save, or publish a receipt. */
    public static void restore(ServerPlayer player,BinaryData image){
        var slots=decode(player,image);for(int i=0;i<slots.size();i++)player.getInventory().setItem(i,slots.get(i));var value=json(image);if(value.isJsonArray())return;var j=value.getAsJsonObject();player.experienceLevel=j.get("level").getAsInt();player.totalExperience=j.get("total").getAsInt();player.experienceProgress=j.get("progress").getAsFloat();if(j.get("tracksCursor").getAsBoolean())player.containerMenu.setCarried(ItemStack.OPTIONAL_CODEC.parse(player.registryAccess().createSerializationContext(JsonOps.INSTANCE),j.get("carried")).getOrThrow());
    }
    public static void install(ServerPlayer player,BinaryData before,BinaryData after){
        if(!matches(player,after)){if(!matches(player,before))throw new IllegalArgumentException("Inventory receipt conflict");restore(player,after);}
        var value=json(after);if(value.isJsonObject()&&value.getAsJsonObject().get("tracksCursor").getAsBoolean()){
            var carried=value.getAsJsonObject().get("carried");boolean empty=ItemStack.OPTIONAL_CODEC.parse(player.registryAccess().createSerializationContext(JsonOps.INSTANCE),carried).getOrThrow().isEmpty();player.getPersistentData().putString("nmg_carried",empty?"":carried.toString());
        }player.getInventory().setChanged();player.containerMenu.broadcastChanges();
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
