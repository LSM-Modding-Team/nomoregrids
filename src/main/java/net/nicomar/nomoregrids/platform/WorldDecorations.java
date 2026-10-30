package net.nicomar.nomoregrids.platform;

import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.operation.OperationPlan;
import net.nicomar.nomoregrids.network.*;

/** Captures private native data once and projects only the documented render fields to clients. */
public final class WorldDecorations {
    public static final String ADAPTER="nomoregrids:decoration";
    private WorldDecorations(){}
    public static boolean piece(PieceRecord p){return p.adapterId().value().equals(ADAPTER);}
    public static boolean eligible(BlockState state){return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("minecraft")&&(state.getBlock() instanceof SignBlock||state.getBlock() instanceof AbstractBannerBlock||state.getBlock() instanceof AbstractSkullBlock);}
    public static BlockState placement(net.minecraft.world.item.context.BlockPlaceContext context,Block preferred){
        var side=context.getClickedFace();Block selected=preferred;
        for(var candidate:BuiltInRegistries.BLOCK){
            if(candidate.asItem()!=context.getItemInHand().getItem()||!eligible(candidate.defaultBlockState()))continue;
            boolean wall=candidate instanceof WallSignBlock||candidate instanceof WallHangingSignBlock||candidate instanceof WallBannerBlock||candidate instanceof WallSkullBlock;
            boolean ceiling=candidate instanceof CeilingHangingSignBlock;
            if(side.getAxis()!=Direction.Axis.Y&&wall||side==Direction.DOWN&&ceiling||side==Direction.UP&&!wall&&!ceiling){selected=candidate;break;}
        }
        var state=selected.getStateForPlacement(context);if(state!=null)return state;
        // The technical host cannot answer native survival; exact anchor validation follows the plan.
        var props=new HashMap<>(NativeStates.properties(selected.defaultBlockState()));
        if(props.containsKey("facing"))props.put("facing",(side.getAxis()==Direction.Axis.Y?context.getHorizontalDirection().getOpposite():side).getName());
        if(props.containsKey("rotation"))props.put("rotation",Integer.toString(Math.floorMod((int)Math.floor((context.getRotation()+180)*16/360+0.5),16)));
        return NativeStates.resolve(new ResourceId(BuiltInRegistries.BLOCK.getKey(selected).toString()),props);
    }
    private static JsonElement encode(Level level,CompoundTag tag){return CompoundTag.CODEC.encodeStart(level.registryAccess().createSerializationContext(JsonOps.INSTANCE),tag).getOrThrow();}
    private static CompoundTag decode(Level level,JsonElement data){return CompoundTag.CODEC.parse(level.registryAccess().createSerializationContext(JsonOps.INSTANCE),data).getOrThrow();}
    private static BinaryData binary(JsonElement data){return new BinaryData(data.toString().getBytes(StandardCharsets.UTF_8));}
    private static JsonElement json(BinaryData data){return JsonParser.parseString(new String(data.copy(),StandardCharsets.UTF_8));}
    public static BlockEntity detached(PieceRecord p,Level level,boolean publicView){
        var state=NativeStates.resolve(p);if(!eligible(state)||!(state.getBlock() instanceof EntityBlock factory))throw new IllegalArgumentException("UNSUPPORTED_ADAPTER: decoration");var be=factory.newBlockEntity(WorldService.pos(p.origin16().cell()),state);if(be==null)throw new IllegalArgumentException("Missing decoration entity");be.setLevel(level);
        var data=publicView?json(p.geometryPayload()).getAsJsonObject().get("data"):json(p.itemPayload()).getAsJsonObject().get("native");be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),decode(level,data)));return be;
    }
    public static PieceRecord capture(BlockState state,Level level,BlockPos pos,UUID id,ItemStack placement){
        if(!eligible(state))throw new IllegalArgumentException("UNSUPPORTED_ADAPTER");BlockEntity be;boolean nativeSource=placement==null;
        if(nativeSource)be=level.getBlockEntity(pos);else{be=((EntityBlock)state.getBlock()).newBlockEntity(pos,state);if(be==null)throw new IllegalArgumentException("Missing decoration");be.setLevel(level);var entityData=placement.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);if(entityData!=null&&!entityData.loadInto(be,level.registryAccess()))throw new IllegalArgumentException("INVALID_DATA: decoration item type");be.applyComponentsFromItemStack(placement);}
        if(!(be instanceof SignBlockEntity||be instanceof BannerBlockEntity||be instanceof SkullBlockEntity))throw new IllegalArgumentException("INVALID_DATA: decoration class");
        var exactSelection=state.getShape(level,pos);var exactCollision=state.getCollisionShape(level,pos);var selection=conservative(exactSelection);var collision=conservative(exactCollision);var payload=new JsonObject();payload.addProperty("schema",1);var full=be.saveWithoutMetadata(level.registryAccess());payload.add("native",encode(level,full));var original=new JsonObject();NativeStates.properties(state).forEach(original::addProperty);payload.add("state",original);if(nativeSource)payload.addProperty("originalHash",java.util.HexFormat.of().formatHex(SnapshotAssembler.sha256(encode(level,full).toString().getBytes(StandardCharsets.UTF_8))));
        var source=new ResourceId(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());return new PieceRecord(id,0,new ResourceId(ADAPTER),1,source,NativeStates.properties(state),new GridPoint(pos.getX()*16L,pos.getY()*16L,pos.getZ()*16L),0,GridPoint.BLOCK_CENTER,new BoxGeometrySnapshot(selection,collision,selection),geometry(level,full,exactSelection,exactCollision),BinaryData.EMPTY,binary(payload),Optional.empty(),Set.of());
    }
    private static net.nicomar.nomoregrids.geometry.BoxUnion conservative(net.minecraft.world.phys.shapes.VoxelShape shape){var boxes=new ArrayList<net.nicomar.nomoregrids.geometry.Box16>();for(var b:shape.toAabbs())boxes.add(net.nicomar.nomoregrids.geometry.Box16.of((long)Math.floor(b.minX*16),(long)Math.floor(b.minY*16),(long)Math.floor(b.minZ*16),(long)Math.ceil(b.maxX*16),(long)Math.ceil(b.maxY*16),(long)Math.ceil(b.maxZ*16)));return new net.nicomar.nomoregrids.geometry.BoxUnion(boxes);}
    private static JsonArray exact(net.minecraft.world.phys.shapes.VoxelShape shape){var boxes=new JsonArray();for(var b:shape.toAabbs()){var box=new JsonArray();for(double n:new double[]{b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ})box.add(n*16);boxes.add(box);}return boxes;}
    private static BinaryData geometry(Level level,CompoundTag tag,net.minecraft.world.phys.shapes.VoxelShape selection,net.minecraft.world.phys.shapes.VoxelShape collision){var json=new JsonObject();json.add("data",json(publicData(level,tag)));json.add("selection",exact(selection));json.add("collision",exact(collision));return binary(json);}
    private static BinaryData publicData(Level level,CompoundTag tag){var projected=tag.copy();for(String key:Set.copyOf(projected.keySet()))if(!Set.of("front_text","back_text","is_waxed","patterns","Patterns","profile","note_block_sound","custom_name").contains(key))projected.remove(key);return binary(encode(level,projected));}
    public static Map<String,String> original(PieceRecord p){var result=new HashMap<String,String>();json(p.itemPayload()).getAsJsonObject().getAsJsonObject("state").entrySet().forEach(e->result.put(e.getKey(),e.getValue().getAsString()));return result;}
    public static boolean matches(PieceRecord p,ServerLevel level,BlockPos pos){var be=level.getBlockEntity(pos);if(be==null)return false;var payload=json(p.itemPayload()).getAsJsonObject();return payload.has("originalHash")&&payload.get("originalHash").getAsString().equals(java.util.HexFormat.of().formatHex(SnapshotAssembler.sha256(encode(level,be.saveWithoutMetadata(level.registryAccess())).toString().getBytes(StandardCharsets.UTF_8))));}
    public static List<ItemStack> drops(PieceRecord p,ServerLevel level,ServerPlayer actor){var be=detached(p,level,false);return Block.getDrops(NativeStates.resolve(p),level,be.getBlockPos(),be,actor,actor==null?ItemStack.EMPTY:actor.getMainHandItem());}
    private static PieceRecord updated(PieceRecord p,BlockEntity be,Level level){var full=be.saveWithoutMetadata(level.registryAccess());var payload=json(p.itemPayload()).getAsJsonObject();payload.add("native",encode(level,full));return new PieceRecord(p.pieceId(),Math.incrementExact(p.revision()),p.adapterId(),p.adapterVersion(),p.sourceBlockId(),p.sourceState(),p.origin16(),p.orientation(),p.pivot16(),p.geometry(),updatedGeometry(p,level,full),p.materialPayload(),binary(payload),p.groupId(),p.flags());}
    private static BinaryData updatedGeometry(PieceRecord p,Level level,CompoundTag tag){var geometry=json(p.geometryPayload()).getAsJsonObject();geometry.add("data",json(publicData(level,tag)));return binary(geometry);}
    public static void use(WorldService service,ServerPlayer player,InteractionHand hand,PieceRecord p,net.minecraft.world.phys.BlockHitResult hit)throws IOException{
        var be=detached(p,player.level(),false);if(!(be instanceof SignBlockEntity sign))return;var local=Direction6.valueOf(hit.getDirection().name()).rotated(net.nicomar.nomoregrids.geometry.Rotation.of(p.orientation()).inverse().id());double angle=Math.toRadians(((SignBlock)NativeStates.resolve(p).getBlock()).getYRotationDegrees(NativeStates.resolve(p)));boolean front=local.x*(-Math.sin(angle))+local.z*Math.cos(angle)>=0;var held=player.getItemInHand(hand);
        if(sign.isWaxed()){sign.executeClickCommandsIfPresent(player.level(),player,be.getBlockPos(),front);return;}
        var slots=NativeInventory.copy(player);var before=NativeInventory.encode(player,slots);int index=hand==InteractionHand.MAIN_HAND?player.getInventory().getSelectedSlot():net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND;boolean changed;
        if(held.getItem() instanceof DyeItem dye)changed=sign.updateText(t->t.setColor(dye.getDyeColor()),front);else if(held.is(Items.GLOW_INK_SAC))changed=sign.updateText(t->t.setHasGlowingText(true),front);else if(held.is(Items.INK_SAC))changed=sign.updateText(t->t.setHasGlowingText(false),front);else if(held.is(Items.HONEYCOMB))changed=sign.setWaxed(true);else{var text=new ArrayList<String>();for(var line:sign.getText(front).getMessages(false))text.add(line.getString());net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,new SignEditorPayload(p.pieceId(),p.revision(),front,text));return;}
        if(changed){if(!player.isCreative())slots.get(index).shrink(1);service.executePrepared(new OperationPlan(UUID.randomUUID(),player.getUUID(),OperationPlan.Kind.EDIT,List.of(new OperationPlan.Change(p.pieceId(),Optional.of(p),Optional.of(updated(p,be,player.level())))),before,NativeInventory.encode(player,slots)));}
    }
    public static void text(ServerPlayer player,SignEditorPayload request){
        player.getTextFilter().processMessageBundle(request.lines()).thenAccept(filtered->player.level().getServer().execute(()->{
            try{var service=NmgRuntime.require(player.level());var p=service.snapshot().records().get(request.piece());if(p==null||p.revision()!=request.revision()||!piece(p))throw new IllegalArgumentException("STALE_REVISION");var ray=LoadedRay.pick(player);if(!(ray instanceof net.minecraft.world.phys.BlockHitResult hit)||!p.worldOccupation().cells(8).contains(new CellPos(hit.getBlockPos().getX(),hit.getBlockPos().getY(),hit.getBlockPos().getZ())))throw new IllegalArgumentException("UNREACHABLE");if(!(player.level().getBlockEntity(hit.getBlockPos()) instanceof net.nicomar.nomoregrids.world.CompositeCellEntity cell)||ComponentSelection.select(player,cell.view(),0).filter(selected->selected.pieceId().equals(p.pieceId())).isEmpty())throw new IllegalArgumentException("OCCLUDED");var be=detached(p,player.level(),false);if(!(be instanceof SignBlockEntity sign)||sign.isWaxed())throw new IllegalArgumentException("PROTECTED");sign.setAllowedPlayerEditor(player.getUUID());sign.updateSignText(player,request.front(),filtered);var inventory=NativeInventory.encode(player,NativeInventory.copy(player));service.executePrepared(new OperationPlan(UUID.randomUUID(),player.getUUID(),OperationPlan.Kind.EDIT,List.of(new OperationPlan.Change(p.pieceId(),Optional.of(p),Optional.of(updated(p,be,player.level())))),inventory,inventory));}
            catch(IOException|RuntimeException ex){player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("NMG: "+ex.getMessage()));}
        }));
    }
}
