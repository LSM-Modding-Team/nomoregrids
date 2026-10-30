package net.nicomar.nomoregrids.platform;

import java.io.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.BoxMesh;
import net.nicomar.nomoregrids.geometry.Rotation;
import net.nicomar.nomoregrids.operation.OperationPlan;
import net.nicomar.nomoregrids.registry.NmgContent;
import net.nicomar.nomoregrids.simulation.functional.FunctionalStore;

/** NeoForge 26.2 resource API. One shared journal per dimension preserves nested transaction rollback and identity. */
public final class FunctionalTransfers extends SnapshotJournal<Map<UUID,FunctionalTransfers.Pending>> {
    record Pending(BinaryData before,FunctionalStore.State after){}
    private static final ThreadLocal<FunctionalTransfers> ACTIVE=new ThreadLocal<>();
    private static final Map<ResourceId,Long> ENERGY=new HashMap<>();
    private final ServerLevel level;private final WorldService service;private Map<UUID,Pending> pending=new HashMap<>();private OperationPlan intent;
    public FunctionalTransfers(ServerLevel level,WorldService service){this.level=level;this.service=service;}
    /** Explicit energy profiles only; furnaces/barrels do not invent an energy store. */
    public static synchronized void energyProfile(ResourceId block,long capacity){if(capacity<1||ENERGY.putIfAbsent(block,capacity)!=null)throw new IllegalArgumentException("Energy profile capacity/duplicate");}
    public static void register(RegisterCapabilitiesEvent event){
        event.registerBlockEntity(Capabilities.Item.BLOCK,NmgContent.CELL_ENTITY.get(),(cell,side)->cell.getLevel() instanceof ServerLevel level&&NmgRuntime.existing(level)!=null?NmgRuntime.existing(level).transfers().items(cell.getBlockPos(),side):null);
        event.registerBlockEntity(Capabilities.Energy.BLOCK,NmgContent.CELL_ENTITY.get(),(cell,side)->cell.getLevel() instanceof ServerLevel level&&NmgRuntime.existing(level)!=null?NmgRuntime.existing(level).transfers().energy(cell.getBlockPos(),side):null);
    }
    @Override protected Map<UUID,Pending> createSnapshot(){return Map.copyOf(pending);}
    @Override protected void revertToSnapshot(Map<UUID,Pending> image){pending=new HashMap<>(image);if(pending.isEmpty()&&ACTIVE.get()==this)ACTIVE.remove();persistIntent();}
    @Override protected void onRootCommit(Map<UUID,Pending> original){
        var committed=intent;pending=new HashMap<>();ACTIVE.remove();if(committed==null)return;
        try{service.executePrepared(committed);service.clearTransferIntent();intent=null;}
        catch(IOException|RuntimeException ex){NmgRuntime.failed(level,ex);throw new IllegalStateException("Committed external resource transfer requires recovery; private intent retained",ex);}
    }
    private void persistIntent(){
        try{if(pending.isEmpty()){service.clearTransferIntent();intent=null;return;}var effects=new ArrayList<OperationPlan.Effect>();for(var e:pending.entrySet()){var p=service.snapshot().records().get(e.getKey());if(p==null)throw new IllegalStateException("Removed functional transfer target");var value=service.effects().get("machine:"+e.getKey());effects.add(new OperationPlan.Effect("machine:"+e.getKey(),value.owner(),Optional.of(e.getValue().before),Optional.of(service.functionals().encode(p,e.getValue().after))));}
            var plan=new OperationPlan(UUID.randomUUID(),OperationPlan.WORLD_ACTOR,OperationPlan.Kind.SYSTEM,List.of(),BinaryData.EMPTY,BinaryData.EMPTY,effects);service.stageTransferIntent(plan);intent=plan;
        }catch(IOException|RuntimeException ex){NmgRuntime.failed(level,ex);throw new IllegalStateException("Cannot persist external transfer intent",ex);}
    }

    private PieceRecord route(BlockPos pos,Direction side){
        if(service.quarantined()||!level.isLoaded(pos))return null;var c=new CellPos(pos.getX(),pos.getY(),pos.getZ());var matches=new ArrayList<PieceRecord>();
        for(var id:service.snapshot().at(c)){var p=service.snapshot().records().get(id);if(!WorldFunctionals.piece(p)||!service.available(p))continue;
            if(side==null){if(p.worldOccupation().cells(8).size()==1)matches.add(p);continue;}
            var normal=Direction6.valueOf(side.name());long x=c.x()*16+(normal.x+1)*8,y=c.y()*16+(normal.y+1)*8,z=c.z()*16+(normal.z+1)*8;long plane=normal.axis()==0?x:normal.axis()==1?y:z,u=normal.axis()==0?z:x,v=normal.axis()==1?z:y;
            if(BoxMesh.extract(p.worldOccupation(),4096).stream().anyMatch(f->f.normal()==normal&&f.plane()==plane&&u>=f.rect().u0()&&u<=f.rect().u1()&&v>=f.rect().v0()&&v<=f.rect().v1()))matches.add(p);
        }return matches.size()==1?matches.getFirst():null;
    }
    private FunctionalStore.State current(PieceRecord p){var value=pending.get(p.pieceId());return value==null?service.functionals().state(p):value.after;}
    private boolean begin(PieceRecord p,TransactionContext transaction){
        service.assertServerThread();if(ACTIVE.get()!=null&&ACTIVE.get()!=this)return false;if(!pending.containsKey(p.pieceId())&&pending.isEmpty())try{service.functionals().synchronize(p);}catch(IOException ex){throw new UncheckedIOException(ex);}
        updateSnapshots(transaction);ACTIVE.set(this);return true;
    }
    private void changed(PieceRecord p,FunctionalStore.State next){var old=pending.get(p.pieceId());BinaryData before=old==null?service.effects().get("machine:"+p.pieceId()).data():old.before;long revision=old==null?service.functionals().state(p).revision()+1:old.after.revision();pending.put(p.pieceId(),new Pending(before,new FunctionalStore.State(p.pieceId(),revision,next.slots(),next.energy(),next.machine())));persistIntent();}
    public ResourceHandler<ItemResource> items(BlockPos pos,Direction side){var p=route(pos,side);return p==null?null:new ItemPort(p.pieceId(),pos.immutable(),side);}
    public EnergyHandler energy(BlockPos pos,Direction side){var p=route(pos,side);if(p==null||!ENERGY.containsKey(p.sourceBlockId()))return null;return new EnergyPort(p.pieceId(),pos.immutable(),side);}
    private final class ItemPort implements ResourceHandler<ItemResource>{
        final UUID id;final BlockPos pos;final Direction side;ItemPort(UUID id,BlockPos pos,Direction side){this.id=id;this.pos=pos;this.side=side;}
        PieceRecord valid(){var p=route(pos,side);return p!=null&&p.pieceId().equals(id)?p:null;}
        Direction local(PieceRecord p){return side==null?null:Direction.valueOf(Direction6.valueOf(side.name()).rotated(Rotation.of(p.orientation()).inverse().id()).name());}
        int[] slots(PieceRecord p){var be=service.functionals().detached(p,current(p));if(side!=null&&be.entity() instanceof WorldlyContainer sided)return sided.getSlotsForFace(local(p));int[] slots=new int[be.getContainerSize()];for(int i=0;i<slots.length;i++)slots[i]=i;return slots;}
        int slot(PieceRecord p,int index){var slots=slots(p);if(index<0||index>=slots.length)throw new IndexOutOfBoundsException(index);return slots[index];}
        @Override public int size(){var p=valid();return p==null?0:slots(p).length;}
        @Override public ItemResource getResource(int index){var p=valid();return p==null?ItemResource.EMPTY:ItemResource.of(service.functionals().detached(p,current(p)).getItem(slot(p,index)));}
        @Override public long getAmountAsLong(int index){var p=valid();return p==null?0:service.functionals().detached(p,current(p)).getItem(slot(p,index)).getCount();}
        @Override public long getCapacityAsLong(int index,ItemResource resource){return resource.isEmpty()?64:isValid(index,resource)?resource.getMaxStackSize():0;}
        @Override public boolean isValid(int index,ItemResource resource){var p=valid();if(p==null||resource.isEmpty())return false;var be=service.functionals().detached(p,current(p));int slot=slot(p,index);var stack=resource.toStack();return be.canPlaceItem(slot,stack)&&(side==null||!(be.entity() instanceof WorldlyContainer sided)||sided.canPlaceItemThroughFace(slot,stack,local(p)));}
        @Override public int insert(int index,ItemResource resource,int amount,TransactionContext transaction){if(amount<0||resource.isEmpty())throw new IllegalArgumentException("Resource amount");var p=valid();if(p==null||amount==0||!isValid(index,resource)||!begin(p,transaction))return 0;var be=service.functionals().detached(p,current(p));int slot=slot(p,index);var old=be.getItem(slot);if(!old.isEmpty()&&!resource.matches(old))return 0;int n=Math.min(amount,Math.min(resource.getMaxStackSize(),be.getMaxStackSize())-old.getCount());if(n<=0)return 0;be.setItem(slot,resource.toStack(old.getCount()+n));var captured=service.functionals().capture(p,be,current(p).revision());changed(p,new FunctionalStore.State(p.pieceId(),captured.revision(),captured.slots(),current(p).energy(),captured.machine()));return n;}
        @Override public int extract(int index,ItemResource resource,int amount,TransactionContext transaction){if(amount<0||resource.isEmpty())throw new IllegalArgumentException("Resource amount");var p=valid();if(p==null||amount==0||!begin(p,transaction))return 0;var be=service.functionals().detached(p,current(p));int slot=slot(p,index);var old=be.getItem(slot);if(!resource.matches(old)||side!=null&&be.entity() instanceof WorldlyContainer sided&&!sided.canTakeItemThroughFace(slot,old,local(p)))return 0;int n=Math.min(amount,old.getCount());if(n<=0)return 0;be.removeItem(slot,n);var captured=service.functionals().capture(p,be,current(p).revision());changed(p,new FunctionalStore.State(p.pieceId(),captured.revision(),captured.slots(),current(p).energy(),captured.machine()));return n;}
    }
    private final class EnergyPort implements EnergyHandler{
        final UUID id;final BlockPos pos;final Direction side;EnergyPort(UUID id,BlockPos pos,Direction side){this.id=id;this.pos=pos;this.side=side;}
        PieceRecord valid(){var p=route(pos,side);return p!=null&&p.pieceId().equals(id)?p:null;}
        @Override public long getAmountAsLong(){var p=valid();return p==null?0:current(p).energy();}
        @Override public long getCapacityAsLong(){var p=valid();return p==null?0:ENERGY.getOrDefault(p.sourceBlockId(),0L);}
        @Override public int insert(int amount,TransactionContext tx){if(amount<0)throw new IllegalArgumentException("Energy amount");var p=valid();if(p==null||!begin(p,tx))return 0;var old=current(p);int n=(int)Math.max(0,Math.min(amount,getCapacityAsLong()-old.energy()));if(n>0)changed(p,new FunctionalStore.State(id,old.revision(),old.slots(),old.energy()+n,old.machine()));return n;}
        @Override public int extract(int amount,TransactionContext tx){if(amount<0)throw new IllegalArgumentException("Energy amount");var p=valid();if(p==null||!begin(p,tx))return 0;var old=current(p);int n=(int)Math.min(amount,old.energy());if(n>0)changed(p,new FunctionalStore.State(id,old.revision(),old.slots(),old.energy()-n,old.machine()));return n;}
    }
}
