package net.nicomar.nomoregrids.compat.paintableblocks;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.*;
import net.nicomar.nomoregrids.platform.NativeStates;

/** Isolated bridge to the inspected native planners. Never installs a temporary native block entity. */
public final class PaintableBridge {
    private final PaintableAdapter adapter;
    private final Class<?> design,entity,block,item,brush,tool,geometry,operations,plan,surface;
    private final DataComponentType<Object> designComponent,brushComponent;
    @SuppressWarnings("unchecked")
    public PaintableBridge()throws ReflectiveOperationException{
        ClassLoader loader=PaintableBridge.class.getClassLoader();adapter=new PaintableAdapter(loader);
        design=Class.forName("net.nicomar.paintableblocks.design.BlockDesign",true,loader);entity=Class.forName("net.nicomar.paintableblocks.blockentity.PaintableBlockEntity",true,loader);block=Class.forName("net.nicomar.paintableblocks.block.PaintableBlock",true,loader);item=Class.forName("net.nicomar.paintableblocks.item.PaintableBlockItem",true,loader);
        brush=Class.forName("net.nicomar.paintableblocks.item.BrushItem",true,loader);tool=Class.forName("net.nicomar.paintableblocks.item.EditingToolItem",true,loader);geometry=Class.forName("net.nicomar.paintableblocks.design.DesignGeometry",true,loader);operations=Class.forName("net.nicomar.paintableblocks.operation.DesignOperations",true,loader);plan=Class.forName("net.nicomar.paintableblocks.operation.DesignOperations$Plan",true,loader);surface=Class.forName("net.nicomar.paintableblocks.design.DesignGeometry$Surface",true,loader);
        Class<?> content=Class.forName("net.nicomar.paintableblocks.registry.ModContent",true,loader);designComponent=(DataComponentType<Object>)holder(content,"DESIGN");brushComponent=(DataComponentType<Object>)holder(content,"BRUSH_PAINT");
    }
    private static Object holder(Class<?> content,String field)throws ReflectiveOperationException{Object holder=content.getField(field).get(null);return holder.getClass().getMethod("get").invoke(holder);}
    private static final class OptionalBridge{
        static final PaintableBridge INSTANCE=create();
        private static PaintableBridge create(){if(!net.neoforged.fml.ModList.get().isLoaded("paintableblocks"))return null;try{return new PaintableBridge();}catch(ReflectiveOperationException|RuntimeException|LinkageError ex){net.nicomar.nomoregrids.NoMoreGrids.LOGGER.warn("PB native bridge unavailable; original designs retained",ex);return null;}}
    }
    public static Optional<PaintableBridge> available(){return Optional.ofNullable(OptionalBridge.INSTANCE);}
    public boolean placementItem(ItemStack stack){return item.isInstance(stack.getItem());}
    public boolean editingItem(ItemStack stack){return brush.isInstance(stack.getItem())||tool.isInstance(stack.getItem())||former(stack);}
    public boolean brush(ItemStack stack){return brush.isInstance(stack.getItem());}
    public boolean former(ItemStack stack){return stack.getItem().getClass().getName().equals("net.nicomar.paintableblocks.item.SlopeFormerItem");}
    public boolean volumeTool(ItemStack stack){return former(stack)||stack.getItem().getClass().getName().equals("net.nicomar.paintableblocks.item.HammerItem");}
    public Object surface(PieceRecord piece,BlockHitResult hit){
        try{var before=adapter.restore(piece.geometryPayload());var face=Direction.valueOf(Direction6.valueOf(hit.getDirection().name()).rotated(Rotation.of(piece.orientation()).inverse().id()).name());return ((Optional<?>)geometry.getMethod("target",design,Vec3.class,Direction.class).invoke(null,before,localPoint(piece,hit.getLocation()),face)).orElseThrow(()->new IllegalArgumentException("No exposed PB surface"));}catch(ReflectiveOperationException ex){throw mismatch(ex);}
    }
    public Box16 selection(PieceRecord p,BlockHitResult hit,boolean frame){
        try{Object d=adapter.restore(p.geometryPayload());Class<?> region=Class.forName("net.nicomar.paintableblocks.operation.VolumeRegion");Object bounds;
            if(frame)bounds=region.getMethod("block",BlockPos.class).invoke(null,BlockPos.ZERO);else{Object target=surface(p,hit);bounds=region.getMethod("cell",BlockPos.class,design,int.class).invoke(null,BlockPos.ZERO,d,(int)surface.getMethod("cell").invoke(target));}
            long x0=(int)region.getMethod("x0").invoke(bounds),y0=(int)region.getMethod("y0").invoke(bounds),z0=(int)region.getMethod("z0").invoke(bounds),x1=(int)region.getMethod("x1").invoke(bounds),y1=(int)region.getMethod("y1").invoke(bounds),z1=(int)region.getMethod("z1").invoke(bounds);
            return Box16.of(x0,y0,z0,x1,y1,z1).transform(Rotation.of(p.orientation()),p.pivot16(),p.origin16());
        }catch(ReflectiveOperationException ex){throw mismatch(ex);}
    }
    public boolean nativeBlock(BlockState state){return block.isInstance(state.getBlock());}
    public PieceRecord placement(BlockPlaceContext context,UUID id){
        try{
            Object nativeDesign=item.getMethod("preparedFor",BlockPlaceContext.class).invoke(null,context);if(nativeDesign==null)throw new IllegalArgumentException("Invalid PB placement design");
            BlockState state=((BlockItem)context.getItemInHand().getItem()).getBlock().getStateForPlacement(context);if(state==null)throw new IllegalArgumentException("Invalid PB placement state");
            return capture(state,nativeDesign,context.getItemInHand(),context.getLevel().registryAccess(),context.getClickedPos(),id);
        }catch(ReflectiveOperationException ex){throw mismatch(ex);}
    }
    public PieceRecord captureNative(ServerLevel level,BlockPos pos,UUID id){
        BlockEntity nativeEntity=level.getBlockEntity(pos);if(!entity.isInstance(nativeEntity))throw new IllegalArgumentException("UNSUPPORTED_ADAPTER: PB machine is not a design");
        try{if(!(boolean)entity.getMethod("valid").invoke(nativeEntity))throw new IllegalArgumentException("INVALID_DATA: PB original retained");Object d=entity.getMethod("design").invoke(nativeEntity);ItemStack original=(ItemStack)entity.getMethod("toItem",boolean.class).invoke(nativeEntity,true);var captured=capture(level.getBlockState(pos),d,original,level.registryAccess(),pos,id);
            var envelope=JsonParser.parseString(new String(captured.itemPayload().copy(),StandardCharsets.UTF_8)).getAsJsonObject();var nativeImage=new JsonObject();nativeImage.addProperty("identity",entity.getMethod("editIdentity").invoke(nativeEntity).toString());nativeImage.addProperty("designHash",hash(captured.geometryPayload()));nativeImage.addProperty("revision",(long)entity.getMethod("revision").invoke(nativeEntity));nativeImage.add("blockEntity",nativeData(nativeEntity,level.registryAccess()));
            var stateImage=new JsonObject();NativeStates.properties(level.getBlockState(pos)).forEach(stateImage::addProperty);nativeImage.add("state",stateImage);envelope.add("native",nativeImage);return withItem(captured,new BinaryData(envelope.toString().getBytes(StandardCharsets.UTF_8)));}
        catch(ReflectiveOperationException ex){throw mismatch(ex);}
    }
    public PieceRecord nativeSelection(ServerLevel level,BlockPos pos){
        try{Object be=level.getBlockEntity(pos);UUID identity=(UUID)entity.getMethod("editIdentity").invoke(be);UUID id=UUID.nameUUIDFromBytes((level.dimension().identifier()+"|"+pos+"|"+identity).getBytes(StandardCharsets.UTF_8));var p=captureNative(level,pos,id);return revision(p,(long)entity.getMethod("revision").invoke(be));}catch(ReflectiveOperationException ex){throw mismatch(ex);}
    }
    public PieceRecord create(ServerLevel level,BlockPos position,int resolution){
        try{Object full=design.getMethod("full",int.class).invoke(null,resolution);Class<?> content=Class.forName("net.nicomar.paintableblocks.registry.ModContent");var nativeBlock=(net.minecraft.world.level.block.Block)content.getMethod("block",int.class).invoke(null,resolution);return capture(nativeBlock.defaultBlockState(),full,nativeBlock.asItem().getDefaultInstance(),level.registryAccess(),position,UUID.randomUUID());}catch(ReflectiveOperationException ex){throw mismatch(ex);}
    }
    public static PieceRecord revision(PieceRecord p,long revision){return new PieceRecord(p.pieceId(),revision,p.adapterId(),p.adapterVersion(),p.sourceBlockId(),p.sourceState(),p.origin16(),p.orientation(),p.pivot16(),p.geometry(),p.geometryPayload(),p.materialPayload(),p.itemPayload(),p.groupId(),p.flags());}
    private PieceRecord capture(BlockState state,Object d,ItemStack original,HolderLookup.Provider lookup,BlockPos pos,UUID id)throws ReflectiveOperationException{
        if(!block.isInstance(state.getBlock())||(int)block.getMethod("resolution").invoke(state.getBlock())!=(int)design.getMethod("resolution").invoke(d))throw new IllegalArgumentException("PB resolution mismatch");
        BinaryData data=adapter.capture(d);var properties=new HashMap<>(NativeStates.properties(state));properties.put("cut",Boolean.toString(!(boolean)design.getMethod("complete").invoke(d)));
        var template=original.copyWithCount(1);template.remove(designComponent);var image=new JsonObject();image.addProperty("schema",1);image.add("item",ItemStack.CODEC.encodeStart(lookup.createSerializationContext(JsonOps.INSTANCE),template).getOrThrow());
        return new PieceRecord(id,0,adapter.id(),adapter.dataVersion(),new ResourceId(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()),properties,new GridPoint(pos.getX()*16L,pos.getY()*16L,pos.getZ()*16L),0,GridPoint.BLOCK_CENTER,snapshot(data),data,BinaryData.EMPTY,new BinaryData(image.toString().getBytes(StandardCharsets.UTF_8)),Optional.empty(),Set.of());
    }
    public BoxGeometrySnapshot snapshot(BinaryData payload){
        try{
            Object d=adapter.restore(payload);BoxUnion collision=NativeStates.boxes((VoxelShape)geometry.getMethod("shape",design).invoke(null,d));List<Box16> conservative=new ArrayList<>();
            for(var primitive:adapter.primitives(payload)){
                double x0=16,y0=16,z0=16,x1=0,y1=0,z1=0;for(var v:primitive.geometry().vertices()){x0=Math.min(x0,v.x());y0=Math.min(y0,v.y());z0=Math.min(z0,v.z());x1=Math.max(x1,v.x());y1=Math.max(y1,v.y());z1=Math.max(z1,v.z());}
                if(x0< -ConvexVolume.EPS||y0< -ConvexVolume.EPS||z0< -ConvexVolume.EPS||x1>16+ConvexVolume.EPS||y1>16+ConvexVolume.EPS||z1>16+ConvexVolume.EPS)throw new IllegalArgumentException("PB atomic geometry outside its frame");
                conservative.add(Box16.of(Math.max(0,(long)Math.floor(x0)),Math.max(0,(long)Math.floor(y0)),Math.max(0,(long)Math.floor(z0)),Math.min(16,(long)Math.ceil(x1)),Math.min(16,(long)Math.ceil(y1)),Math.min(16,(long)Math.ceil(z1))));
            }
            var mask=net.nicomar.nomoregrids.simulation.fluid.CavityCell.blocked(new BoxUnion(conservative));var occupation=MicroBoxes.pack(mask);return new BoxGeometrySnapshot(occupation,collision,occupation);
        }catch(ReflectiveOperationException ex){throw mismatch(ex);}
    }
    public BoxUnion support(BinaryData payload){try{return NativeStates.boxes((VoxelShape)geometry.getMethod("supportShape",design).invoke(null,adapter.restore(payload)));}catch(ReflectiveOperationException ex){throw mismatch(ex);}}
    public int designOrientation(PieceRecord p){try{return (int)design.getMethod("orientation").invoke(adapter.restore(p.geometryPayload()));}catch(ReflectiveOperationException ex){throw mismatch(ex);}}
    public PieceRecord oriented(PieceRecord p,int nativeOrientation){
        if(nativeOrientation<0||nativeOrientation>=24)throw new IllegalArgumentException("PB orientation");try{
            Object d=design.getMethod("withOrientation",int.class).invoke(adapter.restore(p.geometryPayload()),nativeOrientation);BinaryData payload=adapter.capture(d);
            return new PieceRecord(p.pieceId(),p.revision(),p.adapterId(),p.adapterVersion(),p.sourceBlockId(),p.sourceState(),p.origin16(),p.orientation(),p.pivot16(),snapshot(payload),payload,p.materialPayload(),p.itemPayload(),p.groupId(),p.flags());
        }catch(ReflectiveOperationException ex){throw mismatch(ex);}
    }
    private Object recoveryDesign(PieceRecord p)throws ReflectiveOperationException{
        Object d=adapter.restore(p.geometryPayload());if(p.orientation()==0)return d;Class<?> orientation=Class.forName("net.nicomar.paintableblocks.design.DesignOrientation");
        int external=-1;for(int candidate=0;candidate<24;candidate++){
            boolean matches=true;for(var axis:List.of(Direction6.EAST,Direction6.UP,Direction6.SOUTH))if(orientation.getMethod("direction",int.class,Direction.class).invoke(null,candidate,Direction.valueOf(axis.name()))!=Direction.valueOf(axis.rotated(p.orientation()).name())){matches=false;break;}
            if(matches){external=candidate;break;}
        }
        if(external<0)throw new IllegalArgumentException("PB rotation API mismatch");int combined=(int)orientation.getMethod("compose",int.class,int.class).invoke(null,external,(int)design.getMethod("orientation").invoke(d));return design.getMethod("withOrientation",int.class).invoke(d,combined);
    }
    public List<ItemStack> drops(ServerPlayer actor,PieceRecord piece){
        try{
            var state=NativeStates.resolve(piece);var detached=(BlockEntity)entity.getConstructor(BlockPos.class,BlockState.class).newInstance(BlockPos.ZERO,state);
            var envelope=JsonParser.parseString(new String(piece.itemPayload().copy(),StandardCharsets.UTF_8)).getAsJsonObject();
            if(envelope.has("native")){
                var tag=net.minecraft.nbt.CompoundTag.CODEC.parse(actor.registryAccess().createSerializationContext(JsonOps.INSTANCE),envelope.getAsJsonObject("native").get("blockEntity")).getOrThrow();
                detached.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,actor.registryAccess(),tag));
            }
            entity.getMethod("setDesign",design).invoke(detached,recoveryDesign(piece));
            var drops=net.minecraft.world.level.block.Block.getDrops(state,actor.level(),new BlockPos(Math.toIntExact(piece.origin16().cell().x()),Math.toIntExact(piece.origin16().cell().y()),Math.toIntExact(piece.origin16().cell().z())),detached,actor,actor.getMainHandItem());List<ItemStack> result=new ArrayList<>();
            for(var drop:drops){Object recovered=drop.get(designComponent);if(recovered!=null&&piece.itemPayload().size()>0){var template=ItemStack.CODEC.parse(actor.registryAccess().createSerializationContext(JsonOps.INSTANCE),JsonParser.parseString(new String(piece.itemPayload().copy(),StandardCharsets.UTF_8)).getAsJsonObject().get("item")).getOrThrow();if(template.getItem()==drop.getItem()){template.set(designComponent,recovered);drop=template.copyWithCount(drop.getCount());}}result.add(drop);}
            return List.copyOf(result);
        }catch(ReflectiveOperationException ex){throw mismatch(ex);}
    }
    private static JsonElement nativeData(BlockEntity be,HolderLookup.Provider lookup){var tag=be.saveWithoutMetadata(lookup);tag.remove("design");return net.minecraft.nbt.CompoundTag.CODEC.encodeStart(lookup.createSerializationContext(JsonOps.INSTANCE),tag).getOrThrow();}
    private static PieceRecord withItem(PieceRecord p,BinaryData item){return new PieceRecord(p.pieceId(),p.revision(),p.adapterId(),p.adapterVersion(),p.sourceBlockId(),p.sourceState(),p.origin16(),p.orientation(),p.pivot16(),p.geometry(),p.geometryPayload(),p.materialPayload(),item,p.groupId(),p.flags());}
    private static String hash(BinaryData data){return java.util.HexFormat.of().formatHex(net.nicomar.nomoregrids.network.SnapshotAssembler.sha256(data.copy()));}
    public boolean matchesOriginal(PieceRecord p,ServerLevel level,BlockPos position){
        if(p.itemPayload().size()==0)return false;try{
            var envelope=JsonParser.parseString(new String(p.itemPayload().copy(),StandardCharsets.UTF_8)).getAsJsonObject();if(envelope.get("schema").getAsInt()!=1||!envelope.has("native"))return false;
            var original=envelope.getAsJsonObject("native");var be=level.getBlockEntity(position);return entity.isInstance(be)&&(boolean)entity.getMethod("valid").invoke(be)&&original.get("identity").getAsString().equals(entity.getMethod("editIdentity").invoke(be).toString())&&original.get("designHash").getAsString().equals(hash(adapter.capture(entity.getMethod("design").invoke(be))))&&original.get("revision").getAsLong()==(long)entity.getMethod("revision").invoke(be)&&original.get("blockEntity").equals(nativeData(be,level.registryAccess()));
        }catch(ReflectiveOperationException|RuntimeException ex){return false;}
    }
    public record Edit(Optional<PieceRecord> after,List<ItemStack> inventory,Object nativePlan){public Edit{inventory=List.copyOf(inventory);}}
    @SuppressWarnings("unchecked")
    public Edit edit(ServerPlayer actor,InteractionHand hand,PieceRecord piece,BlockHitResult hit,List<ItemStack> inventory,boolean reverse){return editArea(actor,hand,piece,hit,inventory,reverse,null);}
    public Edit editArea(ServerPlayer actor,InteractionHand hand,PieceRecord piece,BlockHitResult hit,List<ItemStack> inventory,boolean reverse,Object first){
        try{
            int slot=hand==InteractionHand.MAIN_HAND?actor.getInventory().getSelectedSlot():net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND;ItemStack held=inventory.get(slot);if(!editingItem(held))throw new IllegalArgumentException("UNSUPPORTED_ADAPTER: PB tool");
            Object before=adapter.restore(piece.geometryPayload());Vec3 local=localPoint(piece,hit.getLocation());Direction face=Direction.valueOf(Direction6.valueOf(hit.getDirection().name()).rotated(Rotation.of(piece.orientation()).inverse().id()).name());
            var target=(Optional<Object>)geometry.getMethod("target",design,Vec3.class,Direction.class).invoke(null,before,local,face);Object selected=target.orElseThrow(()->new IllegalArgumentException("PB surface is not exposed"));Object nativePlan;
            if(brush.isInstance(held.getItem())){
                if(!(boolean)brush.getMethod("valid",ItemStack.class).invoke(held.getItem(),held))throw new IllegalArgumentException("INVALID_DATA: PB brush");
                Object content=brush.getMethod("content",ItemStack.class).invoke(held.getItem(),held);int paint=(int)content.getClass().getMethod("paint").invoke(content),units=(int)content.getClass().getMethod("units").invoke(content);
                Class<?> catalog=Class.forName("net.nicomar.paintableblocks.paint.PaintCatalog");if(!actor.isCreative()&&!(boolean)catalog.getMethod("byId",int.class).invoke(null,paint).getClass().getMethod("survivalAvailable").invoke(catalog.getMethod("byId",int.class).invoke(null,paint)))throw new IllegalArgumentException("INVENTORY_MISSING: paint unavailable");
                nativePlan=operations.getMethod("paint",design,List.class,int.class).invoke(null,before,first==null?List.of(selected):(List<?>)operations.getMethod("area",design,surface,surface).invoke(null,before,first,selected),paint);int cost=(int)plan.getMethod("units").invoke(nativePlan);if(!actor.isCreative()){if(cost>units)throw new IllegalArgumentException("INVENTORY_MISSING: pigment");held.set(brushComponent,content.getClass().getMethod("withUnits",int.class).invoke(content,units-cost));}
            }else{Object action=tool.getMethod("action").invoke(held.getItem());nativePlan=operations.getMethod("tool",design,surface,action.getClass(),Direction.class,boolean.class).invoke(null,before,selected,action,face,reverse);}
            if(!(boolean)plan.getMethod("changes").invoke(nativePlan))throw new IllegalArgumentException("PB edit has no changes");Object after=plan.getMethod("after").invoke(nativePlan);
            if((boolean)design.getMethod("empty").invoke(after))return new Edit(Optional.empty(),inventory,nativePlan);
            BinaryData data=adapter.capture(after);var properties=new HashMap<>(piece.sourceState());properties.put("cut",Boolean.toString(!(boolean)design.getMethod("complete").invoke(after)));
            var next=new PieceRecord(piece.pieceId(),Math.incrementExact(piece.revision()),piece.adapterId(),piece.adapterVersion(),piece.sourceBlockId(),properties,piece.origin16(),piece.orientation(),piece.pivot16(),snapshot(data),data,piece.materialPayload(),piece.itemPayload(),piece.groupId(),piece.flags());
            return new Edit(Optional.of(next),inventory,nativePlan);
        }catch(ReflectiveOperationException ex){throw mismatch(ex);}
    }
    public Edit volume(PieceRecord piece,Box16 region,Direction facing,boolean upper,boolean slope,List<ItemStack> inventory){
        try{Object before=adapter.restore(piece.geometryPayload()),after;var selected=new ArrayList<Integer>();
            if(!slope){int n=(int)design.getMethod("resolution").invoke(before);var bits=new BitSet();Class<?> orientation=Class.forName("net.nicomar.paintableblocks.design.DesignOrientation");int nativeRotation=(int)design.getMethod("orientation").invoke(before);
                if(n!=1)for(int cell=0;cell<n*n*n;cell++)if((boolean)design.getMethod("occupied",int.class).invoke(before,cell)){
                    var center=(Vec3)orientation.getMethod("rotate",int.class,Vec3.class).invoke(null,nativeRotation,new Vec3((cell%n+0.5)/n,(cell/n%n+0.5)/n,(cell/(n*n)+0.5)/n));
                    Vec3 point=worldPoint(piece,center);if(point.x*16>=region.min().x()&&point.x*16<region.max().x()&&point.y*16>=region.min().y()&&point.y*16<region.max().y()&&point.z*16>=region.min().z()&&point.z*16<region.max().z()){bits.set(cell);selected.add(cell);}
                }after=design.getMethod("remove",BitSet.class).invoke(before,bits);
            }else{
                long length=facing.getAxis()==Direction.Axis.X?region.max().x()-region.min().x():region.max().z()-region.min().z(),height=region.max().y()-region.min().y();long a=-height*facing.getStepX(),b=length,c=-height*facing.getStepZ();long start=facing.getAxis()==Direction.Axis.X?(facing.getStepX()>0?region.min().x():region.max().x()):(facing.getStepZ()>0?region.min().z():region.max().z());long offset=b*region.min().y()+(a!=0?a:c)*start;if(upper){a=-a;b=-b;c=-c;offset=-offset;}
                Class<?> orientation=Class.forName("net.nicomar.paintableblocks.design.DesignOrientation");int nativeRotation=(int)design.getMethod("orientation").invoke(before);var zero=worldPoint(piece,(Vec3)orientation.getMethod("rotate",int.class,Vec3.class).invoke(null,nativeRotation,Vec3.ZERO));long[] coefficients=new long[4];
                for(int axis=0;axis<3;axis++){Direction nativeAxis=(Direction)orientation.getMethod("direction",int.class,Direction.class).invoke(null,nativeRotation,new Direction[]{Direction.EAST,Direction.UP,Direction.SOUTH}[axis]);Direction6 world=Direction6.valueOf(nativeAxis.name()).rotated(piece.orientation());coefficients[axis]=16*(a*world.x+b*world.y+c*world.z);}
                coefficients[3]=offset-Math.round(16*(a*zero.x+b*zero.y+c*zero.z));long gcd=0;for(long value:coefficients)gcd=gcd(gcd,Math.abs(value));for(int i=0;i<4;i++)coefficients[i]/=gcd;
                Class<?> plane=Class.forName("net.nicomar.paintableblocks.design.SlopePlane");Object cut=plane.getConstructor(int.class,int.class,int.class,int.class).newInstance(Math.toIntExact(coefficients[0]),Math.toIntExact(coefficients[1]),Math.toIntExact(coefficients[2]),Math.toIntExact(coefficients[3]));after=design.getMethod("withSlope",plane).invoke(before,cut);
            }
            Object proposal=plan.getConstructor(design,design,List.class,List.class,int.class).newInstance(before,after,selected,List.of(),0);return result(piece,after,inventory,proposal);
        }catch(ReflectiveOperationException ex){throw mismatch(ex);}
    }
    private static long gcd(long a,long b){while(b!=0){long t=a%b;a=b;b=t;}return a;}
    private static Vec3 worldPoint(PieceRecord p,Vec3 point){var r=Rotation.of(p.orientation());var x=r.apply(new GridPoint(1,0,0));var y=r.apply(new GridPoint(0,1,0));var z=r.apply(new GridPoint(0,0,1));double a=point.x*16-p.pivot16().x(),b=point.y*16-p.pivot16().y(),c=point.z*16-p.pivot16().z();return new Vec3((x.x()*a+y.x()*b+z.x()*c+p.pivot16().x()+p.origin16().x())/16,(x.y()*a+y.y()*b+z.y()*c+p.pivot16().y()+p.origin16().y())/16,(x.z()*a+y.z()*b+z.z()*c+p.pivot16().z()+p.origin16().z())/16);}
    private Edit result(PieceRecord piece,Object after,List<ItemStack> inventory,Object proposal)throws ReflectiveOperationException{
        if(!(boolean)plan.getMethod("changes").invoke(proposal))return new Edit(Optional.of(piece),inventory,proposal);
        if((boolean)design.getMethod("empty").invoke(after))return new Edit(Optional.empty(),inventory,proposal);var data=adapter.capture(after);var properties=new HashMap<>(piece.sourceState());properties.put("cut",Boolean.toString(!(boolean)design.getMethod("complete").invoke(after)));
        return new Edit(Optional.of(new PieceRecord(piece.pieceId(),Math.incrementExact(piece.revision()),piece.adapterId(),piece.adapterVersion(),piece.sourceBlockId(),properties,piece.origin16(),piece.orientation(),piece.pivot16(),snapshot(data),data,piece.materialPayload(),piece.itemPayload(),piece.groupId(),piece.flags())),inventory,proposal);
    }
    public void permission(ServerPlayer actor,InteractionHand hand,PieceRecord before,BlockPos position,Object proposal){
        try{
            Class<?> event=Class.forName("net.nicomar.paintableblocks.event.PaintableBlockEditEvent");var value=(Event)event.getConstructor(net.minecraft.world.level.Level.class,BlockPos.class,BlockState.class,net.minecraft.world.entity.player.Player.class,InteractionHand.class,plan).newInstance(actor.level(),position,NativeStates.resolve(before),actor,hand,proposal);
            NeoForge.EVENT_BUS.post(value);if(((ICancellableEvent)value).isCanceled())throw new IllegalArgumentException("PROTECTED");
            Object old=plan.getMethod("before").invoke(proposal),next=plan.getMethod("after").invoke(proposal);
            if(!(boolean)design.getMethod("empty").invoke(next)&&(int)design.getMethod("count").invoke(next)<(int)design.getMethod("count").invoke(old)){
                var breaking=new net.nicomar.nomoregrids.event.PieceBreakPermissionEvent(actor.level(),position,NativeStates.resolve(before),actor,before.pieceId());NeoForge.EVENT_BUS.post(breaking);if(breaking.isCanceled())throw new IllegalArgumentException("PROTECTED");
            }
        }catch(ReflectiveOperationException ex){throw mismatch(ex);}
    }
    private static Vec3 localPoint(PieceRecord p,Vec3 world){
        var delta=new Vec3(world.x*16-p.origin16().x()-p.pivot16().x(),world.y*16-p.origin16().y()-p.pivot16().y(),world.z*16-p.origin16().z()-p.pivot16().z());Rotation inverse=Rotation.of(p.orientation()).inverse();GridPoint x=inverse.apply(new GridPoint(1,0,0)),y=inverse.apply(new GridPoint(0,1,0)),z=inverse.apply(new GridPoint(0,0,1));
        return new Vec3((x.x()*delta.x+y.x()*delta.y+z.x()*delta.z+p.pivot16().x())/16,(x.y()*delta.x+y.y()*delta.y+z.y()*delta.z+p.pivot16().y())/16,(x.z()*delta.x+y.z()*delta.y+z.z()*delta.z+p.pivot16().z())/16);
    }
    private static IllegalArgumentException mismatch(ReflectiveOperationException ex){return new IllegalArgumentException("UNSUPPORTED_ADAPTER: PB native API mismatch; original retained",ex);}
}
