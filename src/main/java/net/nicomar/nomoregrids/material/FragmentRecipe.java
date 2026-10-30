package net.nicomar.nomoregrids.material;

import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.nicomar.nomoregrids.registry.NmgContent;

/** Half-cut and two-fragment join; vanilla crafting consumes one item from each occupied input slot. */
public final class FragmentRecipe extends CustomRecipe {
    private final boolean join;
    public FragmentRecipe(boolean join){this.join=join;}
    @Override public boolean matches(CraftingInput input,Level level){return !assemble(input).isEmpty();}
    @Override public ItemStack assemble(CraftingInput input){
        var items=input.items().stream().filter(s->!s.isEmpty()).toList();
        try{
            if(join){
                if(items.size()!=2||items.stream().anyMatch(s->!(s.getItem() instanceof FragmentItem)))return ItemStack.EMPTY;
                var a=FragmentItem.read(items.get(0));var b=FragmentItem.read(items.get(1));
                if(a.block()!=b.block()||a.texture()!=b.texture()||a.length()+b.length()>16)return ItemStack.EMPTY;return FragmentItem.create(a.block(),a.length()+b.length());
            }
            if(items.size()!=1)return ItemStack.EMPTY;ItemStack source=items.getFirst();
            net.minecraft.world.level.block.Block block=source.getItem() instanceof FragmentItem?FragmentItem.read(source).block():source.getItem() instanceof BlockItem item?item.getBlock():null;
            if(block==null||!FragmentItem.cuttable(block)||!FragmentItem.material(source,block))return ItemStack.EMPTY;
            int length=FragmentItem.length(source);if(length<2||length%2!=0)return ItemStack.EMPTY;return FragmentItem.create(block,length/2).copyWithCount(2);
        }catch(RuntimeException ex){return ItemStack.EMPTY;}
    }
    @Override public RecipeSerializer<? extends CustomRecipe> getSerializer(){return join?NmgContent.FRAGMENT_JOIN.get():NmgContent.FRAGMENT_HALVES.get();}
}
