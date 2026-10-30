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
import net.nicomar.nomoregrids.network.*;
import net.nicomar.nomoregrids.operation.*;
import net.nicomar.nomoregrids.persistence.*;
import net.nicomar.nomoregrids.registry.NmgContent;
import net.nicomar.nomoregrids.world.*;

/** Experimental server host. Authoritative history is durable; chunk replicas can be rebuilt on natural load. */
public final class WorldService implements OperationEngine.Host,AutoCloseable {
    private final ServerLevel level;
    private final DurableJournal journal;
    private final SectionStore store;
    private final AdapterRegistry adapters;
    private final VanillaCatalog catalog=new VanillaCatalog();
    private final String receiptKey;
    private final Map<UUID,Long> sequences=new HashMap<>();
    private final Map<UUID,OperationPlan> history=new HashMap<>();
    private final Set<UUID> knownIds=new HashSet<>();
    private OperationEngine engine;
    private boolean recovering;

    public WorldService(ServerLevel level,AdapterRegistry adapters)throws IOException{
        this.level=level;this.adapters=adapters;
        String dimension=level.dimension().identifier().toString();
        String hash=java.util.HexFormat.of().formatHex(SnapshotAssembler.sha256(dimension.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        receiptKey="nmg_applied_"+hash;Path root=level.getServer().getWorldPath(LevelResource.ROOT).resolve("nomoregrids").resolve(hash);
        store=new SectionStore(root.resolve("sections"));journal=new DurableJournal(root.resolve("operations.log"));
        try{restore();}catch(IOException|RuntimeException ex){journal.close();throw ex;}
    }
    private void restore()throws IOException{
        recovering=true;var records=new HashMap<UUID,PieceRecord>();long sequence=0;
        for(var plan:journal.committed()){
            sequences.put(plan.id(),++sequence);history.put(plan.id(),plan);
            for(var c:plan.changes()){
                knownIds.add(c.id());
                if(!Objects.equals(Optional.ofNullable(records.get(c.id())),c.before()))throw new IOException("Journal history conflict");
                if(c.after().isPresent())records.put(c.id(),c.after().orElseThrow());else records.remove(c.id());
            }
            reconcileResources(plan,sequence);
        }
        Map<Long,UUID> owners=new HashMap<>();sequences.forEach((id,seq)->owners.put(seq,history.get(id).actor()));
        for(UUID actor:history.values().stream().map(OperationPlan::actor).collect(java.util.stream.Collectors.toSet())){
            long marker=watermark(actor);if(marker!=0&&!actor.equals(owners.get(marker)))throw new IOException("Player receipt does not belong to this journal history; data retained");
        }
        for(var p:List.copyOf(records.values()))try{adapters.require(p).occupation(p);NativeStates.resolve(p);NativeStates.original(p);}catch(RuntimeException ex){
            var flags=new HashSet<>(p.flags());flags.add(PieceRecord.Flag.SUBSTITUTE);records.put(p.pieceId(),new PieceRecord(p.pieceId(),p.revision(),p.adapterId(),p.adapterVersion(),p.sourceBlockId(),p.sourceState(),p.origin16(),p.orientation(),p.pivot16(),p.geometry(),p.geometryPayload(),p.materialPayload(),p.itemPayload(),p.groupId(),flags));
        }
        var index=PieceIndex.retained(records.values(),adapters::overlap);
        engine=new OperationEngine(index,journal,this,PieceIndex.Limits.DEFAULT);recovering=false;
    }
    private boolean receipt(OperationPlan plan,long sequence)throws IOException{
        return watermark(plan.actor())>=sequence;
    }
    private long watermark(UUID actor)throws IOException{
        // A volatile entity tag is not proof of a completed save, even when the actor is online.
        var saved=level.getServer().getPlayerList().loadPlayerData(new NameAndId(actor,"NMG-recovery"));
        long marker=saved.map(tag->tag.getCompoundOrEmpty("NeoForgeData").getLongOr(receiptKey,0)).orElse(0L);
        if(marker<0)throw new IOException("Negative resource receipt");return marker;
    }
    private void reconcileResources(OperationPlan plan,long sequence)throws IOException{
        if(receipt(plan,sequence))return;
        if(recovering)for(var change:plan.changes())for(var p:List.of(change.before(),change.after()))if(p.isPresent()&&store.read(p.orElseThrow().ownerSection()).checkpoint()>=sequence)
            throw new IOException("World checkpoint exists without its player receipt; operation quarantined");
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
        try(var directory=FileChannel.open(playerFile.getParent(),StandardOpenOption.READ)){directory.force(true);}
        var saved=level.getServer().getPlayerList().loadPlayerData(actor.nameAndId());
        if(saved.isEmpty()||saved.orElseThrow().getCompoundOrEmpty("NeoForgeData").getLongOr(receiptKey,0)<sequence)throw new IOException("Receipt not persisted with player inventory");
    }
    public PieceIndex snapshot(){return engine.snapshot();}
    public boolean quarantined(){return engine.quarantined();}
    public static BlockPos pos(CellPos p){return new BlockPos(Math.toIntExact(p.x()),Math.toIntExact(p.y()),Math.toIntExact(p.z()));}
    public void place(ServerPlayer player,InteractionHand hand,BlockHitResult hit,ModePayload mode)throws IOException{
        engine.execute(placementPlan(player,hand,hit,mode,UUID.randomUUID()));
    }
    public void place(ServerPlayer player,InteractionHand hand,BlockHitResult hit,ModePayload mode,UUID operation)throws IOException{
        if(repeated(operation,player,OperationPlan.Kind.PLACE))return;engine.execute(placementPlan(player,hand,hit,mode,operation));
    }
    public void probe(ServerPlayer player,InteractionHand hand,BlockHitResult hit,ModePayload mode)throws IOException{
        var plan=placementPlan(player,hand,hit,mode,UUID.randomUUID());var before=snapshot();var after=before.changed(plan.changes().stream().map(OperationPlan.Change::indexChange).toList(),PieceIndex.Limits.DEFAULT);validate(plan,before,after);
    }
    private boolean repeated(UUID id,ServerPlayer actor,OperationPlan.Kind kind){
        var old=history.get(id);if(old==null)return false;if(!old.actor().equals(actor.getUUID())||old.kind()!=kind)throw new IllegalArgumentException("Operation ID conflict");return true;
    }
    private OperationPlan placementPlan(ServerPlayer player,InteractionHand hand,BlockHitResult hit,ModePayload mode,UUID operation)throws IOException{
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
            if(!state.isAir()&&state.getBlock()!=NmgContent.CELL.get()){
                if(!catalog.eligible(state))throw new IllegalArgumentException("UNSUPPORTED_ADAPTER");
                PieceRecord nativePiece=NativeStates.capture(state,level,at,UUID.randomUUID());
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
            if(replaced!=null)recoverLoot(player,replaced,slots);
        }
        completeSupport(player,changes,slots);
        return new OperationPlan(operation,player.getUUID(),OperationPlan.Kind.PLACE,changes,before,NativeInventory.encode(player,slots));
    }
    public void remove(ServerPlayer player,UUID id)throws IOException{
        remove(player,id,UUID.randomUUID());
    }
    public void remove(ServerPlayer player,UUID id,UUID operation)throws IOException{
        if(repeated(operation,player,OperationPlan.Kind.REMOVE))return;
        PieceRecord target=snapshot().records().get(id);if(target==null)throw new IllegalArgumentException("STALE_REVISION");adapters.require(target);
        if(!target.adapterId().value().equals("nomoregrids:box"))throw new IllegalArgumentException("Use the adapter's native recovery bridge; generic loot is forbidden");
        var slots=NativeInventory.copy(player);BinaryData before=NativeInventory.encode(player,slots);
        if(!player.isCreative())recoverLoot(player,target,slots);
        var changes=new ArrayList<>(List.of(new OperationPlan.Change(id,Optional.of(target),Optional.empty())));completeSupport(player,changes,slots);
        var plan=new OperationPlan(operation,player.getUUID(),OperationPlan.Kind.REMOVE,changes,before,NativeInventory.encode(player,slots));engine.execute(plan);
    }
    private void recoverLoot(ServerPlayer player,PieceRecord target,List<ItemStack> slots){
        recoverLoot(player,target,slots,true);
    }
    private void recoverLoot(ServerPlayer player,PieceRecord target,List<ItemStack> slots,boolean damageTool){
        if(!target.adapterId().value().equals("nomoregrids:box"))throw new IllegalArgumentException("Native adapter recovery is required");
        var state=NativeStates.resolve(target);var at=pos(target.origin16().cell());
        int length=net.nicomar.nomoregrids.material.FragmentItem.length(target.itemPayload());
        if(player.hasCorrectToolForDrops(state))for(var item:Block.getDrops(state,level,at,null,player,player.getMainHandItem())){
            if(length<16&&item.getItem()==state.getBlock().asItem())item=net.nicomar.nomoregrids.material.FragmentItem.create(state.getBlock(),length).copyWithCount(item.getCount());NativeInventory.insert(slots,item);
        }
        int selected=player.getInventory().getSelectedSlot();ItemStack tool=slots.get(selected);
        if(damageTool&&tool.isDamageableItem()){
            int damage=EnchantmentHelper.processDurabilityChange(level,tool,1);tool.setDamageValue(Math.addExact(tool.getDamageValue(),damage));
            if(tool.getDamageValue()>=tool.getMaxDamage())slots.set(selected,ItemStack.EMPTY);
        }
    }
    private void completeSupport(ServerPlayer player,ArrayList<OperationPlan.Change> changes,List<ItemStack> slots){
        for(int pass=0;pass<16;pass++){
            WorldConnections.complete(level,snapshot(),changes,catalog);
            var proposed=snapshot().changed(changes.stream().map(OperationPlan.Change::indexChange).toList(),PieceIndex.Limits.DEFAULT);
            Set<CellPos> touched=new HashSet<>();for(var c:changes){c.before().ifPresent(p->touched.addAll(p.worldOccupation().cells(8)));c.after().ifPresent(p->touched.addAll(p.worldOccupation().cells(8)));}
            boolean removed=false;
            for(UUID id:WorldSupport.nearby(proposed,touched)){
                var p=proposed.records().get(id);if(WorldSupport.supported(level,proposed,touched,p,catalog))continue;
                var original=snapshot().records().get(id);if(original==null)throw new IllegalArgumentException("NO_SUPPORT");
                changes.removeIf(c->c.id().equals(id));changes.add(new OperationPlan.Change(id,Optional.of(original),Optional.empty()));
                if(!player.isCreative())recoverLoot(player,original,slots,false);removed=true;
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
        engine.execute(new OperationPlan(operation,player.getUUID(),OperationPlan.Kind.EDIT,changes,inventory,NativeInventory.encode(player,slots)));
    }
    @Override public void assertServerThread(){if(!level.getServer().isSameThread())throw new IllegalStateException("Server thread required");}
    @Override public void validate(OperationPlan plan,PieceIndex before,PieceIndex after){
        ServerPlayer player=level.getServer().getPlayerList().getPlayer(plan.actor());if(player==null||player.level()!=level||player.isSpectator())throw new IllegalArgumentException("Invalid actor");
        if(!NativeInventory.matches(player,plan.resourcesBefore()))throw new IllegalArgumentException("STALE_REVISION");
        if(player.getPersistentData().getLongOr(receiptKey,0)>sequences.size())throw new IllegalArgumentException("INVALID_DATA: receipt is ahead of journal");
        double reach=player.blockInteractionRange();var eye=player.getEyePosition();
        boolean inReach=plan.changes().stream().flatMap(c->java.util.stream.Stream.concat(c.before().stream(),c.after().stream())).anyMatch(p->{
            var b=p.worldOccupation().bounds();double x=Math.max(b.min().x()/16.0,Math.min(b.max().x()/16.0,eye.x)),y=Math.max(b.min().y()/16.0,Math.min(b.max().y()/16.0,eye.y)),z=Math.max(b.min().z()/16.0,Math.min(b.max().z()/16.0,eye.z));return eye.distanceToSqr(new Vec3(x,y,z))<=reach*reach;});
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
            if(!level.getFluidState(at).isEmpty())throw new IllegalArgumentException("Partial-fluid host not enabled for this operation");
            var state=level.getBlockState(at);
            if(state.getBlock()==NmgContent.CELL.get()){
                if(!(level.getBlockEntity(at) instanceof CompositeCellEntity entity)||!entity.valid())throw new IllegalArgumentException("INVALID_DATA");
            }else if(!state.isAir()){
                boolean captured=plan.changes().stream().anyMatch(c->c.before().isEmpty()&&c.after().isPresent()&&c.after().orElseThrow().origin16().cell().equals(cell)&&NativeStates.original(c.after().orElseThrow()).equals(state));
                if(!captured)throw new IllegalArgumentException("World changed before conversion");
            }
            try{if(CellView.from(cell,after).encode().length>SnapshotAssembler.CHUNK)throw new IllegalArgumentException("COMPLEXITY_LIMIT: public cell update exceeds 64 KiB");}
            catch(IOException ex){throw new IllegalArgumentException("COMPLEXITY_LIMIT: cell encoding",ex);}
            int collisionBoxes=0;for(UUID id:after.at(cell))collisionBoxes+=after.records().get(id).geometry().collision().boxes().size();
            if(collisionBoxes>512)throw new IllegalArgumentException("COMPLEXITY_LIMIT: cell collision shape");
        }
        Set<SectionPos> budgets=new HashSet<>();for(var cell:plan.affectedCells())budgets.add(cell.section());
        for(var section:budgets){
            try{SectionStore.encode(section,0,after.ownedBy(section).stream().map(after.records()::get).toList());}
            catch(IOException ex){throw new IllegalArgumentException("COMPLEXITY_LIMIT: persistent section",ex);}
            long triangles=0;for(UUID id:after.referencedBy(section)){
                var p=after.records().get(id);triangles+=BoxMesh.extract(p.geometry().occupation(),131072).size()*2L;
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
        var event=new BeforeOperationEvent(level,plan);NeoForge.EVENT_BUS.post(event);if(event.isCanceled())throw new IllegalArgumentException("PROTECTED");
        for(var c:plan.changes())if(c.before().isPresent()&&c.after().isEmpty()){
            var p=c.before().orElseThrow();var permission=new net.nicomar.nomoregrids.event.PieceBreakPermissionEvent(level,pos(p.origin16().cell()),NativeStates.resolve(p),player,p.pieceId());NeoForge.EVENT_BUS.post(permission);if(permission.isCanceled())throw new IllegalArgumentException("PROTECTED");
        }
        if(!NativeInventory.matches(player,plan.resourcesBefore()))throw new IllegalArgumentException("STALE_REVISION: inventory changed during a permission callback");
    }
    @Override public void install(OperationPlan plan,PieceIndex after)throws IOException{
        long sequence=sequences.computeIfAbsent(plan.id(),key->(long)sequences.size()+1);history.put(plan.id(),plan);
        for(var c:plan.changes())knownIds.add(c.id());
        reconcileResources(plan,sequence);
        for(var cell:plan.affectedCells())syncCell(cell,after);
        Set<SectionPos> sections=new HashSet<>();for(var c:plan.changes()){c.before().ifPresent(p->sections.add(p.ownerSection()));c.after().ifPresent(p->sections.add(p.ownerSection()));}
        for(var section:sections)store.write(section,sequence,after.ownedBy(section).stream().map(after.records()::get).toList());
    }
    private void syncCell(CellPos cell,PieceIndex index)throws IOException{
        BlockPos at=pos(cell);if(!level.isLoaded(at))throw new IOException("Region unloaded during commit");
        if(index.at(cell).isEmpty()){
            if(level.getBlockState(at).getBlock()==NmgContent.CELL.get())level.setBlock(at,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);return;
        }
        if(level.getBlockState(at).getBlock()!=NmgContent.CELL.get()&&!level.setBlock(at,NmgContent.CELL.get().defaultBlockState(),Block.UPDATE_ALL))throw new IOException("Container placement rejected");
        if(!(level.getBlockEntity(at) instanceof CompositeCellEntity entity))throw new IOException("Missing cell entity");entity.install(CellView.from(cell,index));
    }
    public void repairLoadedCells(Collection<BlockPos> existingCells)throws IOException{
        if(engine.quarantined())throw new IOException("Committed operation requires recovery; replicas retained");
        Set<CellPos> cells=new HashSet<>();for(var p:existingCells)cells.add(new CellPos(p.getX(),p.getY(),p.getZ()));
        for(var cell:cells){
            var state=level.getBlockState(pos(cell));
            if(level.getBlockEntity(pos(cell)) instanceof CompositeCellEntity entity&&!entity.valid())continue;
            if(level.getBlockEntity(pos(cell)) instanceof CompositeCellEntity entity&&entity.view().replicas().stream().anyMatch(p->!knownIds.contains(p.pieceId())))continue;
            boolean expectedNative=snapshot().at(cell).stream().map(snapshot().records()::get).anyMatch(p->p.orientation()==0&&p.origin16().equals(new GridPoint(cell.x()*16,cell.y()*16,cell.z()*16))&&!p.flags().contains(PieceRecord.Flag.SUBSTITUTE)&&NativeStates.original(p).equals(state));
            if(state.isAir()||state.getBlock()==NmgContent.CELL.get()||expectedNative)syncCell(cell,snapshot());
            else if(!snapshot().at(cell).isEmpty())throw new IOException("Native block conflicts with committed NMG authority at "+cell);
        }
    }
    public void repairChunk(net.minecraft.world.level.chunk.LevelChunk chunk)throws IOException{
        var chunkPos=chunk.getPos();Set<BlockPos> cells=new HashSet<>();
        chunk.getBlockEntities().forEach((pos,entity)->{if(entity instanceof CompositeCellEntity)cells.add(pos);});
        for(int y=level.getMinSectionY();y<level.getMaxSectionY();y++)for(UUID id:snapshot().referencedBy(new SectionPos(chunkPos.x,y,chunkPos.z)))
            for(var cell:snapshot().records().get(id).worldOccupation().cells(8))if(Math.floorDiv(cell.x(),16)==chunkPos.x&&Math.floorDiv(cell.z(),16)==chunkPos.z)cells.add(pos(cell));
        // repairLoadedCells also covers cells whose replicas disappeared before a chunk save.
        repairLoadedCells(cells);
    }
    @Override public boolean alreadyApplied(UUID id)throws IOException{
        OperationPlan plan=history.get(id);Long sequence=sequences.get(id);if(plan==null||sequence==null||!receipt(plan,sequence))return false;
        for(var c:plan.changes())for(var p:List.of(c.before(),c.after()))if(p.isPresent()&&store.read(p.orElseThrow().ownerSection()).checkpoint()<sequence)return false;return true;
    }
    @Override public void afterCommit(OperationPlan plan){NeoForge.EVENT_BUS.post(new net.nicomar.nomoregrids.event.AfterOperationEvent(level,plan));}
    @Override public void close()throws IOException{journal.close();}
}
