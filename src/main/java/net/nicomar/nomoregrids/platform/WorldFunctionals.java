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
    private static final Map<AbstractContainerMenu,Integer> DESTINATIONS=new WeakHashMap<>();
    public static boolean managed(AbstractContainerMenu menu){return MENUS.containsKey(menu);}
    private final ServerLevel level;private final WorldService service;private int roundRobin;
    public WorldFunctionals(ServerLevel level,WorldService service){this.level=level;this.service=service;}
    public static boolean eligible(BlockState state){return IDS.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())&&state.getFluidState().isEmpty();}
    public static boolean piece(PieceRecord p){return p.adapterId().value().equals(ADAPTER);}
    private static String key(UUID id){return "machine:"+id;}
    private static BinaryData tag(ServerLevel level,CompoundTag tag){return new BinaryData(CompoundTag.CODEC.encodeStart(level.registryAccess().createSerializationContext(JsonOps.INSTANCE),tag).getOrThrow().toString().getBytes(StandardCharsets.UTF_8));}
    private static CompoundTag tag(ServerLevel level,BinaryData data){return CompoundTag.CODEC.parse(level.registryAccess().createSerializationContext(JsonOps.INSTANCE),JsonParser.parseString(new String(data.copy(),StandardCharsets.UTF_8))).getOrThrow();}
    public static PieceRecord capture(BlockState state,Level world,BlockPos at,UUID id){
        if(!eligible(state))throw new IllegalArgumentException("UNSUPPORTED_ADAPTER: functional profile");var selection=NativeStates.boxes(state.getShape(world,at));var collision=NativeStates.boxes(state.getCollisionShape(world,at));BinaryData original=BinaryData.EMPTY;
        if(world instanceof ServerLevel server&&world.getBlockEntity(at)!=null)original=new BinaryData(net.nicomar.nomoregrids.network.SnapshotAssembler.sha256(tag(server,world.getBlockEntity(at).saveWithoutMetadata(server.registryAccess())).copy()));
        var source=new ResourceId(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());return new PieceRecord(id,0,new ResourceId(ADAPTER),1,source,NativeStates.properties(state),new GridPoint(at.getX()*16L,at.getY()*16L,at.getZ()*16L),0,GridPoint.BLOCK_CENTER,new BoxGeometrySnapshot(selection,collision,selection),new NativeStateImage(source,NativeStates.properties(state)).encode(),BinaryData.EMPTY,original,Optional.empty(),Set.of());
    }
    public boolean matchesNative(PieceRecord p,BlockPos at){if(p.itemPayload().size()!=32||level.getBlockEntity(at)==null)return false;return Arrays.equals(p.itemPayload().copy(),net.nicomar.nomoregrids.network.SnapshotAssembler.sha256(tag(level,level.getBlockEntity(at).saveWithoutMetadata(level.registryAccess())).copy()));}
    private BaseContainerBlockEntity fresh(PieceRecord p){
        var state=NativeStates.resolve(p);if(!eligible(state)||!(state.getBlock() instanceof EntityBlock factory))throw new IllegalArgumentException("UNSUPPORTED_ADAPTER: native functional class");var be=factory.newBlockEntity(WorldService.pos(p.origin16().cell()),state);if(!(be instanceof BaseContainerBlockEntity container))throw new IllegalArgumentException("UNSUPPORTED_ADAPTER: native container");container.setLevel(level);return container;
    }
    private FunctionalStore.State capture(PieceRecord p,BaseContainerBlockEntity be,long revision){
        var slots=new ArrayList<BinaryData>();var ops=level.registryAccess().createSerializationContext(JsonOps.INSTANCE);for(int i=0;i<be.getContainerSize();i++)slots.add(new BinaryData(ItemStack.OPTIONAL_CODEC.encodeStart(ops,be.getItem(i)).getOrThrow().toString().getBytes(StandardCharsets.UTF_8)));
        var nbt=be.saveWithoutMetadata(level.registryAccess());nbt.remove("Items");nbt.remove("items");return new FunctionalStore.State(p.pieceId(),revision,slots,0,tag(level,nbt));
    }
    private BinaryData encode(PieceRecord p,FunctionalStore.State state){try{return new BinaryData(SimulationCodec.encode(new SimulationSnapshot(p.ownerSection(),0,Map.of(),Map.of(p.pieceId(),state),Map.of(),Map.of())));}catch(IOException ex){throw new IllegalArgumentException("COMPLEXITY_LIMIT: functional checkpoint",ex);}}
    private FunctionalStore.State state(PieceRecord p){var value=service.effects().get(key(p.pieceId()));if(value==null)throw new IllegalArgumentException("INVALID_DATA: missing functional authority");try{return SimulationCodec.decode(value.data().copy()).functionals().get(p.pieceId());}catch(IOException ex){throw new IllegalArgumentException("INVALID_DATA: functional checkpoint",ex);}}
    public BaseContainerBlockEntity detached(PieceRecord p){return detached(p,state(p));}
    private BaseContainerBlockEntity detached(PieceRecord p,FunctionalStore.State state){
        var be=fresh(p);be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),tag(level,state.machine())));if(be.getContainerSize()!=state.slots().size())throw new IllegalArgumentException("INVALID_DATA: functional layout");var ops=level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        for(int i=0;i<state.slots().size();i++)be.setItem(i,ItemStack.OPTIONAL_CODEC.parse(ops,JsonParser.parseString(new String(state.slots().get(i).copy(),StandardCharsets.UTF_8))).getOrThrow());
        var combined=be.saveWithoutMetadata(level.registryAccess());combined.merge(tag(level,state.machine()));be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),combined));return be;
    }
    public OperationPlan prepare(OperationPlan plan){
        var effects=new ArrayList<>(plan.effects());for(var c:plan.changes()){
            if(c.after().isPresent()&&piece(c.after().orElseThrow())){
                var p=c.after().orElseThrow();var value=service.effects().get(key(p.pieceId()));if(value==null){BaseContainerBlockEntity be;
                    var actual=level.getBlockEntity(WorldService.pos(p.origin16().cell()));if(actual instanceof BaseContainerBlockEntity nativeBE&&matchesNative(p,nativeBE.getBlockPos()))be=nativeBE;else if(p.itemPayload().size()==0)be=fresh(p);else throw new IllegalArgumentException("STALE_REVISION: native inventory");
                    effects.add(new OperationPlan.Effect(key(p.pieceId()),p.origin16().cell(),Optional.empty(),Optional.of(encode(p,capture(p,be,0)))));
                }else if(c.before().isPresent()&&!c.before().orElseThrow().ownerSection().equals(p.ownerSection()))throw new IllegalArgumentException("UNSUPPORTED_ADAPTER: functional cross-section move requires owner migration");
            }else if(c.before().isPresent()&&piece(c.before().orElseThrow())){var p=c.before().orElseThrow();var old=service.effects().get(key(p.pieceId()));if(old==null)throw new IllegalArgumentException("INVALID_DATA: missing functional authority");effects.add(new OperationPlan.Effect(key(p.pieceId()),old.owner(),Optional.of(old.data()),Optional.empty()));}
        }
        return new OperationPlan(plan.id(),plan.actor(),plan.kind(),plan.changes(),plan.resourcesBefore(),plan.resourcesAfter(),effects);
    }
    public List<ItemStack> drops(PieceRecord p,ServerPlayer player){var be=detached(p);var result=new ArrayList<ItemStack>();for(int i=0;i<be.getContainerSize();i++){result.add(be.getItem(i).copy());be.setItem(i,ItemStack.EMPTY);}result.addAll(Block.getDrops(NativeStates.resolve(p),level,be.getBlockPos(),be,player,player==null?ItemStack.EMPTY:player.getMainHandItem()));return result.stream().filter(s->!s.isEmpty()).toList();}
    private OperationPlan.Effect update(PieceRecord p,BaseContainerBlockEntity be){var old=service.effects().get(key(p.pieceId()));var previous=state(p);var next=capture(p,be,Math.incrementExact(previous.revision()));return new OperationPlan.Effect(key(p.pieceId()),old.owner(),Optional.of(old.data()),Optional.of(encode(p,next)));}
    public void tick()throws IOException{
        var active=service.snapshot().records().values().stream().filter(WorldFunctionals::piece).filter(p->level.isLoaded(WorldService.pos(p.origin16().cell()))).sorted(Comparator.comparing(PieceRecord::pieceId)).toList();if(active.isEmpty())return;
        int count=Math.min(16,active.size());for(int i=0;i<count;i++){var p=active.get(Math.floorMod(roundRobin++,active.size()));var before=state(p);var be=detached(p,before);if(!(be instanceof AbstractFurnaceBlockEntity furnace))continue;
            AbstractFurnaceBlockEntity.serverTick(level,be.getBlockPos(),be.getBlockState(),furnace);var after=capture(p,be,before.revision());if(!after.equals(before)){
                var changes=new ArrayList<OperationPlan.Change>();boolean lit=tag(level,after.machine()).getIntOr("lit_time_remaining",0)>0;var properties=new HashMap<>(p.sourceState());if(properties.containsKey("lit")&&!properties.get("lit").equals(Boolean.toString(lit))){properties.put("lit",Boolean.toString(lit));var next=new PieceRecord(p.pieceId(),Math.incrementExact(p.revision()),p.adapterId(),p.adapterVersion(),p.sourceBlockId(),properties,p.origin16(),p.orientation(),p.pivot16(),p.geometry(),p.geometryPayload(),p.materialPayload(),p.itemPayload(),p.groupId(),p.flags());changes.add(new OperationPlan.Change(p.pieceId(),Optional.of(p),Optional.of(next)));}
                service.executeWorld(changes,List.of(update(p,be)));
            }
        }refreshMenus();
    }
    private void refreshMenus(){for(var e:List.copyOf(MENUS.entrySet()))if(e.getValue().owner==this){var p=service.snapshot().records().get(e.getValue().piece);if(p!=null)e.getValue().container.replace(detached(p));}}
    public void open(ServerPlayer player,PieceRecord p){
        var be=detached(p);if(!be.canOpen(player))throw new IllegalArgumentException("PROTECTED");var container=new FunctionalContainer(this,p.pieceId(),be);var data=new ContainerData(){
            @Override public int get(int index){var nbt=container.be.saveWithoutMetadata(level.registryAccess());return nbt.getIntOr(new String[]{"lit_time_remaining","lit_total_time","cooking_time_spent","cooking_total_time"}[index],0);}
            @Override public void set(int index,int value){}@Override public int getCount(){return 4;}};
        player.openMenu(new SimpleMenuProvider((window,inventory,actor)->{AbstractContainerMenu menu=switch(p.sourceBlockId().value()){
            case "minecraft:furnace"->new FurnaceMenu(window,inventory,container,data);case "minecraft:smoker"->new SmokerMenu(window,inventory,container,data);case "minecraft:blast_furnace"->new BlastFurnaceMenu(window,inventory,container,data);default->ChestMenu.threeRows(window,inventory,container);};MENUS.put(menu,new MenuSession(this,p.pieceId(),container));return menu;},be.getDisplayName().copy().append(Component.literal(" · derecho: destino; clic: transferir"))));
    }
    public static boolean click(AbstractContainerMenu menu,int index,int button,ContainerInput input,Player actor){
        var session=MENUS.get(menu);if(session==null||!(actor instanceof ServerPlayer player))return false;
        try{session.owner.transfer(menu,index,button,input,player,session);}catch(IOException|RuntimeException ex){if(session.owner.service.quarantined())NmgRuntime.failed(session.owner.level,ex);player.sendOverlayMessage(Component.literal("No se pudo transferir: "+ex.getMessage()));}menu.setCarried(ItemStack.EMPTY);menu.broadcastFullState();return true;
    }
    private void transfer(AbstractContainerMenu menu,int index,int button,ContainerInput input,ServerPlayer actor,MenuSession session)throws IOException{
        if(input!=ContainerInput.PICKUP&&input!=ContainerInput.QUICK_MOVE||index<0||index>=menu.slots.size()||!session.container.stillValid(actor))return;var p=service.snapshot().records().get(session.piece);var be=detached(p);var slots=NativeInventory.copy(actor);var before=NativeInventory.encode(actor,slots);var clicked=menu.slots.get(index);
        if(clicked.container==session.container){int slot=clicked.getContainerSlot();if(button==1){DESTINATIONS.put(menu,slot);return;}var item=be.getItem(slot);if(item.isEmpty())return;int amount=button==1?1:item.getCount();NativeInventory.insert(slots,item.copyWithCount(amount));be.removeItem(slot,amount);}
        else if(clicked.container==actor.getInventory()){
            int source=clicked.getContainerSlot();if(source<0||source>=slots.size())return;var item=slots.get(source);if(item.isEmpty())return;int remaining=button==1?1:item.getCount(),moved=0;
            for(int i=0;i<be.getContainerSize()&&remaining>0;i++){if(DESTINATIONS.containsKey(menu)&&DESTINATIONS.get(menu)!=i||!be.canPlaceItem(i,item))continue;var old=be.getItem(i);if(!old.isEmpty()&&!ItemStack.isSameItemSameComponents(old,item))continue;int n=Math.min(remaining,Math.min(item.getMaxStackSize(),be.getMaxStackSize())-old.getCount());if(n>0){be.setItem(i,item.copyWithCount(old.getCount()+n));remaining-=n;moved+=n;}}
            if(moved==0)return;item.shrink(moved);
        }else return;
        service.executePrepared(new OperationPlan(UUID.randomUUID(),actor.getUUID(),OperationPlan.Kind.TRANSFER,List.of(),before,NativeInventory.encode(actor,slots),List.of(update(p,be))));refreshMenus();
    }
    private static final class FunctionalContainer implements Container {
        final WorldFunctionals owner;final UUID piece;BaseContainerBlockEntity be;
        FunctionalContainer(WorldFunctionals owner,UUID piece,BaseContainerBlockEntity be){this.owner=owner;this.piece=piece;this.be=be;}void replace(BaseContainerBlockEntity value){be=value;}
        @Override public int getContainerSize(){return be.getContainerSize();}@Override public boolean isEmpty(){return be.isEmpty();}@Override public ItemStack getItem(int slot){return be.getItem(slot).copy();}
        @Override public ItemStack removeItem(int slot,int amount){throw new UnsupportedOperationException("Use transactional transfer");}@Override public ItemStack removeItemNoUpdate(int slot){throw new UnsupportedOperationException("Use transactional transfer");}@Override public void setItem(int slot,ItemStack stack){throw new UnsupportedOperationException("Use transactional transfer");}@Override public void setChanged(){}@Override public void clearContent(){throw new UnsupportedOperationException("Use transactional transfer");}
        @Override public boolean stillValid(Player player){var p=owner.service.snapshot().records().get(piece);if(p==null||player.level()!=owner.level||owner.service.quarantined())return false;var c=p.origin16().cell();return owner.level.isLoaded(WorldService.pos(c))&&player.distanceToSqr(c.x()+0.5,c.y()+0.5,c.z()+0.5)<=64;}
        @Override public boolean canPlaceItem(int slot,ItemStack stack){return be.canPlaceItem(slot,stack);}
    }
}
