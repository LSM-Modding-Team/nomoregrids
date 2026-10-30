package net.nicomar.nomoregrids.platform;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.operation.OperationPlan;
import net.nicomar.nomoregrids.persistence.*;
import net.nicomar.nomoregrids.simulation.functional.FunctionalStore;

/** Explicit native furnace/barrel bridge. Detached native objects are temporary plans, never installed as world BEs. */
public final class WorldFunctionals {
    public static final String ADAPTER="nomoregrids:functional";
    private static final Set<String> IDS=Set.of("minecraft:barrel","minecraft:furnace","minecraft:blast_furnace","minecraft:smoker");
    private record MenuSession(WorldFunctionals owner,UUID piece,FunctionalContainer container){}
    private static final Map<AbstractContainerMenu,MenuSession> MENUS=new WeakHashMap<>();
    private static final ThreadLocal<Boolean> COPY=ThreadLocal.withInitial(()->false);
    public static boolean planningCopy(){return COPY.get();}
    private static final ThreadLocal<FunctionalHandle> MENU_TARGET=new ThreadLocal<>();
    private static final Map<BlockPos,net.nicomar.nomoregrids.network.FunctionalMenuPayload> CLIENT_CONTEXT=new LinkedHashMap<>();
    private record ClientMenu(Level level,BlockPos position,UUID piece){}
    private static final Map<AbstractContainerMenu,ClientMenu> CLIENT_MENUS=new WeakHashMap<>();
    public static void clientContext(net.nicomar.nomoregrids.network.FunctionalMenuPayload context){if(CLIENT_CONTEXT.size()>=16)CLIENT_CONTEXT.remove(CLIENT_CONTEXT.keySet().iterator().next());CLIENT_CONTEXT.put(context.position(),context);}
    public static BlockEntity menuEntity(AbstractContainerMenu menu,Level level,BlockPos pos){var target=MENU_TARGET.get();if(target!=null&&target.getBlockPos().equals(pos))return target.entity();if(level.isClientSide()&&level.getBlockEntity(pos) instanceof net.nicomar.nomoregrids.world.CompositeCellEntity cell&&cell.valid()){var context=CLIENT_CONTEXT.get(pos);if(context!=null&&cell.view().replicas().stream().anyMatch(p->p.pieceId().equals(context.piece()))){var state=NativeStates.resolve(new ResourceId(context.block()),context.state());if(PaintableFunctionals.eligible(state)&&state.getBlock() instanceof EntityBlock factory){var be=factory.newBlockEntity(pos,state);if(be!=null){be.setLevel(level);CLIENT_MENUS.put(menu,new ClientMenu(level,pos,context.piece()));return be;}}}}return level.getBlockEntity(pos);}
    public static Optional<Boolean> paintableMenuValid(AbstractContainerMenu menu,Player player){var session=MENUS.get(menu);if(session!=null)return Optional.of(session.container.stillValid(player));var client=CLIENT_MENUS.get(menu);if(client==null)return Optional.empty();if(player.level()!=client.level||!client.level.isLoaded(client.position))return Optional.of(false);return Optional.of(client.level.getBlockEntity(client.position) instanceof net.nicomar.nomoregrids.world.CompositeCellEntity cell&&cell.valid()&&cell.view().replicas().stream().anyMatch(p->p.pieceId().equals(client.piece)));}
    public static BlockEntity scopedEntity(Level level,BlockPos pos){var target=MENU_TARGET.get();return target!=null&&target.getBlockPos().equals(pos)?target.entity():level.getBlockEntity(pos);}
    private static final ThreadLocal<Stage> STAGE=new ThreadLocal<>();
    private record Stage(ServerPlayer player,List<ItemStack> drops){}
    private final Map<UUID,Long> simulated=new HashMap<>();
    public static boolean managed(AbstractContainerMenu menu){return MENUS.containsKey(menu);}
    private final ServerLevel level;private final WorldService service;private int roundRobin;
    public WorldFunctionals(ServerLevel level,WorldService service){this.level=level;this.service=service;}
    public static boolean eligible(BlockState state){return (IDS.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())||PaintableFunctionals.eligible(state))&&state.getFluidState().isEmpty();}
    public static boolean piece(PieceRecord p){return p.adapterId().value().equals(ADAPTER);}
    private static String key(UUID id){return "machine:"+id;}
    private static BinaryData tag(ServerLevel level,CompoundTag tag){return new BinaryData(CompoundTag.CODEC.encodeStart(level.registryAccess().createSerializationContext(JsonOps.INSTANCE),tag).getOrThrow().toString().getBytes(StandardCharsets.UTF_8));}
    private static CompoundTag tag(ServerLevel level,BinaryData data){return CompoundTag.CODEC.parse(level.registryAccess().createSerializationContext(JsonOps.INSTANCE),JsonParser.parseString(new String(data.copy(),StandardCharsets.UTF_8))).getOrThrow();}
    public static PieceRecord placement(BlockState state,Level world,BlockPos at,UUID id){return capture(state,world,at,id,false);}
    public static PieceRecord placement(BlockState state,Level world,BlockPos at,UUID id,ItemStack stack){var p=placement(state,world,at,id);if(!PaintableFunctionals.eligible(state))return p;var data=new BinaryData(ItemStack.CODEC.encodeStart(world.registryAccess().createSerializationContext(JsonOps.INSTANCE),stack.copyWithCount(1)).getOrThrow().toString().getBytes(StandardCharsets.UTF_8));return new PieceRecord(p.pieceId(),0,p.adapterId(),1,p.sourceBlockId(),p.sourceState(),p.origin16(),p.orientation(),p.pivot16(),p.geometry(),p.geometryPayload(),p.materialPayload(),data,p.groupId(),p.flags());}
    public static PieceRecord capture(BlockState state,Level world,BlockPos at,UUID id){return capture(state,world,at,id,true);}
    private static PieceRecord capture(BlockState state,Level world,BlockPos at,UUID id,boolean nativeSource){
        if(!eligible(state))throw new IllegalArgumentException("UNSUPPORTED_ADAPTER: functional profile");var selection=NativeStates.boxes(state.getShape(world,at));var collision=NativeStates.boxes(state.getCollisionShape(world,at));BinaryData original=BinaryData.EMPTY;
        if(nativeSource&&world instanceof ServerLevel server&&world.getBlockEntity(at)!=null)original=nativeDigest(server,world.getBlockEntity(at));
        var source=new ResourceId(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());return new PieceRecord(id,0,new ResourceId(ADAPTER),1,source,NativeStates.properties(state),new GridPoint(at.getX()*16L,at.getY()*16L,at.getZ()*16L),0,GridPoint.BLOCK_CENTER,new BoxGeometrySnapshot(selection,collision,selection),new NativeStateImage(source,NativeStates.properties(state)).encode(),BinaryData.EMPTY,original,Optional.empty(),Set.of());
    }
    public static void clearTransferredNative(ServerLevel level,BlockEntity entity){if(!(entity.getBlockState().getBlock() instanceof EntityBlock factory))throw new IllegalArgumentException("Transferred entity factory missing");var empty=factory.newBlockEntity(entity.getBlockPos(),entity.getBlockState());if(empty==null)throw new IllegalArgumentException("Transferred entity factory failed");entity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),empty.saveWithoutMetadata(level.registryAccess())));}
    public static BinaryData nativeDigest(ServerLevel level,BlockEntity be){var json=JsonParser.parseString(new String(tag(level,be.saveWithoutMetadata(level.registryAccess())).copy(),StandardCharsets.UTF_8));return new BinaryData(net.nicomar.nomoregrids.network.SnapshotAssembler.sha256(CanonicalJson.bytes(json)));}
    public static boolean matchesDigest(ServerLevel level,BlockEntity be,BinaryData digest){return digest.equals(nativeDigest(level,be))||Arrays.equals(digest.copy(),net.nicomar.nomoregrids.network.SnapshotAssembler.sha256(tag(level,be.saveWithoutMetadata(level.registryAccess())).copy()));}
    public boolean matchesNative(PieceRecord p,BlockPos at){return p.itemPayload().size()==32&&level.getBlockEntity(at)!=null&&matchesDigest(level,level.getBlockEntity(at),p.itemPayload());}
    private FunctionalHandle fresh(PieceRecord p){
        var state=NativeStates.resolve(p);if(!eligible(state)||!(state.getBlock() instanceof EntityBlock factory))throw new IllegalArgumentException("UNSUPPORTED_ADAPTER: native functional class");var be=factory.newBlockEntity(WorldService.pos(p.origin16().cell()),state);if(!(be instanceof BaseContainerBlockEntity)&&!(be!=null&&PaintableFunctionals.entity(be)))throw new IllegalArgumentException("UNSUPPORTED_ADAPTER: native container");be.setLevel(level);if(p.itemPayload().size()>0&&p.itemPayload().size()!=32&&PaintableFunctionals.entity(be)){var stack=ItemStack.CODEC.parse(level.registryAccess().createSerializationContext(JsonOps.INSTANCE),JsonParser.parseString(new String(p.itemPayload().copy(),StandardCharsets.UTF_8))).getOrThrow();be.applyComponentsFromItemStack(stack);}return new FunctionalHandle(be);
    }
    FunctionalStore.State capture(PieceRecord p,FunctionalHandle be,long revision){boolean previousCopy=COPY.get();COPY.set(true);try{
        if(PaintableFunctionals.entity(be.entity())&&!PaintableFunctionals.valid(be.entity()))throw new IllegalArgumentException("INVALID_DATA: PB functional state");var slots=new ArrayList<BinaryData>();var ops=level.registryAccess().createSerializationContext(JsonOps.INSTANCE);for(int i=0;i<be.getContainerSize();i++)slots.add(new BinaryData(ItemStack.OPTIONAL_CODEC.encodeStart(ops,be.getItem(i)).getOrThrow().toString().getBytes(StandardCharsets.UTF_8)));
        var nbt=be.saveWithoutMetadata(level.registryAccess());nbt.remove("Items");nbt.remove("items");PaintableFunctionals.stripInventory(be.entity(),nbt);return new FunctionalStore.State(p.pieceId(),revision,slots,service.effects().containsKey(key(p.pieceId()))?state(p).energy():0,tag(level,nbt));}finally{COPY.set(previousCopy);}
    }
    BinaryData encode(PieceRecord p,FunctionalStore.State state){try{return new BinaryData(SimulationCodec.encode(new SimulationSnapshot(p.ownerSection(),0,Map.of(),Map.of(p.pieceId(),state),Map.of(),Map.of())));}catch(IOException ex){throw new IllegalArgumentException("COMPLEXITY_LIMIT: functional checkpoint",ex);}}
    FunctionalStore.State state(PieceRecord p){var value=service.effects().get(key(p.pieceId()));if(value==null)throw new IllegalArgumentException("INVALID_DATA: missing functional authority");try{return SimulationCodec.decode(value.data().copy()).functionals().get(p.pieceId());}catch(IOException ex){throw new IllegalArgumentException("INVALID_DATA: functional checkpoint",ex);}}
    public FunctionalHandle detached(PieceRecord p){return detached(p,state(p));}
    FunctionalHandle detached(PieceRecord p,FunctionalStore.State state){boolean previousCopy=COPY.get();COPY.set(true);try{
        var be=fresh(p);be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),tag(level,state.machine())));if(be.getContainerSize()!=state.slots().size())throw new IllegalArgumentException("INVALID_DATA: functional layout");var ops=level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        for(int i=0;i<state.slots().size();i++)be.setItem(i,ItemStack.OPTIONAL_CODEC.parse(ops,JsonParser.parseString(new String(state.slots().get(i).copy(),StandardCharsets.UTF_8))).getOrThrow());
        if(!PaintableFunctionals.production(be.entity())){var combined=be.saveWithoutMetadata(level.registryAccess());combined.merge(tag(level,state.machine()));be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),combined));}return be;}finally{COPY.set(previousCopy);}
    }
    public OperationPlan prepare(OperationPlan plan){
        var effects=new ArrayList<>(plan.effects());for(var c:plan.changes()){
            if(c.after().isPresent()&&piece(c.after().orElseThrow())){
                var p=c.after().orElseThrow();var value=service.effects().get(key(p.pieceId()));if(value==null){FunctionalHandle be;
                    var actual=level.getBlockEntity(WorldService.pos(c.nativeSource().orElse(p.origin16().cell())));if(actual!=null&&(actual instanceof BaseContainerBlockEntity||PaintableFunctionals.entity(actual))&&matchesNative(p,actual.getBlockPos())){be=fresh(p);be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),actual.saveWithoutMetadata(level.registryAccess())));var source=c.nativeSource().orElse(p.origin16().cell());var key=WorldService.nativeCaptureKey(source);var previous=service.effects().get(key);var data=new NativeCaptureStamp(p.sourceBlockId(),p.itemPayload()).encode();if(previous==null||!previous.data().equals(data))effects.add(new OperationPlan.Effect(key,source,Optional.ofNullable(previous).map(WorldEffectStore.Value::data),Optional.of(data)));}else if(p.itemPayload().size()!=32)be=fresh(p);else throw new IllegalArgumentException("STALE_REVISION: native inventory");
                    effects.add(new OperationPlan.Effect(key(p.pieceId()),p.origin16().cell(),Optional.empty(),Optional.of(encode(p,capture(p,be,0)))));
                }else if(!value.owner().equals(p.origin16().cell())){
                    var pending=effects.stream().filter(e->e.key().equals(key(p.pieceId()))).findFirst();var migrated=state(p);if(pending.isPresent()&&pending.orElseThrow().after().isPresent())try{migrated=SimulationCodec.decode(pending.orElseThrow().after().orElseThrow().copy()).functionals().get(p.pieceId());}catch(IOException ex){throw new IllegalArgumentException("INVALID_DATA: migrating functional state",ex);}var data=encode(p,migrated);effects.removeIf(e->e.key().equals(key(p.pieceId())));effects.add(new OperationPlan.Effect(key(p.pieceId()),p.origin16().cell(),Optional.of(value.data()),Optional.of(data),value.owner()));
                }
            }else if(c.before().isPresent()&&piece(c.before().orElseThrow())){var p=c.before().orElseThrow();var old=service.effects().get(key(p.pieceId()));if(old==null)throw new IllegalArgumentException("INVALID_DATA: missing functional authority");effects.add(new OperationPlan.Effect(key(p.pieceId()),old.owner(),Optional.of(old.data()),Optional.empty()));}
        }
        return new OperationPlan(plan.id(),plan.actor(),plan.kind(),plan.changes(),plan.resourcesBefore(),plan.resourcesAfter(),effects);
    }
    public List<ItemStack> drops(PieceRecord p,ServerPlayer player){var be=detached(p);if(PaintableFunctionals.entity(be.entity())){var item=PaintableFunctionals.item(be.entity());return player!=null&&player.isCreative()&&ItemStack.isSameItemSameComponents(item,NativeStates.resolve(p).getBlock().asItem().getDefaultInstance())?List.of():List.of(item);}var result=new ArrayList<ItemStack>();for(int i=0;i<be.getContainerSize();i++){result.add(be.getItem(i).copy());be.setItem(i,ItemStack.EMPTY);}if(player==null||!player.isCreative())result.addAll(Block.getDrops(NativeStates.resolve(p),level,be.getBlockPos(),be.entity(),player,player==null?ItemStack.EMPTY:player.getMainHandItem()));return result.stream().filter(s->!s.isEmpty()).toList();}
    private OperationPlan.Effect update(PieceRecord p,FunctionalHandle be){var old=service.effects().get(key(p.pieceId()));var previous=state(p);var next=capture(p,be,Math.incrementExact(previous.revision()));return new OperationPlan.Effect(key(p.pieceId()),old.owner(),Optional.of(old.data()),Optional.of(encode(p,next)));}
    private OperationPlan.Effect update(PieceRecord p,FunctionalStore.State after){var old=service.effects().get(key(p.pieceId()));var before=state(p);var next=new FunctionalStore.State(p.pieceId(),Math.incrementExact(before.revision()),after.slots(),after.energy(),after.machine());return new OperationPlan.Effect(key(p.pieceId()),old.owner(),Optional.of(old.data()),Optional.of(encode(p,next)));}
    public void tick()throws IOException{
        var active=service.snapshot().records().values().stream().filter(WorldFunctionals::piece).filter(p->level.isLoaded(WorldService.pos(p.origin16().cell()))).sorted(Comparator.comparing(PieceRecord::pieceId)).toList();var loaded=new HashSet<UUID>();for(var p:active){loaded.add(p.pieceId());simulated.putIfAbsent(p.pieceId(),level.getGameTime()-1);}simulated.keySet().retainAll(loaded);if(active.isEmpty())return;
        long start=System.nanoTime();for(int i=0;i<Math.min(16,active.size())&&System.nanoTime()-start<2_000_000;i++){var p=active.get(Math.floorMod(roundRobin++,active.size()));try{synchronize(p);}catch(IllegalArgumentException pending){if(!Objects.toString(pending.getMessage(),"").contains("backlog"))throw pending;}}refreshMenus();
    }
    private void refreshMenus(){for(var e:List.copyOf(MENUS.entrySet()))if(e.getValue().owner==this){var p=service.snapshot().records().get(e.getValue().piece);if(p!=null)e.getValue().container.replace(detached(p));}}
    public int analog(BlockPos position,Direction direction){var c=new CellPos(position.getX(),position.getY(),position.getZ());var normal=Direction6.valueOf(direction.getOpposite().name());long plane=(normal.axis()==0?c.x():normal.axis()==1?c.y():c.z())*16+(normal.sign()>0?16:0);int value=0;for(var id:service.snapshot().at(c)){var p=service.snapshot().records().get(id);if(!piece(p)||!service.available(p)||!NativeStates.resolve(p).hasAnalogOutputSignal())continue;if(net.nicomar.nomoregrids.diagnostic.GeometryCache.faces(p.worldOccupation(),4096).stream().anyMatch(face->face.normal()==normal&&face.plane()==plane))value=Math.max(value,AbstractContainerMenu.getRedstoneSignalFromContainer(detached(p)));}return value;}
    public void use(ServerPlayer player,net.minecraft.world.InteractionHand hand,PieceRecord p){var be=detached(p);if(!PaintableFunctionals.bucket(be.entity())||!PaintableFunctionals.bucketItem(player.getItemInHand(hand))){open(player,p);return;}try{synchronize(p);var original=state(p);var staged=detached(p,original);var before=NativeInventory.encode(player,NativeInventory.copy(player));var drops=new ArrayList<ItemStack>();BinaryData after;FunctionalStore.State next;MENU_TARGET.set(staged);STAGE.set(new Stage(player,drops));boolean oldCopy=COPY.get();COPY.set(true);try{PaintableFunctionals.bucketUse(player,staged.getBlockPos(),hand);after=NativeInventory.encode(player,NativeInventory.copy(player));next=capture(p,staged,original.revision());}finally{COPY.set(oldCopy);STAGE.remove();MENU_TARGET.remove();NativeInventory.restore(player,before);}var effects=new ArrayList<OperationPlan.Effect>();if(!next.equals(original))effects.add(update(p,next));if(!drops.isEmpty())effects.add(new OperationPlan.Effect("loot:"+UUID.randomUUID(),p.origin16().cell(),Optional.empty(),Optional.of(WorldLoot.encode(level,drops))));if(!effects.isEmpty()||!before.equals(after))service.executePrepared(new OperationPlan(UUID.randomUUID(),player.getUUID(),OperationPlan.Kind.TRANSFER,List.of(),before,after,effects));}catch(IOException ex){throw new UncheckedIOException(ex);}}
    public void open(ServerPlayer player,PieceRecord p){
        try{synchronize(p);}catch(IOException ex){throw new UncheckedIOException(ex);}
        var be=detached(p);if(!be.canOpen(player))throw new IllegalArgumentException("PROTECTED");var container=new FunctionalContainer(this,p.pieceId(),be);
        if(PaintableFunctionals.entity(be.entity())){net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,new net.nicomar.nomoregrids.network.FunctionalMenuPayload(p.pieceId(),be.getBlockPos(),p.sourceBlockId().value(),p.sourceState()));player.openMenu(new SimpleMenuProvider((window,inventory,actor)->{MENU_TARGET.set(container.be);try{var menu=PaintableFunctionals.menu(window,inventory,be.getBlockPos(),be.entity());MENUS.put(menu,new MenuSession(this,p.pieceId(),container));return menu;}finally{MENU_TARGET.remove();}},be.getDisplayName()),be.getBlockPos());return;}
        var data=new ContainerData(){
            @Override public int get(int index){var nbt=container.be.saveWithoutMetadata(level.registryAccess());return nbt.getIntOr(new String[]{"lit_time_remaining","lit_total_time","cooking_time_spent","cooking_total_time"}[index],0);}
            @Override public void set(int index,int value){}@Override public int getCount(){return 4;}};
        player.openMenu(new SimpleMenuProvider((window,inventory,actor)->{AbstractContainerMenu menu=switch(p.sourceBlockId().value()){
            case "minecraft:furnace"->new FurnaceMenu(window,inventory,container,data);case "minecraft:smoker"->new SmokerMenu(window,inventory,container,data);case "minecraft:blast_furnace"->new BlastFurnaceMenu(window,inventory,container,data);default->ChestMenu.threeRows(window,inventory,container);};MENUS.put(menu,new MenuSession(this,p.pieceId(),container));return menu;},be.getDisplayName()));
    }
    public static boolean stage(AbstractContainerMenu menu,ServerPlayer player,Runnable operation){
        var session=MENUS.get(menu);if(session==null||STAGE.get()!=null)return false;var owner=session.owner;var p=owner.service.snapshot().records().get(session.piece);if(p==null||!session.container.stillValid(player))return true;
        try{owner.synchronize(p);p=owner.service.snapshot().records().get(session.piece);session.container.replace(owner.detached(p));var before=NativeInventory.encode(player,NativeInventory.copy(player));var original=owner.state(p);var staged=owner.detached(p,original);session.container.replace(staged);var drops=new ArrayList<ItemStack>();BinaryData after;FunctionalStore.State next;int xp=0;staged=session.container.be;
            int oldOutput=staged.entity() instanceof AbstractFurnaceBlockEntity?staged.getItem(2).getCount():0;
            STAGE.set(new Stage(player,drops));boolean oldCopy=COPY.get();COPY.set(true);try{operation.run();after=NativeInventory.encode(player,NativeInventory.copy(player));if(staged.entity() instanceof AbstractFurnaceBlockEntity&&oldOutput>staged.getItem(2).getCount())xp=owner.claimExperience(staged);next=owner.capture(p,staged,original.revision());}finally{COPY.set(oldCopy);STAGE.remove();NativeInventory.restore(player,before);session.container.replace(owner.detached(p,original));}
            if(xp>0)after=NativeInventory.experience(after,xp);
            var effects=new ArrayList<OperationPlan.Effect>();if(!next.equals(original))effects.add(owner.update(p,next));
            if(!drops.isEmpty()){var cell=new CellPos(player.getBlockX(),player.getBlockY(),player.getBlockZ());effects.add(new OperationPlan.Effect("loot:"+UUID.randomUUID(),cell,Optional.empty(),Optional.of(WorldLoot.encode(owner.level,drops))));}
            if(!effects.isEmpty()||!before.equals(after))owner.service.executePrepared(new OperationPlan(UUID.randomUUID(),player.getUUID(),OperationPlan.Kind.TRANSFER,List.of(),before,after,effects));owner.refreshMenus();
        }catch(IOException|RuntimeException ex){if(owner.service.quarantined())NmgRuntime.failed(owner.level,ex);player.sendOverlayMessage(Component.literal("NMG: "+ex.getMessage()));}menu.broadcastFullState();return true;
    }
    public static boolean click(AbstractContainerMenu menu,int index,int button,ContainerInput input,Player actor){return actor instanceof ServerPlayer player&&stage(menu,player,()->menu.clicked(index,button,input,actor));}
    public static boolean deferDrop(ServerPlayer player,ItemStack stack){var stage=STAGE.get();if(stage==null||stage.player!=player)return false;if(!stack.isEmpty())stage.drops.add(stack.copy());return true;}
    private int claimExperience(FunctionalHandle be){
        var nbt=be.saveWithoutMetadata(level.registryAccess());var used=nbt.getCompoundOrEmpty("RecipesUsed");int points=0;
        for(String id:used.keySet()){
            var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,net.minecraft.resources.Identifier.parse(id));var holder=level.recipeAccess().byKey(key);
            if(holder.isPresent()&&holder.orElseThrow().value() instanceof net.minecraft.world.item.crafting.AbstractCookingRecipe recipe){float exact=used.getIntOr(id,0)*recipe.experience();int n=(int)Math.floor(exact);if(level.getRandom().nextFloat()<exact-n)n++;points=Math.addExact(points,n);}
        }
        nbt.put("RecipesUsed",new CompoundTag());be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),nbt));return points;
    }
    public static void closeCursor(ServerPlayer player){
        var session=MENUS.get(player.containerMenu);var owner=session==null?NmgRuntime.existing(player.level()):session.owner.service;if(owner==null)return;
        if(player.containerMenu.getCarried().isEmpty())NativeInventory.restoreCursor(player);if(player.containerMenu.getCarried().isEmpty())return;
        if(session==null&&player.getPersistentData().getStringOr("nmg_carried","").isEmpty())return;
        var slots=NativeInventory.copy(player);var before=NativeInventory.encode(player,slots);var carried=player.containerMenu.getCarried().copy();var effects=new ArrayList<OperationPlan.Effect>();
        try{var attempt=slots.stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));NativeInventory.insert(attempt,carried);slots=attempt;}catch(IllegalArgumentException full){var c=new CellPos(player.getBlockX(),player.getBlockY(),player.getBlockZ());effects.add(new OperationPlan.Effect("loot:"+UUID.randomUUID(),c,Optional.empty(),Optional.of(WorldLoot.encode(player.level(),List.of(carried)))));}
        var after=NativeInventory.withCarried(player,NativeInventory.encode(player,slots),ItemStack.EMPTY);try{owner.executePrepared(new OperationPlan(UUID.randomUUID(),player.getUUID(),OperationPlan.Kind.TRANSFER,List.of(),before,after,effects));}catch(IOException|RuntimeException ex){NmgRuntime.failed(player.level(),ex);throw new IllegalStateException("NMG cursor receipt requires recovery",ex);}
    }
    public void synchronize(PieceRecord p)throws IOException{
        if(!piece(p)||!level.isLoaded(WorldService.pos(p.origin16().cell())))return;long now=level.getGameTime();long previous=simulated.getOrDefault(p.pieceId(),now-1);if(previous>now)previous=now-1;long due=now-previous;if(due==0)return;
        var original=state(p);var be=detached(p,original);if(!(be.entity() instanceof AbstractFurnaceBlockEntity)&&!PaintableFunctionals.machine(be.entity())){simulated.put(p.pieceId(),now);return;}
        int steps=(int)Math.min(128,due);FunctionalStore.State after;boolean oldCopy=COPY.get();COPY.set(true);try{for(int i=0;i<steps;i++)if(be.entity() instanceof AbstractFurnaceBlockEntity furnace)AbstractFurnaceBlockEntity.serverTick(level,be.getBlockPos(),be.getBlockState(),furnace);else PaintableFunctionals.tick(level,be.entity());after=capture(p,be,original.revision());}finally{COPY.set(oldCopy);}
        if(!after.equals(original)){var changes=new ArrayList<OperationPlan.Change>();boolean lit=tag(level,after.machine()).getIntOr("lit_time_remaining",0)>0;var values=new HashMap<>(p.sourceState());if(values.containsKey("lit")&&!Objects.equals(values.get("lit"),Boolean.toString(lit))){values.put("lit",Boolean.toString(lit));var next=new PieceRecord(p.pieceId(),Math.incrementExact(p.revision()),p.adapterId(),p.adapterVersion(),p.sourceBlockId(),values,p.origin16(),p.orientation(),p.pivot16(),p.geometry(),p.geometryPayload(),p.materialPayload(),p.itemPayload(),p.groupId(),p.flags());changes.add(new OperationPlan.Change(p.pieceId(),Optional.of(p),Optional.of(next)));}service.executeWorld(changes,List.of(update(p,be)));}
        simulated.put(p.pieceId(),previous+steps);if(due>steps)throw new IllegalArgumentException("PENDING_MINING: functional tick backlog");
    }
    private static final class FunctionalContainer implements Container {
        final WorldFunctionals owner;final UUID piece;FunctionalHandle be;
        FunctionalContainer(WorldFunctionals owner,UUID piece,FunctionalHandle be){this.owner=owner;this.piece=piece;this.be=be;}void replace(FunctionalHandle value){if(PaintableFunctionals.entity(be.entity())){var tag=value.saveWithoutMetadata(owner.level.registryAccess());be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,owner.level.registryAccess(),tag));}else be=value;}
        @Override public int getContainerSize(){return be.getContainerSize();}@Override public boolean isEmpty(){return be.isEmpty();}@Override public ItemStack getItem(int slot){return STAGE.get()!=null?be.getItem(slot):be.getItem(slot).copy();}
        @Override public ItemStack removeItem(int slot,int amount){if(STAGE.get()==null)throw new IllegalStateException("Functional write requires a staged menu");return be.removeItem(slot,amount);}@Override public ItemStack removeItemNoUpdate(int slot){if(STAGE.get()==null)throw new IllegalStateException("Functional write requires a staged menu");return be.removeItemNoUpdate(slot);}@Override public void setItem(int slot,ItemStack stack){if(STAGE.get()==null)throw new IllegalStateException("Functional write requires a staged menu");be.setItem(slot,stack);}@Override public void setChanged(){}@Override public void clearContent(){if(STAGE.get()==null)throw new IllegalStateException("Functional write requires a staged menu");be.clearContent();}
        @Override public boolean stillValid(Player player){var p=owner.service.snapshot().records().get(piece);if(p==null||player.level()!=owner.level||owner.service.quarantined()||player.isSpectator()||!owner.level.mayInteract(player,WorldService.pos(p.origin16().cell())))return false;if(PaintableFunctionals.entity(be.entity())&&!be.getBlockPos().equals(WorldService.pos(p.origin16().cell())))return false;var c=p.origin16().cell();return owner.level.isLoaded(WorldService.pos(c))&&player.distanceToSqr(c.x()+0.5,c.y()+0.5,c.z()+0.5)<=64;}
        @Override public boolean canPlaceItem(int slot,ItemStack stack){return be.canPlaceItem(slot,stack);}
    }
}
