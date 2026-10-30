package net.nicomar.nomoregrids.platform;

import java.lang.reflect.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Optional PB schema-2 production/bucket profiles, checked against the inspected native API. */
public final class PaintableFunctionals {
    private static final String ROOT="net.nicomar.paintableblocks.";
    private static final class Api {
        final Class<?> production,bucket,workstation,machine,productionMenu,bucketMenu;final Method validProduction,validBucket,toProductionItem,toBucketItem,tick,bucketUse;
        Api()throws ReflectiveOperationException{var loader=PaintableFunctionals.class.getClassLoader();if(Class.forName(ROOT+"design.BlockDesign",false,loader).getField("VERSION").getInt(null)!=2)throw new IllegalArgumentException("PB schema");production=Class.forName(ROOT+"blockentity.PortableProductionEntity",false,loader);bucket=Class.forName(ROOT+"blockentity.PaintBucketBlockEntity",false,loader);workstation=Class.forName(ROOT+"blockentity.WorkstationBlockEntity",false,loader);machine=Class.forName(ROOT+"blockentity.PaintMachineBlockEntity",false,loader);productionMenu=Class.forName(ROOT+"menu.ProductionMenu",false,loader);bucketMenu=Class.forName(ROOT+"menu.PaintBucketMenu",false,loader);validProduction=production.getMethod("valid");validBucket=bucket.getMethod("valid");toProductionItem=production.getMethod("toItem");toBucketItem=bucket.getMethod("toItem");tick=machine.getMethod("tick",Level.class,BlockPos.class,BlockState.class,machine);Class.forName(ROOT+"menu.WorkstationMenu",false,loader).getMethod("action",Player.class,int.class,int.class,int.class,int.class);bucketMenu.getMethod("clickMenuButton",Player.class,int.class);bucketUse=Class.forName(ROOT+"interaction.ServerInteractions",false,loader).getMethod("bucket",Player.class,BlockPos.class,net.minecraft.world.InteractionHand.class,boolean.class);}
    }
    private static final class OptionalApi{static final Api VALUE=create();private static Api create(){try{return new Api();}catch(ReflectiveOperationException|RuntimeException|LinkageError ex){return null;}}}
    private PaintableFunctionals(){}
    public static boolean eligible(BlockState state){if(OptionalApi.VALUE==null)return false;String type=state.getBlock().getClass().getName();return Set.of(ROOT+"block.WorkstationBlock",ROOT+"block.PaintMachineBlock",ROOT+"block.PaintBucketBlock").contains(type);}
    public static boolean entity(BlockEntity be){return OptionalApi.VALUE!=null&&(OptionalApi.VALUE.production.isInstance(be)||OptionalApi.VALUE.bucket.isInstance(be));}
    public static boolean bucket(BlockEntity be){return OptionalApi.VALUE!=null&&OptionalApi.VALUE.bucket.isInstance(be);}
    public static boolean bucketItem(ItemStack stack){String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();return id.equals("paintableblocks:paint")||stack.getItem().getClass().getName().equals(ROOT+"item.BrushItem");}
    public static void bucketUse(Player player,BlockPos pos,net.minecraft.world.InteractionHand hand){invoke(OptionalApi.VALUE.bucketUse,null,player,pos,hand,player.isShiftKeyDown());}
    public static boolean production(BlockEntity be){return OptionalApi.VALUE!=null&&OptionalApi.VALUE.production.isInstance(be);}
    public static boolean machine(BlockEntity be){return OptionalApi.VALUE!=null&&OptionalApi.VALUE.machine.isInstance(be);}
    public static boolean valid(BlockEntity be){if(!entity(be))return false;return (boolean)invoke(production(be)?OptionalApi.VALUE.validProduction:OptionalApi.VALUE.validBucket,be);}
    public static ItemStack item(BlockEntity be){if(!valid(be))throw new IllegalArgumentException("INVALID_DATA: PB functional");return ((ItemStack)invoke(production(be)?OptionalApi.VALUE.toProductionItem:OptionalApi.VALUE.toBucketItem,be)).copy();}
    public static void stripInventory(BlockEntity be,CompoundTag tag){if(!production(be))return;var production=tag.getCompoundOrEmpty("production").copy();production.put("items",new ListTag());tag.put("production",production);}
    public static void tick(Level level,BlockEntity be){if(machine(be))invoke(OptionalApi.VALUE.tick,null,level,be.getBlockPos(),be.getBlockState(),be);}
    public static AbstractContainerMenu menu(int id,Inventory inventory,BlockPos pos,BlockEntity be){String type=OptionalApi.VALUE.bucket.isInstance(be)?"PaintBucketMenu":OptionalApi.VALUE.workstation.isInstance(be)?"WorkstationMenu":"PaintMachineMenu";try{return (AbstractContainerMenu)Class.forName(ROOT+"menu."+type).getConstructor(int.class,Inventory.class,BlockPos.class).newInstance(id,inventory,pos);}catch(ReflectiveOperationException ex){throw new IllegalArgumentException("UNSUPPORTED_ADAPTER: PB menu",ex);}}
    public static boolean action(AbstractContainerMenu menu,Player player,int action,int variant,int resolution,int material){try{return (boolean)invoke(menu.getClass().getMethod("action",Player.class,int.class,int.class,int.class,int.class),menu,player,action,variant,resolution,material);}catch(NoSuchMethodException ex){throw new IllegalArgumentException("PB action API",ex);}}
    private static Object invoke(Method method,Object owner,Object...args){try{return method.invoke(owner,args);}catch(InvocationTargetException ex){if(ex.getCause() instanceof RuntimeException failure)throw failure;throw new IllegalArgumentException("PB functional callback",ex.getCause());}catch(ReflectiveOperationException ex){throw new IllegalArgumentException("PB functional API",ex);}}
}
