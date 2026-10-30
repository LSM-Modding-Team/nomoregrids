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
    private VoxelShape collisionCache,selectionCache;
    public CompositeCellEntity(BlockPos pos,BlockState state){super(NmgContent.CELL_ENTITY.get(),pos,state);}
    public CellView view(){return view;}public boolean valid(){return valid;}
    public void install(CellView view){
        if(view.replicas().isEmpty())throw new IllegalArgumentException("Empty cells must be restored to air");
        this.view=Objects.requireNonNull(view);collisionCache=null;selectionCache=null;preserved=null;valid=true;setChanged();requestModelDataUpdate();
        if(level!=null){updateLight();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_ALL);}
    }
    private void updateLight(){
        if(level.isClientSide())return;var parts=new ArrayList<net.nicomar.nomoregrids.simulation.light.CellLighting.Part>();
        long bx=worldPosition.getX()*16L,by=worldPosition.getY()*16L,bz=worldPosition.getZ()*16L;
        for(var p:view.replicas()){
            net.minecraft.world.level.block.state.BlockState source=null;try{source=net.nicomar.nomoregrids.platform.NativeStates.resolve(p);}catch(RuntimeException ignored){}
            var clipped=new ArrayList<Box16>();
            for(var b:p.worldOccupation().boxes()){
                long x0=Math.max(0,b.min().x()-bx),y0=Math.max(0,b.min().y()-by),z0=Math.max(0,b.min().z()-bz),x1=Math.min(16,b.max().x()-bx),y1=Math.min(16,b.max().y()-by),z1=Math.min(16,b.max().z()-bz);
                if(x0<x1&&y0<y1&&z0<z1)clipped.add(Box16.of(x0,y0,z0,x1,y1,z1));
            }
            var profile=source==null?new net.nicomar.nomoregrids.material.MaterialProfile(-1,3600000,0.6f,0,0,15,true,true,false):
                new net.nicomar.nomoregrids.material.MaterialProfile(source.getDestroySpeed(level,worldPosition),source.getBlock().getExplosionResistance(),source.getBlock().getFriction(),source.getBlock().getBounceRestitution(),source.getLightEmission(),source.getLightDampening(),source.canOcclude(),true,false);
            parts.add(new net.nicomar.nomoregrids.simulation.light.CellLighting.Part(clipped,profile));
        }
        var light=net.nicomar.nomoregrids.simulation.light.CellLighting.calculate(parts);var state=getBlockState().setValue(CompositeCellBlock.EMISSION,light.emission()).setValue(CompositeCellBlock.ATTENUATION,light.attenuation()).setValue(CompositeCellBlock.SEALED,light.sealed());
        if(!state.equals(getBlockState()))level.setBlock(worldPosition,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
    }
    public VoxelShape shape(boolean collision){
        if(!valid)return Shapes.block();var cached=collision?collisionCache:selectionCache;if(cached!=null)return cached;VoxelShape shape=Shapes.empty();
        long bx=worldPosition.getX()*16L,by=worldPosition.getY()*16L,bz=worldPosition.getZ()*16L;
        for(var piece:view.replicas()){
            BoxUnion local=collision?piece.geometry().collision():piece.geometry().selection();
            for(var box:local.transform(Rotation.of(piece.orientation()),piece.pivot16(),piece.origin16()).boxes())
                shape=Shapes.or(shape,Block.box(box.min().x()-bx,box.min().y()-by,box.min().z()-bz,box.max().x()-bx,box.max().y()-by,box.max().z()-bz));
        }if(collision){collisionCache=shape;return shape;}selectionCache=new CompositeOutlineShape(shape,view);return selectionCache;
    }
    @Override protected void loadAdditional(ValueInput input){
        super.loadAdditional(input);collisionCache=null;selectionCache=null;preserved=input.getStringOr("nmg_public","");valid=false;
        try{if(preserved.length()>CellView.MAX_BYTES*4/3+8)throw new IOException("Cell snapshot too large");view=CellView.decode(Base64.getDecoder().decode(preserved));valid=!view.replicas().isEmpty();}
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
