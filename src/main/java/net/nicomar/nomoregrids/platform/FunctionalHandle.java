package net.nicomar.nomoregrids.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;

/** A planning view of one detached native BE. It is never installed or saved as a second world inventory. */
public final class FunctionalHandle implements Container {
    private final BlockEntity entity;
    private final Container inventory;
    public FunctionalHandle(BlockEntity entity){this.entity=entity;this.inventory=entity instanceof Container c?c:null;}
    public BlockEntity entity(){return entity;}
    public BlockPos getBlockPos(){return entity.getBlockPos();}
    public BlockState getBlockState(){return entity.getBlockState();}
    public CompoundTag saveWithoutMetadata(HolderLookup.Provider lookup){return entity.saveWithoutMetadata(lookup);}
    public void loadWithComponents(ValueInput input){entity.loadWithComponents(input);}
    public boolean canOpen(Player player){return entity instanceof BaseContainerBlockEntity nativeBE?nativeBE.canOpen(player):PaintableFunctionals.valid(entity);}
    public Component getDisplayName(){return entity instanceof BaseContainerBlockEntity nativeBE?nativeBE.getDisplayName():Component.translatable(entity.getBlockState().getBlock().getDescriptionId());}
    public int getContainerSize(){return inventory==null?0:inventory.getContainerSize();}
    public boolean isEmpty(){return inventory==null||inventory.isEmpty();}
    public ItemStack getItem(int slot){return inventory==null?ItemStack.EMPTY:inventory.getItem(slot);}
    public ItemStack removeItem(int slot,int amount){return inventory==null?ItemStack.EMPTY:inventory.removeItem(slot,amount);}
    public ItemStack removeItemNoUpdate(int slot){return inventory==null?ItemStack.EMPTY:inventory.removeItemNoUpdate(slot);}
    public void setItem(int slot,ItemStack stack){if(inventory!=null)inventory.setItem(slot,stack);}
    public void setChanged(){entity.setChanged();}
    public void clearContent(){if(inventory!=null)inventory.clearContent();}
    public boolean stillValid(Player player){return inventory!=null&&inventory.stillValid(player);}
    public boolean canPlaceItem(int slot,ItemStack stack){return inventory!=null&&inventory.canPlaceItem(slot,stack);}
    public int getMaxStackSize(){return inventory==null?64:inventory.getMaxStackSize();}
}
