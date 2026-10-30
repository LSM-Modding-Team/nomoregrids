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
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;
import net.nicomar.nomoregrids.registry.NmgContent;
import net.nicomar.nomoregrids.geometry.*;

/** Stores public query/render data only, never authoritative inventory or loot data. */
public final class CompositeCellEntity extends BlockEntity {
    public static final ModelProperty<CellView> VIEW=new ModelProperty<>();
    private CellView view=new CellView(List.of());
    private String preserved;
    private boolean valid;
    private VoxelShape collisionCache,selectionCache,supportCache;
    private Map<UUID,List<BoxMesh.Face>> contactFaces;
    public CompositeCellEntity(BlockPos pos,BlockState state){super(NmgContent.CELL_ENTITY.get(),pos,state);}
    @Override public void onLoad(){super.onLoad();LoadedCells.add(this);}
    @Override public void setRemoved(){LoadedCells.remove(this);super.setRemoved();}
    public CellView view(){return view;}public boolean valid(){return valid;}
    public void install(CellView view){
        if(!view.occupied())throw new IllegalArgumentException("Empty cells must be restored to air");
        var previous=this.view;this.view=Objects.requireNonNull(view);LoadedCells.add(this);collisionCache=null;selectionCache=null;supportCache=null;contactFaces=null;preserved=null;valid=true;setChanged();requestModelDataUpdate();
        if(level instanceof net.minecraft.server.level.ServerLevel server)net.nicomar.nomoregrids.network.CellReplication.changed(server,worldPosition,previous,view);
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
        var water=view.cavity().map(c->c.amount(net.nicomar.nomoregrids.simulation.fluid.CavityCell.Kind.WATER)).orElse(0);var lava=view.cavity().map(c->c.amount(net.nicomar.nomoregrids.simulation.fluid.CavityCell.Kind.LAVA)).orElse(0);
        var light=net.nicomar.nomoregrids.simulation.light.CellLighting.calculate(parts);var state=getBlockState().setValue(CompositeCellBlock.EMISSION,lava>0?15:light.emission()).setValue(CompositeCellBlock.ATTENUATION,light.attenuation()).setValue(CompositeCellBlock.SEALED,light.sealed()).setValue(CompositeCellBlock.FLUID,lava>0?2:water>0?1:0).setValue(CompositeCellBlock.SOURCE,view.cavity().map(c->c.source()!=net.nicomar.nomoregrids.simulation.fluid.CavityCell.Kind.EMPTY).orElse(false));
        if(!state.equals(getBlockState()))level.setBlock(worldPosition,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
    }
    public VoxelShape shape(boolean collision){
        if(!valid)return Shapes.block();var cached=collision?collisionCache:selectionCache;if(cached!=null)return cached;VoxelShape shape=Shapes.empty();
        long bx=worldPosition.getX()*16L,by=worldPosition.getY()*16L,bz=worldPosition.getZ()*16L;
        for(var piece:view.replicas()){
            BoxUnion local=collision?piece.geometry().collision():piece.geometry().selection();
            for(var box:local.transform(Rotation.of(piece.orientation()),piece.pivot16(),piece.origin16()).boxes())
                shape=Shapes.or(shape,Block.box(box.min().x()-bx,box.min().y()-by,box.min().z()-bz,box.max().x()-bx,box.max().y()-by,box.max().z()-bz));
        }if(collision){collisionCache=shape;return shape;}if(view.replicas().isEmpty()&&view.cavity().isPresent())shape=Shapes.block();selectionCache=new CompositeOutlineShape(shape,view);return selectionCache;
    }
    public Optional<net.nicomar.nomoregrids.core.PieceRecord> ground(net.minecraft.world.entity.Entity entity){
        if(!valid||entity==null)return Optional.empty();var body=entity.getBoundingBox();if(contactFaces==null){
            Map<UUID,List<BoxMesh.Face>> faces=new HashMap<>();for(var p:view.replicas())if(!p.flags().contains(net.nicomar.nomoregrids.core.PieceRecord.Flag.SUBSTITUTE))faces.put(p.pieceId(),net.nicomar.nomoregrids.diagnostic.GeometryCache.faces(p.geometry().collision().transform(Rotation.of(p.orientation()),p.pivot16(),p.origin16()),4096));contactFaces=Map.copyOf(faces);
        }
        var foot=new net.nicomar.nomoregrids.simulation.contact.ContactResolver.Foot(body.minX*16,body.minZ*16,body.maxX*16,body.maxZ*16,body.minY*16);
        return net.nicomar.nomoregrids.simulation.contact.ContactResolver.ground(foot,contactFaces).flatMap(hit->view.replicas().stream().filter(p->p.pieceId().equals(hit.piece())).findFirst());
    }
    public boolean climbs(net.minecraft.world.entity.LivingEntity entity){
        if(!valid||entity==null)return false;var body=entity.getBoundingBox().inflate(1e-7);for(var p:view.replicas())if(p.sourceBlockId().value().equals("minecraft:ladder")&&!p.flags().contains(net.nicomar.nomoregrids.core.PieceRecord.Flag.SUBSTITUTE)&&net.nicomar.nomoregrids.core.Direction6.UP.rotated(p.orientation())==net.nicomar.nomoregrids.core.Direction6.UP){
            for(var box:p.worldOccupation().boxes())if(body.intersects(new net.minecraft.world.phys.AABB(box.min().x()/16.0,box.min().y()/16.0,box.min().z()/16.0,box.max().x()/16.0,box.max().y()/16.0,box.max().z()/16.0)))return true;
        }return false;
    }
    public boolean extendsVertically(net.minecraft.world.entity.Entity entity){
        if(!valid||entity==null)return false;return ground(entity).map(p->p.geometry().collision().transform(Rotation.of(p.orientation()),p.pivot16(),p.origin16()).boxes().stream().anyMatch(b->b.max().y()>worldPosition.getY()*16L+16)).orElse(false);
    }
    public VoxelShape supportShape(){
        if(!valid)return Shapes.empty();if(supportCache!=null)return supportCache;VoxelShape shape=Shapes.empty();
        long bx=worldPosition.getX()*16L,by=worldPosition.getY()*16L,bz=worldPosition.getZ()*16L;
        for(var p:view.replicas()){
            if(p.flags().contains(net.nicomar.nomoregrids.core.PieceRecord.Flag.SUBSTITUTE)||!net.nicomar.nomoregrids.simulation.support.SupportPatches.required(p).isEmpty())continue;BoxUnion support;
            if(p.adapterId().value().equals("nomoregrids:box"))support=p.geometry().occupation();
            else if(p.adapterId().value().equals("nomoregrids:paintableblocks")){
                var bridge=net.nicomar.nomoregrids.compat.paintableblocks.PaintableBridge.available();if(bridge.isEmpty())continue;try{support=bridge.orElseThrow().support(p.geometryPayload());}catch(RuntimeException ex){continue;}
            }else continue;
            for(var box:support.transform(Rotation.of(p.orientation()),p.pivot16(),p.origin16()).boxes())shape=Shapes.or(shape,Block.box(box.min().x()-bx,box.min().y()-by,box.min().z()-bz,box.max().x()-bx,box.max().y()-by,box.max().z()-bz));
        }supportCache=shape;return shape;
    }
    @Override protected void loadAdditional(ValueInput input){
        super.loadAdditional(input);collisionCache=null;selectionCache=null;supportCache=null;contactFaces=null;preserved=input.getStringOr("nmg_public","");valid=false;
        try{if(preserved.length()>CellView.MAX_BYTES*4/3+8)throw new IOException("Cell snapshot too large");view=CellView.decode(Base64.getDecoder().decode(preserved));valid=view.occupied();}
        catch(IOException|IllegalArgumentException ex){view=new CellView(List.of());}
    }
    @Override protected void saveAdditional(ValueOutput output){
        super.saveAdditional(output);
        if(preserved!=null){output.putString("nmg_public",preserved);return;}
        try{output.putString("nmg_public",Base64.getEncoder().encodeToString(view.encode()));}catch(IOException ex){throw new IllegalStateException("Cannot save cell",ex);}
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup){
        // Public replicas travel via the bounded fragment protocol, including initial chunk tracking.
        return new CompoundTag();
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return null;}
    @Override public ModelData getModelData(){return valid?ModelData.builder().with(VIEW,view).build():ModelData.EMPTY;}
}
