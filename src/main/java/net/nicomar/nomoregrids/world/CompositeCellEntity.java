package net.nicomar.nomoregrids.world;

import java.io.IOException;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.nicomar.nomoregrids.registry.NmgContent;
import net.nicomar.nomoregrids.geometry.*;

/** Stores public query/render data only, never authoritative inventory or loot data. */
public final class CompositeCellEntity extends BlockEntity {
    public static final ModelProperty<CellView> VIEW=new ModelProperty<>();
    private CellView view=new CellView(List.of());
    private String preserved;
    private boolean valid;
    public CompositeCellEntity(BlockPos pos,BlockState state){super(NmgContent.CELL_ENTITY.get(),pos,state);}
    public CellView view(){return view;}public boolean valid(){return valid;}
    public void install(CellView view){
        this.view=Objects.requireNonNull(view);preserved=null;valid=true;setChanged();requestModelDataUpdate();
        if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_ALL);
    }
    public VoxelShape shape(boolean collision){
        if(!valid)return Shapes.block();VoxelShape shape=Shapes.empty();
        long bx=worldPosition.getX()*16L,by=worldPosition.getY()*16L,bz=worldPosition.getZ()*16L;
        for(var piece:view.replicas()){
            BoxUnion local=collision?piece.geometry().collision():piece.geometry().selection();
            for(var box:local.transform(Rotation.of(piece.orientation()),piece.pivot16(),piece.origin16()).boxes())
                shape=Shapes.or(shape,Block.box(box.min().x()-bx,box.min().y()-by,box.min().z()-bz,box.max().x()-bx,box.max().y()-by,box.max().z()-bz));
        }return shape;
    }
    @Override protected void loadAdditional(ValueInput input){
        super.loadAdditional(input);preserved=input.getStringOr("nmg_public","");valid=false;
        try{if(preserved.length()>CellView.MAX_BYTES*4/3+8)throw new IOException("Cell snapshot too large");view=CellView.decode(Base64.getDecoder().decode(preserved));valid=true;}
        catch(IOException|IllegalArgumentException ex){view=new CellView(List.of());}
    }
    @Override protected void saveAdditional(ValueOutput output){
        super.saveAdditional(output);
        if(preserved!=null){output.putString("nmg_public",preserved);return;}
        try{output.putString("nmg_public",Base64.getEncoder().encodeToString(view.encode()));}catch(IOException ex){throw new IllegalStateException("Cannot save cell",ex);}
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup){return saveWithoutMetadata(lookup);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public ModelData getModelData(){return valid?ModelData.builder().with(VIEW,view).build():ModelData.EMPTY;}
}
