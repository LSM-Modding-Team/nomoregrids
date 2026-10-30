package net.nicomar.nomoregrids.platform;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.nicomar.nomoregrids.api.adapter.*;
import net.nicomar.nomoregrids.compat.vanilla.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.event.BeforeOperationEvent;
import net.nicomar.nomoregrids.geometry.*;
import net.nicomar.nomoregrids.geometry.Rotation;
import net.nicomar.nomoregrids.network.*;
import net.nicomar.nomoregrids.operation.*;
import net.nicomar.nomoregrids.persistence.*;
import net.nicomar.nomoregrids.registry.NmgContent;
import net.nicomar.nomoregrids.world.*;

/** Experimental server host. Authoritative history is durable; chunk replicas can be rebuilt on natural load. */
public final class WorldService implements OperationEngine.Host,AutoCloseable {
    private final ServerLevel level;
    private DurableJournal journal;
    private final Path root;
    private JournalArchive archive;
    private long baseSequence;
    private boolean compactionFault;
    private final SectionStore store;
    private final WorldEffectStore effectStore;
    private final Map<String,WorldEffectStore.Value> effects=new TreeMap<>();
    private final WorldFunctionals functionals;
    public WorldFunctionals functionals(){return functionals;}
    private final WorldFluids fluids;
    private final Map<UUID,Map<BlockPos,net.minecraft.world.level.block.state.BlockState>> nativeImages=new HashMap<>();
    public WorldFluids fluids(){return fluids;}
    public PieceAdapter adapter(PieceRecord p){return adapters.require(p);}
    private int writing;
    public boolean writing(){return writing>0;}
    public Map<String,WorldEffectStore.Value> effects(){return Collections.unmodifiableMap(effects);}
    private final AdapterRegistry adapters;
    private final VanillaCatalog catalog=new VanillaCatalog();
    private final String receiptKey;
    private final Map<UUID,Long> sequences=new HashMap<>();
    private final Map<UUID,OperationPlan> history=new HashMap<>();
    private final Set<UUID> knownIds=new HashSet<>();
    private final Map<UUID,Runnable> permissionHooks=new HashMap<>();
    private OperationEngine engine;
    private boolean recovering;

    public WorldService(ServerLevel level,AdapterRegistry adapters)throws IOException{
        this.level=level;this.adapters=adapters;fluids=new WorldFluids(level,this);functionals=new WorldFunctionals(level,this);
        String dimension=level.dimension().identifier().toString();
        String hash=java.util.HexFormat.of().formatHex(SnapshotAssembler.sha256(dimension.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        receiptKey="nmg_applied_"+hash;root=level.getServer().getWorldPath(LevelResource.ROOT).resolve("nomoregrids").resolve(hash);
        Map<String,Integer> versions=new HashMap<>();adapters.versions().forEach((id,version)->versions.put(id.value(),version));WorldRules.verifyOrCreate(root.resolve("rules.json"),SessionPayload.localHash(),ProtocolRules.defaults(versions));
        store=new SectionStore(root.resolve("sections"));effectStore=new WorldEffectStore(root.resolve("effects"));archive=JournalArchive.open(root).orElse(null);baseSequence=archive==null?0:archive.state().sequence();journal=new DurableJournal(archive==null?root.resolve("operations.log"):archive.activeLog());
        try{restore();}catch(IOException|RuntimeException ex){journal.close();throw ex;}
    }
    private void restore()throws IOException{
        recovering=true;var records=new HashMap<UUID,PieceRecord>();if(archive!=null){records.putAll(archive.state().records());effects.putAll(archive.state().effects());knownIds.addAll(records.keySet());}long sequence=baseSequence;
        for(var plan:journal.committed()){
            sequences.put(plan.id(),++sequence);history.put(plan.id(),plan);
            for(var c:plan.changes()){
                knownIds.add(c.id());
                if(!Objects.equals(Optional.ofNullable(records.get(c.id())),c.before()))throw new IOException("Journal history conflict");
                if(c.after().isPresent())records.put(c.id(),c.after().orElseThrow());else records.remove(c.id());
            }
            applyEffects(plan);reconcileResources(plan,sequence);
        }
        Map<Long,UUID> owners=new HashMap<>();sequences.forEach((id,seq)->owners.put(seq,history.get(id).actor()));
        var actors=new HashSet<UUID>();if(archive!=null)actors.addAll(archive.state().receipts().keySet());history.values().stream().map(OperationPlan::actor).filter(id->!id.equals(OperationPlan.WORLD_ACTOR)).forEach(actors::add);
        for(UUID actor:actors){
            long marker=watermark(actor);if(archive!=null&&marker<archive.state().receipts().getOrDefault(actor,0L))throw new IOException("Player receipt predates compacted history; original retained");if(marker!=0&&(marker<=baseSequence?!Objects.equals(archive.state().receipts().get(actor),marker):!actor.equals(owners.get(marker))))throw new IOException("Player receipt does not belong to this journal history; data retained");
        }
        for(var p:List.copyOf(records.values()))try{adapters.require(p).occupation(p);NativeStates.resolve(p);NativeStates.original(p);}catch(RuntimeException ex){
            var flags=new HashSet<>(p.flags());flags.add(PieceRecord.Flag.SUBSTITUTE);records.put(p.pieceId(),new PieceRecord(p.pieceId(),p.revision(),p.adapterId(),p.adapterVersion(),p.sourceBlockId(),p.sourceState(),p.origin16(),p.orientation(),p.pivot16(),p.geometry(),p.geometryPayload(),p.materialPayload(),p.itemPayload(),p.groupId(),flags));
        }
        var index=PieceIndex.retained(records.values(),adapters::overlap);
        engine=new OperationEngine(index,journal,this,PieceIndex.Limits.DEFAULT);recovering=false;
    }
    private boolean receipt(OperationPlan plan,long sequence)throws IOException{
        if(plan.actor().equals(OperationPlan.WORLD_ACTOR))return true;return watermark(plan.actor())>=sequence;
    }
    private long watermark(UUID actor)throws IOException{
        // A volatile entity tag is not proof of a completed save, even when the actor is online.
        var saved=level.getServer().getPlayerList().loadPlayerData(new NameAndId(actor,"NMG-recovery"));
        long marker=saved.map(tag->tag.getCompoundOrEmpty("NeoForgeData").getLongOr(receiptKey,0)).orElse(0L);
        if(marker<0)throw new IOException("Negative resource receipt");return marker;
    }
    private void reconcileResources(OperationPlan plan,long sequence)throws IOException{
        if(plan.actor().equals(OperationPlan.WORLD_ACTOR))return;
        if(receipt(plan,sequence))return;
        if(recovering)for(var change:plan.changes())for(var p:List.of(change.before(),change.after()))if(p.isPresent()&&store.read(p.orElseThrow().ownerSection()).checkpoint()>=sequence)
            throw new IOException("World checkpoint exists without its player receipt; operation quarantined");
        if(recovering)for(var effect:plan.effects())if(effectStore.checkpoint(effect.owner().section())>=sequence)throw new IOException("Effects checkpoint exists without its player receipt; operation quarantined");
        ServerPlayer actor=level.getServer().getPlayerList().getPlayer(plan.actor());
        if(actor==null)throw new IOException("Recovery waiting for actor "+plan.actor()+"; no resources replayed");
        if(!recovering&&!NativeInventory.matches(actor,plan.resourcesBefore()))throw new IOException("Resource image changed before live commit application");
        try{NativeInventory.install(actor,plan.resourcesBefore(),plan.resourcesAfter());}
        catch(RuntimeException ex){throw new IOException("Resource receipt conflict; operation quarantined",ex);}
        actor.getPersistentData().putLong(receiptKey,sequence);
        level.getServer().getPlayerList().saveAll();
        Path playerFile=level.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(actor.getUUID()+".dat");
        if(!Files.isRegularFile(playerFile))throw new IOException("Missing saved player receipt");
        try(var file=FileChannel.open(playerFile,StandardOpenOption.WRITE)){file.force(true);}
        DurableDirectories.force(playerFile.getParent());
        var saved=level.getServer().getPlayerList().loadPlayerData(actor.nameAndId());
        if(saved.isEmpty()||saved.orElseThrow().getCompoundOrEmpty("NeoForgeData").getLongOr(receiptKey,0)<sequence)throw new IOException("Receipt not persisted with player inventory");
    }
    public PieceIndex snapshot(){return engine.snapshot();}
    public boolean quarantined(){return compactionFault||engine.quarantined();}
    public net.nicomar.nomoregrids.diagnostic.TimingSamples timings(){return engine.timings();}
    public static BlockPos pos(CellPos p){return new BlockPos(Math.toIntExact(p.x()),Math.toIntExact(p.y()),Math.toIntExact(p.z()));}
    public void place(ServerPlayer player,InteractionHand hand,BlockHitResult hit,ModePayload mode)throws IOException{
        executePrepared(placementPlan(player,hand,hit,mode,UUID.randomUUID(),false));
    }
    public void place(ServerPlayer player,InteractionHand hand,BlockHitResult hit,ModePayload mode,UUID operation)throws IOException{
        if(repeated(operation,player,OperationPlan.Kind.PLACE))return;executePrepared(placementPlan(player,hand,hit,mode,operation,false));
    }
    public boolean probe(ServerPlayer player,InteractionHand hand,BlockHitResult hit,ModePayload mode)throws IOException{
        var plan=fluids.prepare(placementPlan(player,hand,hit,mode,UUID.randomUUID(),true));var before=snapshot();var after=before.changed(plan.changes().stream().map(OperationPlan.Change::indexChange).toList(),PieceIndex.Limits.DEFAULT);validate(plan,before,after,true);return plan.changes().stream().noneMatch(c->c.before().isPresent()&&c.after().isEmpty());
    }
    public void executePrepared(OperationPlan original)throws IOException{
        var plan=fluids.prepare(functionals.prepare(original));var images=new HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();for(var c:plan.affectedCells())images.put(pos(c),level.getBlockState(pos(c)));nativeImages.put(plan.id(),images);
        try{engine.execute(plan);}finally{nativeImages.remove(plan.id());}
    }
    public CellView cellView(CellPos c,PieceIndex index){return new CellView(CellView.from(c,index).replicas(),fluids.at(c));}
    public boolean hasOperation(UUID operation,UUID actor){var old=history.get(operation);if(old==null){if(archive==null)return false;try{var stamp=archive.operation(operation);if(stamp.isEmpty())return false;if(!stamp.orElseThrow().actor().equals(actor))throw new IllegalArgumentException("Operation ID conflict");return true;}catch(IOException ex){throw new UncheckedIOException(ex);}}if(!old.actor().equals(actor))throw new IllegalArgumentException("Operation ID conflict");return true;}
    private boolean repeated(UUID id,ServerPlayer actor,OperationPlan.Kind kind){
        var old=history.get(id);if(old==null){if(!hasOperation(id,actor.getUUID()))return false;try{if(archive.operation(id).orElseThrow().kind()!=kind)throw new IllegalArgumentException("Operation ID conflict");return true;}catch(IOException ex){throw new UncheckedIOException(ex);}}if(!old.actor().equals(actor.getUUID())||old.kind()!=kind)throw new IllegalArgumentException("Operation ID conflict");return true;
    }
    private OperationPlan placementPlan(ServerPlayer player,InteractionHand hand,BlockHitResult hit,ModePayload mode,UUID operation,boolean preview)throws IOException{
        assertServerThread();PieceRecord candidate=ConstructionCandidate.build(player,hand,hit,mode,catalog,UUID.randomUUID());
        var changes=new ArrayList<OperationPlan.Change>();
        PieceRecord replaced=null;
        if(mode.replace()){
            if(!(level.getBlockEntity(hit.getBlockPos()) instanceof CompositeCellEntity cell))throw new IllegalArgumentException("Replacement requires a selected component");
            var selected=ComponentSelection.select(player,cell.view(),mode.candidate()).orElseThrow();replaced=snapshot().records().get(selected.pieceId());
            if(replaced==null||replaced.revision()!=selected.revision())throw new IllegalArgumentException("STALE_REVISION");
            if(!player.isCreative()&&!player.hasCorrectToolForDrops(NativeStates.resolve(replaced)))throw new IllegalArgumentException("Replacement requires the correct main-hand tool");
            changes.add(new OperationPlan.Change(replaced.pieceId(),Optional.of(replaced),Optional.empty()));
        }
        for(var cell:candidate.worldOccupation().cells(8)){
            BlockPos at=pos(cell);if(!level.isLoaded(at))throw new IllegalArgumentException("UNLOADED_REGION");
            var state=level.getBlockState(at);
            if(!snapshot().at(cell).isEmpty()&&nativeProjection(cell,snapshot()).filter(state::equals).isPresent())continue;
            if(state.getBlock() instanceof LiquidBlock)continue;
            if(!state.isAir()&&state.getBlock()!=NmgContent.CELL.get()){
                var paintable=net.nicomar.nomoregrids.compat.paintableblocks.PaintableBridge.available().filter(b->b.nativeBlock(state));
                if(paintable.isEmpty()&&!WorldFunctionals.eligible(state)&&!catalog.eligible(state))throw new IllegalArgumentException("UNSUPPORTED_ADAPTER");
                PieceRecord nativePiece=WorldFunctionals.eligible(state)?WorldFunctionals.capture(state,level,at,UUID.randomUUID()):paintable.isPresent()?paintable.orElseThrow().captureNative(level,at,UUID.randomUUID()):NativeStates.capture(state,level,at,UUID.randomUUID());
                changes.add(new OperationPlan.Change(nativePiece.pieceId(),Optional.empty(),Optional.of(nativePiece)));
            }
        }
        changes.add(new OperationPlan.Change(candidate.pieceId(),Optional.empty(),Optional.of(candidate)));
        var slots=NativeInventory.copy(player);BinaryData before=NativeInventory.encode(player,slots);
        if(!player.isCreative()){
            int slot=hand==InteractionHand.MAIN_HAND?player.getInventory().getSelectedSlot():net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND;
            var sourceBlock=NativeStates.resolve(candidate).getBlock();
            if(net.nicomar.nomoregrids.material.FragmentItem.cuttable(sourceBlock))NativeInventory.takeFragment(slots,sourceBlock,net.nicomar.nomoregrids.material.FragmentItem.length(candidate.itemPayload()));
            else{if(slots.get(slot).isEmpty())throw new IllegalArgumentException("INVENTORY_MISSING");slots.get(slot).shrink(1);}
            if(replaced!=null&&!preview)recoverLoot(player,replaced,slots);
        }
        completeSupport(player,changes,slots,preview);
        return new OperationPlan(operation,player.getUUID(),OperationPlan.Kind.PLACE,changes,before,NativeInventory.encode(player,slots));
    }
    public void remove(ServerPlayer player,UUID id)throws IOException{
        remove(player,id,UUID.randomUUID());
    }
    public void remove(ServerPlayer player,UUID id,UUID operation)throws IOException{
        if(repeated(operation,player,OperationPlan.Kind.REMOVE))return;
        PieceRecord target=snapshot().records().get(id);if(target==null)throw new IllegalArgumentException("STALE_REVISION");adapters.require(target);
        if(!Set.of("nomoregrids:box","nomoregrids:paintableblocks",WorldFunctionals.ADAPTER).contains(target.adapterId().value()))throw new IllegalArgumentException("Use the adapter's native recovery bridge; generic loot is forbidden");
        var slots=NativeInventory.copy(player);BinaryData before=NativeInventory.encode(player,slots);
        if(!player.isCreative())recoverLoot(player,target,slots);
        var changes=new ArrayList<>(List.of(new OperationPlan.Change(id,Optional.of(target),Optional.empty())));completeSupport(player,changes,slots);
        var plan=new OperationPlan(operation,player.getUUID(),OperationPlan.Kind.REMOVE,changes,before,NativeInventory.encode(player,slots));executePrepared(plan);
    }
    private void recoverLoot(ServerPlayer player,PieceRecord target,List<ItemStack> slots){
        recoverLoot(player,target,slots,true);
    }
    private void recoverLoot(ServerPlayer player,PieceRecord target,List<ItemStack> slots,boolean damageTool){
        boolean functional=WorldFunctionals.piece(target);boolean paintable=target.adapterId().value().equals("nomoregrids:paintableblocks");if(!functional&&!paintable&&!target.adapterId().value().equals("nomoregrids:box"))throw new IllegalArgumentException("Native adapter recovery is required");
        var state=NativeStates.resolve(target);var at=pos(target.origin16().cell());
        int length=paintable||functional?16:net.nicomar.nomoregrids.material.FragmentItem.length(target.itemPayload());
        var loot=functional?functionals.drops(target,player):paintable?net.nicomar.nomoregrids.compat.paintableblocks.PaintableBridge.available().orElseThrow(()->new IllegalArgumentException("UNSUPPORTED_ADAPTER: PB recovery")).drops(player,target):player.hasCorrectToolForDrops(state)?Block.getDrops(state,level,at,null,player,player.getMainHandItem()):List.<ItemStack>of();
        for(var item:loot){
            if(length<16&&item.getItem()==state.getBlock().asItem())item=net.nicomar.nomoregrids.material.FragmentItem.create(state.getBlock(),length).copyWithCount(item.getCount());NativeInventory.insert(slots,item);
        }
        int selected=player.getInventory().getSelectedSlot();ItemStack tool=slots.get(selected);
        if(damageTool&&tool.isDamageableItem()){
            int damage=EnchantmentHelper.processDurabilityChange(level,tool,1);tool.setDamageValue(Math.addExact(tool.getDamageValue(),damage));
            if(tool.getDamageValue()>=tool.getMaxDamage())slots.set(selected,ItemStack.EMPTY);
        }
    }
    public void editPaintable(ServerPlayer player,InteractionHand hand,UUID id,BlockHitResult hit,UUID operation)throws IOException{editPaintable(player,hand,id,hit,operation,false);}
    public void editPaintable(ServerPlayer player,InteractionHand hand,UUID id,BlockHitResult hit,UUID operation,boolean control)throws IOException{
        if(repeated(operation,player,OperationPlan.Kind.EDIT))return;var old=snapshot().records().get(id);if(old==null||!old.adapterId().value().equals("nomoregrids:paintableblocks"))throw new IllegalArgumentException("UNSUPPORTED_ADAPTER");
        PaintableSelections.use(this,player,hand,old,hit,operation,control);
    }
    public void commitPaintable(ServerPlayer player,InteractionHand hand,Map<UUID,net.nicomar.nomoregrids.compat.paintableblocks.PaintableBridge.Edit> edits,List<ItemStack> slots,UUID operation)throws IOException{
        var before=NativeInventory.encode(player,NativeInventory.copy(player));var changes=new ArrayList<OperationPlan.Change>();var originals=new HashMap<UUID,PieceRecord>();
        for(var e:edits.entrySet()){var old=snapshot().records().get(e.getKey());if(old==null)throw new IllegalArgumentException("STALE_REVISION");originals.put(e.getKey(),old);changes.add(new OperationPlan.Change(e.getKey(),Optional.of(old),e.getValue().after()));}
        completeSupport(player,changes,slots);var plan=new OperationPlan(operation,player.getUUID(),OperationPlan.Kind.EDIT,changes,before,NativeInventory.encode(player,slots));var bridge=net.nicomar.nomoregrids.compat.paintableblocks.PaintableBridge.available().orElseThrow();
        permissionHooks.put(operation,()->{for(var e:edits.entrySet())bridge.permission(player,hand,originals.get(e.getKey()),pos(originals.get(e.getKey()).origin16().cell()),e.getValue().nativePlan());});
        try{executePrepared(plan);}finally{permissionHooks.remove(operation);}
    }
    private void completeSupport(ServerPlayer player,ArrayList<OperationPlan.Change> changes,List<ItemStack> slots){completeSupport(player,changes,slots,false);}
    private void completeSupport(ServerPlayer player,ArrayList<OperationPlan.Change> changes,List<ItemStack> slots,boolean preview){
        for(int pass=0;pass<16;pass++){
            WorldConnections.complete(level,snapshot(),changes,catalog);
            var proposed=snapshot().changed(changes.stream().map(OperationPlan.Change::indexChange).toList(),PieceIndex.Limits.DEFAULT);
            Set<CellPos> touched=new HashSet<>();for(var c:changes){c.before().ifPresent(p->touched.addAll(p.worldOccupation().cells(8)));c.after().ifPresent(p->touched.addAll(p.worldOccupation().cells(8)));}
            boolean removed=false;
            for(UUID id:WorldSupport.nearby(proposed,touched)){
                var p=proposed.records().get(id);if(WorldSupport.supported(level,proposed,touched,p,catalog))continue;
                var original=snapshot().records().get(id);if(original==null)throw new IllegalArgumentException("NO_SUPPORT");
                changes.removeIf(c->c.id().equals(id));changes.add(new OperationPlan.Change(id,Optional.of(original),Optional.empty()));
                if(!player.isCreative()&&!preview)recoverLoot(player,original,slots,false);removed=true;
                if(changes.size()>128)throw new IllegalArgumentException("COMPLEXITY_LIMIT: support cascade");
            }
            if(!removed)return;
        }throw new IllegalArgumentException("COMPLEXITY_LIMIT: support cascade did not stabilize");
    }
    public void toggleConnections(ServerPlayer player,UUID id,UUID operation)throws IOException{
        if(repeated(operation,player,OperationPlan.Kind.EDIT))return;var p=snapshot().records().get(id);if(p==null)throw new IllegalArgumentException("STALE_REVISION");
        var flags=new HashSet<>(p.flags());if(!flags.remove(PieceRecord.Flag.CONNECTIONS_LOCKED))flags.add(PieceRecord.Flag.CONNECTIONS_LOCKED);
        var next=new PieceRecord(p.pieceId(),Math.incrementExact(p.revision()),p.adapterId(),p.adapterVersion(),p.sourceBlockId(),p.sourceState(),p.origin16(),p.orientation(),p.pivot16(),p.geometry(),p.geometryPayload(),p.materialPayload(),p.itemPayload(),p.groupId(),flags);
        var slots=NativeInventory.copy(player);var inventory=NativeInventory.encode(player,slots);var changes=new ArrayList<>(List.of(new OperationPlan.Change(id,Optional.of(p),Optional.of(next))));completeSupport(player,changes,slots);
        executePrepared(new OperationPlan(operation,player.getUUID(),OperationPlan.Kind.EDIT,changes,inventory,NativeInventory.encode(player,slots)));
    }
    @Override public void assertServerThread(){if(!level.getServer().isSameThread())throw new IllegalStateException("Server thread required");}
    @Override public void validate(OperationPlan plan,PieceIndex before,PieceIndex after){
        validate(plan,before,after,false);
    }
    private void validate(OperationPlan plan,PieceIndex before,PieceIndex after,boolean preview){
        if(compactionFault)throw new IllegalStateException("Recovery required after checkpoint publication");
        validateEffects(plan);
        if(plan.actor().equals(OperationPlan.WORLD_ACTOR)){validateSystem(plan,before,after);return;}
        ServerPlayer player=level.getServer().getPlayerList().getPlayer(plan.actor());if(player==null||player.level()!=level||player.isSpectator())throw new IllegalArgumentException("Invalid actor");
        if(!NativeInventory.matches(player,plan.resourcesBefore()))throw new IllegalArgumentException("STALE_REVISION");
        if(player.getPersistentData().getLongOr(receiptKey,0)>baseSequence+sequences.size())throw new IllegalArgumentException("INVALID_DATA: receipt is ahead of journal");
        double reach=player.blockInteractionRange();var eye=player.getEyePosition();
        boolean inReach=plan.changes().stream().flatMap(c->java.util.stream.Stream.concat(c.before().stream(),c.after().stream())).anyMatch(p->{
            var b=p.worldOccupation().bounds();double x=Math.max(b.min().x()/16.0,Math.min(b.max().x()/16.0,eye.x)),y=Math.max(b.min().y()/16.0,Math.min(b.max().y()/16.0,eye.y)),z=Math.max(b.min().z()/16.0,Math.min(b.max().z()/16.0,eye.z));return eye.distanceToSqr(new Vec3(x,y,z))<=reach*reach;});
        if(plan.kind()==OperationPlan.Kind.CLAIM||plan.kind()==OperationPlan.Kind.TRANSFER)inReach=plan.affectedCells().stream().allMatch(c->eye.distanceToSqr(new Vec3(c.x()+0.5,c.y()+0.5,c.z()+0.5))<=16);
        if(!inReach)throw new IllegalArgumentException("Interaction out of reach");
        if(plan.kind()==OperationPlan.Kind.PLACE){
            var p=plan.changes().reversed().stream().flatMap(c->c.after().stream()).findFirst().orElseThrow();var b=p.worldOccupation().bounds();
            Vec3 destination=new Vec3(Math.max(b.min().x()/16.0,Math.min(b.max().x()/16.0,eye.x)),Math.max(b.min().y()/16.0,Math.min(b.max().y()/16.0,eye.y)),Math.max(b.min().z()/16.0,Math.min(b.max().z()/16.0,eye.z)));
            var ray=LoadedRay.clip(player,eye,destination,net.minecraft.world.level.ClipContext.Block.COLLIDER);
            if(ray.getType()!=HitResult.Type.MISS&&ray.getLocation().distanceToSqr(destination)>1e-10)throw new IllegalArgumentException("Placement surface is occluded");
        }
        for(var c:plan.changes()){
            c.after().ifPresent(adapters::require);
            for(var p:List.of(c.before(),c.after()))if(p.isPresent()){
                PieceRecord piece=p.orElseThrow();BlockPos owner=pos(new CellPos(piece.ownerSection().x()*16,piece.ownerSection().y()*16,piece.ownerSection().z()*16));
                if(!level.isLoaded(owner))throw new IllegalArgumentException("UNLOADED_REGION");
            }
        }
        for(var cell:plan.affectedCells()){
            BlockPos at=pos(cell);if(!level.isLoaded(at)||level.isOutsideBuildHeight(at)||!level.getWorldBorder().isWithinBounds(at))throw new IllegalArgumentException("UNLOADED_REGION or world boundary");
            if(!level.mayInteract(player,at)||!player.getAbilities().mayBuild)throw new IllegalArgumentException("PROTECTED");
            if(!plan.pieceCells().contains(cell))continue;
            if(!level.getFluidState(at).isEmpty())WorldFluids.kind(level.getFluidState(at));
            var state=level.getBlockState(at);
            if(state.getBlock()==NmgContent.CELL.get()){
                if(!(level.getBlockEntity(at) instanceof CompositeCellEntity entity)||!entity.valid())throw new IllegalArgumentException("INVALID_DATA");
            }else if(!state.isAir()&&!(state.getBlock() instanceof LiquidBlock)&&nativeProjection(cell,before).filter(state::equals).isEmpty()){
                boolean captured=plan.changes().stream().anyMatch(c->c.before().isEmpty()&&c.after().isPresent()&&c.after().orElseThrow().origin16().cell().equals(cell)&&matchesOriginalWorld(c.after().orElseThrow(),at,state));
                if(!captured)throw new IllegalArgumentException("World changed before conversion");
            }
            try{if(CellView.from(cell,after).encode().length>SnapshotAssembler.MAX)throw new IllegalArgumentException("COMPLEXITY_LIMIT: public cell update exceeds 2 MiB");}
            catch(IOException ex){throw new IllegalArgumentException("COMPLEXITY_LIMIT: cell encoding",ex);}
            int collisionBoxes=0;for(UUID id:after.at(cell))collisionBoxes+=after.records().get(id).geometry().collision().boxes().size();
            if(collisionBoxes>512)throw new IllegalArgumentException("COMPLEXITY_LIMIT: cell collision shape");
        }
        Set<SectionPos> budgets=new HashSet<>();for(var cell:plan.affectedCells())budgets.add(cell.section());
        for(var section:budgets){
            try{SectionStore.encode(section,0,after.ownedBy(section).stream().map(after.records()::get).toList());}
            catch(IOException ex){throw new IllegalArgumentException("COMPLEXITY_LIMIT: persistent section",ex);}
            long triangles=0;for(UUID id:after.referencedBy(section)){
                var p=after.records().get(id);triangles+=net.nicomar.nomoregrids.diagnostic.GeometryCache.faces(p.geometry().occupation(),131072).size()*2L;
                if(triangles>262144)throw new IllegalArgumentException("COMPLEXITY_LIMIT: section mesh");
            }
        }
        for(var c:plan.changes())if(c.after().isPresent()){
            var p=c.after().orElseThrow();
            for(var box:p.geometry().collision().transform(Rotation.of(p.orientation()),p.pivot16(),p.origin16()).boxes()){
                var bounds=new AABB(box.min().x()/16.0,box.min().y()/16.0,box.min().z()/16.0,box.max().x()/16.0,box.max().y()/16.0,box.max().z()/16.0);
                if(bounds.intersects(player.getBoundingBox())||!level.getEntityCollisions(null,bounds).isEmpty())throw new IllegalArgumentException("ENTITY_BLOCKED");
            }
            if(!WorldSupport.supported(level,after,plan.affectedCells(),p,catalog))throw new IllegalArgumentException("NO_SUPPORT");
        }
        var event=new BeforeOperationEvent(level,plan,preview);NeoForge.EVENT_BUS.post(event);if(event.isCanceled())throw new IllegalArgumentException("PROTECTED");
        if(!preview&&permissionHooks.containsKey(plan.id()))permissionHooks.get(plan.id()).run();
        for(var c:plan.changes())if(!preview&&c.before().isPresent()&&c.after().isEmpty()){
            var p=c.before().orElseThrow();var permission=new net.nicomar.nomoregrids.event.PieceBreakPermissionEvent(level,pos(p.origin16().cell()),NativeStates.resolve(p),player,p.pieceId());NeoForge.EVENT_BUS.post(permission);if(permission.isCanceled())throw new IllegalArgumentException("PROTECTED");
        }
        if(!NativeInventory.matches(player,plan.resourcesBefore()))throw new IllegalArgumentException("STALE_REVISION: inventory changed during a permission callback");
        validateWorldImages(plan,before);
    }
    private void validateWorldImages(OperationPlan plan,PieceIndex before){
        for(var image:nativeImages.getOrDefault(plan.id(),Map.of()).entrySet())if(!level.isLoaded(image.getKey())||!level.getBlockState(image.getKey()).equals(image.getValue()))throw new IllegalArgumentException("STALE_REVISION: native image");
        var checked=new HashSet<>(plan.pieceCells());plan.effects().stream().filter(e->e.key().startsWith("fluid:")).forEach(e->checked.add(e.owner()));
        for(var cell:checked){
            var at=pos(cell);if(!level.isLoaded(at))throw new IllegalArgumentException("UNLOADED_REGION");var state=level.getBlockState(at);
            if(!before.at(cell).isEmpty()||fluids.at(cell).isPresent()){
                if(nativeProjection(cell,before).filter(state::equals).isEmpty()&&(!(level.getBlockEntity(at) instanceof CompositeCellEntity entity)||!entity.valid()||!entity.view().equals(cellView(cell,before))))throw new IllegalArgumentException("STALE_REVISION: replica requires recovery");
            }else if(state.getBlock()==NmgContent.CELL.get())throw new IllegalArgumentException("INVALID_DATA: unrecognized cell is retained");
            else if(!state.isAir()&&!(state.getBlock() instanceof LiquidBlock)){
                boolean expected=plan.changes().stream().anyMatch(c->c.before().isEmpty()&&c.after().isPresent()&&c.after().orElseThrow().origin16().cell().equals(cell)&&matchesOriginalWorld(c.after().orElseThrow(),at,state));
                if(!expected)throw new IllegalArgumentException("STALE_REVISION: native world changed");
            }
        }
    }
    private boolean matchesOriginalWorld(PieceRecord p,BlockPos at,net.minecraft.world.level.block.state.BlockState state){
        if(!NativeStates.original(p).equals(state))return false;
        if(WorldFunctionals.piece(p))return functionals.matchesNative(p,at);
        return !p.adapterId().value().equals("nomoregrids:paintableblocks")||net.nicomar.nomoregrids.compat.paintableblocks.PaintableBridge.available().filter(b->b.matchesOriginal(p,level,at)).isPresent();
    }
    @Override public void install(OperationPlan plan,PieceIndex after)throws IOException{
        long sequence=sequences.computeIfAbsent(plan.id(),key->baseSequence+sequences.size()+1);history.put(plan.id(),plan);
        for(var c:plan.changes())knownIds.add(c.id());
        reconcileResources(plan,sequence);
        applyEffects(plan);
        writing++;try{var cells=new HashSet<>(plan.pieceCells());plan.effects().stream().filter(e->e.key().startsWith("fluid:")||e.key().startsWith("native:")).forEach(e->cells.add(e.owner()));for(var cell:cells)syncCell(cell,after);}finally{writing--;}
        Set<SectionPos> effectSections=new HashSet<>();plan.effects().forEach(e->effectSections.add(e.owner().section()));
        for(var section:effectSections)effectStore.write(section,sequence,sectionEffects(section));
        Set<SectionPos> sections=new HashSet<>();for(var c:plan.changes()){c.before().ifPresent(p->sections.add(p.ownerSection()));c.after().ifPresent(p->sections.add(p.ownerSection()));}
        for(var section:sections)store.write(section,sequence,after.ownedBy(section).stream().map(after.records()::get).toList());
    }
    private Map<String,WorldEffectStore.Value> sectionEffects(SectionPos section){var result=new TreeMap<String,WorldEffectStore.Value>();effects.forEach((key,value)->{if(value.owner().section().equals(section))result.put(key,value);});return result;}
    private void validateEffects(OperationPlan plan){
        for(var e:plan.effects())if(!Objects.equals(Optional.ofNullable(effects.get(e.key())).map(WorldEffectStore.Value::data),e.before())||effects.containsKey(e.key())&&!effects.get(e.key()).owner().equals(e.owner()))throw new IllegalArgumentException("STALE_REVISION: world effects");
        var afterIndex=snapshot().changed(plan.changes().stream().map(OperationPlan.Change::indexChange).toList(),PieceIndex.Limits.DEFAULT);
        for(var e:plan.effects())if(e.key().startsWith("native:")&&e.after().isPresent()&&!afterIndex.at(e.owner()).isEmpty())throw new IllegalArgumentException("OVERLAP: native mechanism goal");
        var proposed=new TreeMap<>(effects);for(var e:plan.effects()){if(e.after().isPresent())proposed.put(e.key(),new WorldEffectStore.Value(e.owner(),e.after().orElseThrow()));else proposed.remove(e.key());}
        for(var section:plan.effects().stream().map(e->e.owner().section()).distinct().toList()){
            var values=new TreeMap<String,WorldEffectStore.Value>();proposed.forEach((k,v)->{if(v.owner().section().equals(section))values.put(k,v);});try{WorldEffectStore.encode(1,values);}catch(IOException ex){throw new IllegalArgumentException("COMPLEXITY_LIMIT: section effects",ex);}
        }
    }
    private void applyEffects(OperationPlan plan)throws IOException{
        for(var e:plan.effects()){
            var current=Optional.ofNullable(effects.get(e.key())).map(WorldEffectStore.Value::data);if(!current.equals(e.before())&&!current.equals(e.after()))throw new IOException("Conflicting world effect; original retained");
            if(e.after().isPresent())effects.put(e.key(),new WorldEffectStore.Value(e.owner(),e.after().orElseThrow()));else effects.remove(e.key());
        }
    }
    private void validateSystem(OperationPlan plan,PieceIndex before,PieceIndex after){
        if(plan.kind()!=OperationPlan.Kind.SYSTEM)throw new IllegalArgumentException("Invalid world operation");
        for(var cell:plan.affectedCells()){var at=pos(cell);if(!level.isLoaded(at)||level.isOutsideBuildHeight(at)||!level.getWorldBorder().isWithinBounds(at))throw new IllegalArgumentException("UNLOADED_REGION");}
        for(var c:plan.changes())c.after().ifPresent(adapters::require);
        try{for(var cell:plan.pieceCells()){
            CellView.from(cell,after).encode();int boxes=0;for(UUID id:after.at(cell))boxes+=after.records().get(id).geometry().collision().boxes().size();if(boxes>512)throw new IllegalArgumentException("COMPLEXITY_LIMIT: world collision");
        }for(var section:plan.pieceCells().stream().map(CellPos::section).distinct().toList())SectionStore.encode(section,0,after.ownedBy(section).stream().map(after.records()::get).toList());}
        catch(IOException ex){throw new IllegalArgumentException("COMPLEXITY_LIMIT: world projection",ex);}
        validateWorldImages(plan,before);
        var event=new BeforeOperationEvent(level,plan);NeoForge.EVENT_BUS.post(event);if(event.isCanceled())throw new IllegalArgumentException("PROTECTED");validateEffects(plan);validateWorldImages(plan,before);
    }
    public void executeWorld(List<OperationPlan.Change> changes,List<OperationPlan.Effect> effects)throws IOException{
        if(changes.isEmpty()&&effects.isEmpty())return;executePrepared(new OperationPlan(UUID.randomUUID(),OperationPlan.WORLD_ACTOR,OperationPlan.Kind.SYSTEM,changes,BinaryData.EMPTY,BinaryData.EMPTY,effects));
    }
    public static String nativeKey(CellPos c){return "native:"+c.x()+":"+c.y()+":"+c.z();}
    public Optional<net.minecraft.world.level.block.state.BlockState> nativeGoal(CellPos cell){var value=effects.get(nativeKey(cell));if(value==null)return Optional.empty();try{var image=NativeStateImage.decode(value.data());return Optional.of(NativeStates.resolve(image.block(),image.properties()));}catch(IOException ex){throw new IllegalArgumentException("INVALID_DATA: native goal",ex);}}
    public OperationPlan.Effect nativeChange(CellPos cell,net.minecraft.world.level.block.state.BlockState after){var old=Optional.ofNullable(effects.get(nativeKey(cell))).map(WorldEffectStore.Value::data);var data=new NativeStateImage(new ResourceId(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(after.getBlock()).toString()),NativeStates.properties(after)).encode();return new OperationPlan.Effect(nativeKey(cell),cell,old,Optional.of(data));}
    public void releaseNativeGoal(CellPos cell)throws IOException{var old=effects.get(nativeKey(cell));if(old!=null)executeWorld(List.of(),List.of(new OperationPlan.Effect(nativeKey(cell),cell,Optional.of(old.data()),Optional.empty())));}
    private void syncCell(CellPos cell,PieceIndex index)throws IOException{
        BlockPos at=pos(cell);if(!level.isLoaded(at))throw new IOException("Region unloaded during commit");
        var goal=nativeGoal(cell);if(goal.isPresent()){
            if(!index.at(cell).isEmpty()||fluids.at(cell).isPresent())throw new IOException("Native goal conflicts with component authority");if(!level.getBlockState(at).equals(goal.orElseThrow())&&!level.setBlock(at,goal.orElseThrow(),Block.UPDATE_ALL))throw new IOException("Native mechanism projection rejected");return;
        }
        var nativeState=fluids.at(cell).isEmpty()?nativeProjection(cell,index):Optional.<net.minecraft.world.level.block.state.BlockState>empty();if(nativeState.isPresent()){
            if(!level.getBlockState(at).equals(nativeState.orElseThrow())&&!level.setBlock(at,nativeState.orElseThrow(),Block.UPDATE_ALL))throw new IOException("Native restoration rejected");return;
        }
        if(index.at(cell).isEmpty()&&fluids.at(cell).isEmpty()){
            if(level.getBlockState(at).getBlock()==NmgContent.CELL.get())level.setBlock(at,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);return;
        }
        if(level.getBlockState(at).getBlock()!=NmgContent.CELL.get()&&!level.setBlock(at,NmgContent.CELL.get().defaultBlockState(),Block.UPDATE_ALL))throw new IOException("Container placement rejected");
        if(!(level.getBlockEntity(at) instanceof CompositeCellEntity entity))throw new IOException("Missing cell entity");entity.install(cellView(cell,index));
    }
    public Optional<net.minecraft.world.level.block.state.BlockState> nativeProjection(CellPos cell){return nativeProjection(cell,snapshot());}
    private Optional<net.minecraft.world.level.block.state.BlockState> nativeProjection(CellPos cell,PieceIndex index){
        if(fluids.at(cell).isPresent()||index.at(cell).size()!=1)return Optional.empty();var p=index.records().get(index.at(cell).iterator().next());
        if(!p.adapterId().value().equals("nomoregrids:box")||p.flags().contains(PieceRecord.Flag.SUBSTITUTE)||p.orientation()!=0||p.groupId().isPresent()||!p.origin16().equals(new GridPoint(cell.x()*16,cell.y()*16,cell.z()*16))||p.worldOccupation().cells(8).size()!=1)return Optional.empty();
        try{var state=NativeStates.resolve(p);if(!catalog.eligible(state))return Optional.empty();var captured=NativeStates.capture(state,level,pos(cell),p.pieceId());if(!captured.geometry().equals(p.geometry()))return Optional.empty();return Optional.of(state);}catch(RuntimeException ex){return Optional.empty();}
    }
    public void releaseNative(CellPos cell)throws IOException{
        if(nativeProjection(cell).isEmpty())return;var p=snapshot().records().get(snapshot().at(cell).iterator().next());executeWorld(List.of(new OperationPlan.Change(p.pieceId(),Optional.of(p),Optional.empty())),List.of());
    }
    public void repairLoadedCells(Collection<BlockPos> existingCells)throws IOException{
        writing++;try{repairCells(existingCells);}finally{writing--;}
    }
    private void repairCells(Collection<BlockPos> existingCells)throws IOException{
        if(quarantined())throw new IOException("Committed operation requires recovery; replicas retained");
        Set<CellPos> cells=new HashSet<>();for(var p:existingCells)cells.add(new CellPos(p.getX(),p.getY(),p.getZ()));
        for(var cell:cells){
            var state=level.getBlockState(pos(cell));
            if(level.getBlockEntity(pos(cell)) instanceof CompositeCellEntity entity&&!entity.valid())continue;
            if(level.getBlockEntity(pos(cell)) instanceof CompositeCellEntity entity&&entity.view().replicas().stream().anyMatch(p->!knownPiece(p.pieceId())))continue;
            boolean expectedNative=nativeProjection(cell,snapshot()).filter(state::equals).isPresent()||snapshot().at(cell).stream().map(snapshot().records()::get).anyMatch(p->p.orientation()==0&&p.origin16().equals(new GridPoint(cell.x()*16,cell.y()*16,cell.z()*16))&&!p.flags().contains(PieceRecord.Flag.SUBSTITUTE)&&matchesOriginalWorld(p,pos(cell),state));
            if(nativeGoal(cell).isPresent()||state.isAir()||state.getBlock()==NmgContent.CELL.get()||state.getBlock() instanceof LiquidBlock||expectedNative)syncCell(cell,snapshot());
            else if(!snapshot().at(cell).isEmpty()||fluids.at(cell).isPresent())throw new IOException("Native block conflicts with committed NMG authority at "+cell);
        }
    }
    public Set<BlockPos> cellsInChunk(net.minecraft.world.level.chunk.LevelChunk chunk){
        var chunkPos=chunk.getPos();Set<BlockPos> cells=new HashSet<>();
        chunk.getBlockEntities().forEach((pos,entity)->{if(entity instanceof CompositeCellEntity)cells.add(pos);});
        for(int y=level.getMinSectionY();y<level.getMaxSectionY();y++)for(UUID id:snapshot().referencedBy(new SectionPos(chunkPos.x(),y,chunkPos.z())))
            for(var cell:snapshot().records().get(id).worldOccupation().cells(8))if(Math.floorDiv(cell.x(),16)==chunkPos.x()&&Math.floorDiv(cell.z(),16)==chunkPos.z())cells.add(pos(cell));
        for(var e:effects.entrySet())if(e.getKey().startsWith("native:")){var c=e.getValue().owner();if(Math.floorDiv(c.x(),16)==chunkPos.x()&&Math.floorDiv(c.z(),16)==chunkPos.z())cells.add(pos(c));}
        for(var c:fluids.snapshot().keySet())if(Math.floorDiv(c.x(),16)==chunkPos.x()&&Math.floorDiv(c.z(),16)==chunkPos.z())cells.add(pos(c));
        return Set.copyOf(cells);
    }
    public void repairChunk(net.minecraft.world.level.chunk.LevelChunk chunk)throws IOException{
        repairLoadedCells(cellsInChunk(chunk));
    }
    public void externalChanged(Set<CellPos> observed)throws IOException{
        if(quarantined())throw new IOException("Recovery required");
        var changes=new ArrayList<OperationPlan.Change>();var auxiliary=new ArrayList<OperationPlan.Effect>();
        for(int pass=0;pass<16;pass++){
            WorldConnections.complete(level,snapshot(),changes,catalog,observed);
            var proposed=snapshot().changed(changes.stream().map(OperationPlan.Change::indexChange).toList(),PieceIndex.Limits.DEFAULT);boolean removed=false;
            Set<CellPos> touched=new HashSet<>(observed);for(var c:changes){c.before().ifPresent(p->touched.addAll(p.worldOccupation().cells(8)));c.after().ifPresent(p->touched.addAll(p.worldOccupation().cells(8)));}
            // Observed native cells must still be queried from the actual world, not treated as removed.
            var changedCells=new HashSet<CellPos>();for(var c:changes){c.before().ifPresent(p->changedCells.addAll(p.worldOccupation().cells(8)));c.after().ifPresent(p->changedCells.addAll(p.worldOccupation().cells(8)));}
            for(UUID id:WorldSupport.nearby(proposed,touched)){
                var p=proposed.records().get(id);if(p.flags().contains(PieceRecord.Flag.SUBSTITUTE)||WorldSupport.supported(level,proposed,changedCells,p,catalog))continue;
                var old=snapshot().records().get(id);if(old==null)throw new IllegalArgumentException("NO_SUPPORT: native connection capture");
                changes.removeIf(c->c.id().equals(id));changes.add(new OperationPlan.Change(id,Optional.of(old),Optional.empty()));WorldLoot.queue(level,old,auxiliary);removed=true;
            }
            if(!removed){executeWorld(changes,auxiliary);return;}
        }throw new IllegalArgumentException("COMPLEXITY_LIMIT: external support cascade");
    }
    public void removeWorld(Collection<UUID> ids,boolean drops)throws IOException{
        if(ids.isEmpty())return;var changes=new ArrayList<OperationPlan.Change>();var auxiliary=new ArrayList<OperationPlan.Effect>();
        for(var id:new TreeSet<>(ids)){var old=snapshot().records().get(id);if(old==null||old.flags().contains(PieceRecord.Flag.SUBSTITUTE))continue;adapters.require(old);if(drops)WorldLoot.queue(level,old,auxiliary);changes.add(new OperationPlan.Change(id,Optional.of(old),Optional.empty()));}
        for(int pass=0;pass<16;pass++){
            WorldConnections.complete(level,snapshot(),changes,catalog);var proposed=snapshot().changed(changes.stream().map(OperationPlan.Change::indexChange).toList(),PieceIndex.Limits.DEFAULT);var touched=new HashSet<CellPos>();for(var c:changes){c.before().ifPresent(p->touched.addAll(p.worldOccupation().cells(8)));c.after().ifPresent(p->touched.addAll(p.worldOccupation().cells(8)));}boolean removed=false;
            for(var id:WorldSupport.nearby(proposed,touched)){var p=proposed.records().get(id);if(p.flags().contains(PieceRecord.Flag.SUBSTITUTE)||WorldSupport.supported(level,proposed,touched,p,catalog))continue;var old=snapshot().records().get(id);if(old==null)throw new IllegalArgumentException("NO_SUPPORT");changes.removeIf(c->c.id().equals(id));changes.add(new OperationPlan.Change(id,Optional.of(old),Optional.empty()));WorldLoot.queue(level,old,auxiliary);removed=true;}
            if(!removed){executeWorld(changes,auxiliary);return;}
        }throw new IllegalArgumentException("COMPLEXITY_LIMIT: material support cascade");
    }
    public void claimLoot(ServerPlayer player)throws IOException{
        if(player.isSpectator()||player.level()!=level||quarantined())return;var claimed=new ArrayList<OperationPlan.Effect>();var slots=NativeInventory.copy(player);var before=NativeInventory.encode(player,slots);
        for(var entry:effects.entrySet())if(entry.getKey().startsWith("loot:")&&claimed.size()<8){var value=entry.getValue();var c=value.owner();
            if(!level.isLoaded(pos(c))||player.getEyePosition().distanceToSqr(new Vec3(c.x()+0.5,c.y()+0.5,c.z()+0.5))>9)continue;
            var attempt=slots.stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            try{for(var stack:WorldLoot.decode(level,value.data()))NativeInventory.insert(attempt,stack);}catch(IllegalArgumentException full){continue;}
            slots=attempt;claimed.add(new OperationPlan.Effect(entry.getKey(),c,Optional.of(value.data()),Optional.empty()));
        }
        if(!claimed.isEmpty())executePrepared(new OperationPlan(UUID.randomUUID(),player.getUUID(),OperationPlan.Kind.CLAIM,List.of(),before,NativeInventory.encode(player,slots),claimed));
    }
    private boolean knownPiece(UUID id){if(knownIds.contains(id))return true;if(archive==null)return false;try{return archive.knownPiece(id);}catch(IOException ex){throw new UncheckedIOException(ex);}}
    public long compact()throws IOException{
        assertServerThread();if(quarantined())throw new IOException("Recovery required before compaction");long sequence=baseSequence+sequences.size();if(sequence==baseSequence)return sequence;
        var receipts=new HashMap<UUID,Long>();if(archive!=null)receipts.putAll(archive.state().receipts());var stamps=new HashMap<UUID,JournalArchive.Stamp>();
        for(var plan:history.values()){
            long n=sequences.get(plan.id());if(!plan.actor().equals(OperationPlan.WORLD_ACTOR))receipts.merge(plan.actor(),n,Math::max);
            stamps.put(plan.id(),new JournalArchive.Stamp(plan.id(),plan.actor(),plan.kind(),n,SnapshotAssembler.sha256(OperationCodec.encode(plan))));
        }
        for(var receipt:receipts.entrySet())if(watermark(receipt.getKey())!=receipt.getValue())throw new IOException("Compaction requires every actor's exact durable receipt");
        var prepared=JournalArchive.prepare(root,new JournalArchive.State(sequence,snapshot().records(),effects,receipts),archive,stamps,knownIds);var nextJournal=new DurableJournal(prepared.activeLog());
        try{prepared.publish();}catch(IOException|RuntimeException ex){nextJournal.close();throw ex;}
        // Reopen verifies the published checkpoint and every ledger hash before releasing old history.
        JournalArchive verified;try{verified=JournalArchive.open(root).orElseThrow();}catch(IOException|RuntimeException ex){compactionFault=true;NmgRuntime.failed(level,ex);try{nextJournal.close();}catch(IOException closing){ex.addSuppressed(closing);}throw ex;}var oldJournal=journal;var current=snapshot();journal=nextJournal;archive=verified;baseSequence=sequence;history.clear();sequences.clear();knownIds.clear();knownIds.addAll(current.records().keySet());engine=new OperationEngine(current,journal,this,PieceIndex.Limits.DEFAULT);
        try{oldJournal.close();}catch(IOException ex){net.nicomar.nomoregrids.NoMoreGrids.LOGGER.warn("Old compacted journal close failed; new generation is durable",ex);}
        try{prepared.prune();}catch(IOException ex){net.nicomar.nomoregrids.NoMoreGrids.LOGGER.warn("Compaction retained older files after cleanup failure",ex);}return sequence;
    }
    @Override public boolean alreadyApplied(UUID id)throws IOException{
        OperationPlan plan=history.get(id);Long sequence=sequences.get(id);if(plan==null||sequence==null||!receipt(plan,sequence))return false;
        for(var c:plan.changes())for(var p:List.of(c.before(),c.after()))if(p.isPresent()&&store.read(p.orElseThrow().ownerSection()).checkpoint()<sequence)return false;for(var e:plan.effects())if(effectStore.checkpoint(e.owner().section())<sequence)return false;return true;
    }
    @Override public void afterCommit(OperationPlan plan){for(var cell:plan.affectedCells())WorldMutationHooks.changed(level,pos(cell));NeoForge.EVENT_BUS.post(new net.nicomar.nomoregrids.event.AfterOperationEvent(level,plan));}
    @Override public void close()throws IOException{journal.close();}
}
