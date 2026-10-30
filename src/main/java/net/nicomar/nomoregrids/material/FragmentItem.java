package net.nicomar.nomoregrids.material;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.nicomar.nomoregrids.core.BinaryData;
import net.nicomar.nomoregrids.registry.NmgContent;

/** Non-vanilla fragment item: a partial pane can never be placed as a full native pane. */
public final class FragmentItem extends Item {
    private static final int MAGIC=0x4e4d4746;
    public record Data(Block block,int length,int texture){public Data{if(!cuttable(block)||length<1||length>16||texture!=0)throw new IllegalArgumentException("Invalid fragment");}}
    public FragmentItem(Properties properties){super(properties);}
    public static boolean cuttable(Block block){return block instanceof IronBarsBlock;}
    public static ItemStack create(Block block,int length){
        Data data=new Data(block,length,0);var tag=new CompoundTag();tag.putInt("schema",1);tag.putString("block",BuiltInRegistries.BLOCK.getKey(block).toString());tag.putInt("length",data.length);tag.putInt("texture",data.texture);
        var stack=new ItemStack(NmgContent.FRAGMENT.get());stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return stack;
    }
    public static Data read(ItemStack stack){
        if(!(stack.getItem() instanceof FragmentItem))throw new IllegalArgumentException("Not a fragment");var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(tag.getIntOr("schema",0)!=1)throw new IllegalArgumentException("Unknown fragment schema; item retained");
        Block block=BuiltInRegistries.BLOCK.getOptional(Identifier.parse(tag.getStringOr("block",""))).orElseThrow();var data=new Data(block,tag.getIntOr("length",0),tag.getIntOr("texture",-1));
        if(!ItemStack.isSameItemSameComponents(stack,create(block,data.length)))throw new IllegalArgumentException("Valued/custom fragment metadata requires an adapter");return data;
    }
    public static BinaryData descriptor(int length){if(length<1||length>16)throw new IllegalArgumentException("Length");return new BinaryData(ByteBuffer.allocate(6).putInt(MAGIC).put((byte)1).put((byte)length).array());}
    public static int length(BinaryData descriptor){
        if(descriptor.size()==0)return 16;byte[] bytes=descriptor.copy();if(bytes.length!=6)throw new IllegalArgumentException("Unknown item payload");var in=ByteBuffer.wrap(bytes);
        if(in.getInt()!=MAGIC||in.get()!=1)throw new IllegalArgumentException("Unknown fragment descriptor");int length=in.get()&255;if(length<1||length>16)throw new IllegalArgumentException("Length");return length;
    }
    public static boolean material(ItemStack stack,Block block){
        if(stack.isEmpty())return false;
        try{if(stack.getItem() instanceof FragmentItem)return read(stack).block==block;
            return stack.getItem() instanceof BlockItem item&&item.getBlock()==block&&ItemStack.isSameItemSameComponents(stack,item.getDefaultInstance());}
        catch(RuntimeException ex){return false;}
    }
    public static int length(ItemStack stack){return stack.getItem() instanceof FragmentItem?read(stack).length:16;}
    @Override public Component getName(ItemStack stack){try{Data data=read(stack);return Component.translatable("item.nomoregrids.fragment.named",data.block.getName(),data.length);}catch(RuntimeException ex){return Component.translatable("item.nomoregrids.fragment.invalid");}}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> tooltip,TooltipFlag flag){super.appendHoverText(stack,context,display,tooltip,flag);tooltip.accept(Component.translatable("tooltip.nomoregrids.fragment"));}
    @Override public InteractionResult useOn(UseOnContext context){
        if(context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player)net.nicomar.nomoregrids.platform.NmgRuntime.action(player,new net.nicomar.nomoregrids.network.ActionPayload(java.util.UUID.randomUUID(),net.nicomar.nomoregrids.network.ActionPayload.Action.PLACE,context.getHand()==InteractionHand.OFF_HAND));
        return InteractionResult.SUCCESS;
    }
}
