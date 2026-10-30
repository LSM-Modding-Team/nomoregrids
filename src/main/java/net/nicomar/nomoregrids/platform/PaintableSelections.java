package net.nicomar.nomoregrids.platform;

import java.io.IOException;
import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import net.nicomar.nomoregrids.compat.paintableblocks.PaintableBridge;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.Box16;
import net.nicomar.nomoregrids.network.PaintableSelectionPayload;

/** Per-player native tool selections with pinned revisions, held-tool image and one atomic batch commit. */
public final class PaintableSelections {
    private static final Map<UUID,Selection> SELECTIONS=new HashMap<>();
    private static final class Selection {
        final WorldService service;final InteractionHand hand;final ItemStack tool;final long expires;final UUID anchor;final long revision;final Object surface;final boolean area,slope;final Box16 first;
        Box16 region;Map<UUID,PieceRecord> pinned;final PieceRecord anchorImage;final GridPoint creationAnchor;final UUID session=UUID.randomUUID();boolean create,upper;int direction,resolution=4;Box16 transformRegion;
        Selection(WorldService service,ServerPlayer p,InteractionHand hand,PieceRecord anchor,Object surface,boolean area,boolean slope,Box16 first){this.service=service;this.hand=hand;tool=p.getItemInHand(hand).copy();expires=p.level().getGameTime()+1200;this.anchor=anchor.pieceId();revision=anchor.revision();anchorImage=anchor;creationAnchor=first.min().add(new GridPoint(0,16,0));this.surface=surface;this.area=area;this.slope=slope;this.first=first;region=first;}
    }
    private PaintableSelections(){}
    public static void clear(UUID player){SELECTIONS.remove(player);}
    private static void send(ServerPlayer p,Selection s){PacketDistributor.sendToPlayer(p,new PaintableSelectionPayload(p.level().dimension().identifier().toString(),s==null?0:s.pinned==null?1:s.slope?3:2,s==null?GridPoint.ZERO:s.region.min(),s==null?GridPoint.ZERO:s.region.max(),s==null?new UUID(0,0):s.session,s!=null&&s.slope,s!=null&&s.create,s==null?0:s.direction,s!=null&&s.upper,s==null?4:s.resolution));}
    private static Box16 union(Box16 a,Box16 b){return Box16.of(Math.min(a.min().x(),b.min().x()),Math.min(a.min().y(),b.min().y()),Math.min(a.min().z(),b.min().z()),Math.max(a.max().x(),b.max().x()),Math.max(a.max().y(),b.max().y()),Math.max(a.max().z(),b.max().z()));}
    public static void use(WorldService service,ServerPlayer p,InteractionHand hand,PieceRecord piece,BlockHitResult hit,UUID operation,boolean control)throws IOException{
        var bridge=PaintableBridge.available().orElseThrow();var held=p.getItemInHand(hand);var s=SELECTIONS.get(p.getUUID());
        if(s!=null&&(s.service!=service||s.hand!=hand||p.level().getGameTime()>s.expires||!ItemStack.matches(s.tool,held))){SELECTIONS.remove(p.getUUID());send(p,null);throw new IllegalArgumentException("STALE_REVISION: PB selection changed");}
        if(control&&p.isShiftKeyDown()){SELECTIONS.remove(p.getUUID());send(p,null);return;}
        if(bridge.brush(held)){
            if(control){
                if(s!=null){SELECTIONS.remove(p.getUUID());send(p,null);return;}
                s=new Selection(service,p,hand,piece,bridge.surface(piece,hit),true,false,bridge.selection(piece,hit,false));SELECTIONS.put(p.getUUID(),s);send(p,s);p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Primera superficie seleccionada. Usa el pincel en la segunda para pintar el área."));return;
            }
            Object first=null;if(s!=null){if(!s.area||!s.anchor.equals(piece.pieceId())||s.revision!=piece.revision())throw new IllegalArgumentException("STALE_REVISION: PB area");first=s.surface;}
            var slots=NativeInventory.copy(p);var edit=bridge.editArea(p,hand,piece,hit,slots,p.isShiftKeyDown(),first);service.commitPaintable(p,hand,Map.of(piece.pieceId(),edit),service.snapshot().records().containsKey(piece.pieceId())?Map.of():Map.of(piece.pieceId(),piece),slots,operation);
            SELECTIONS.remove(p.getUUID());send(p,null);return;
        }
        if(!bridge.volumeTool(held)||s==null&&!control&&!bridge.former(held)){
            var slots=NativeInventory.copy(p);var edit=bridge.edit(p,hand,piece,hit,slots,p.isShiftKeyDown());service.commitPaintable(p,hand,Map.of(piece.pieceId(),edit),service.snapshot().records().containsKey(piece.pieceId())?Map.of():Map.of(piece.pieceId(),piece),slots,operation);return;
        }
        if(s==null){s=new Selection(service,p,hand,piece,null,false,bridge.former(held),bridge.selection(piece,hit,bridge.former(held)));SELECTIONS.put(p.getUUID(),s);send(p,s);p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Primera esquina seleccionada. Selecciona la segunda con Ctrl + usar."));return;}
        var anchor=service.snapshot().records().get(s.anchor);if(anchor==null&&!bridge.matchesOriginal(s.anchorImage,p.level(),WorldService.pos(s.anchorImage.origin16().cell()))||anchor!=null&&anchor.revision()!=s.revision)throw new IllegalArgumentException("STALE_REVISION: PB anchor");
        if(s.area)throw new IllegalArgumentException("STALE_REVISION: tool selection");
        if(s.pinned==null||control){
            s.region=union(s.first,bridge.selection(piece,hit,s.slope));var size=s.region.max().subtract(s.region.min());if(size.x()>256||size.y()>256||size.z()>256)throw new IllegalArgumentException("COMPLEXITY_LIMIT: PB volume is at most 16 blocks per axis");s.transformRegion=s.region;s.direction=switch(p.getDirection()){case EAST->0;case SOUTH->1;case WEST->2;default->3;};s.pinned=targets(service,s.region);send(p,s);p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Volumen seleccionado. Usa para confirmar; mira la dirección de la pendiente y mantén Mayús para invertirla. Ctrl + Mayús + usar cancela."));return;
        }
        if(!s.pinned.equals(targets(service,s.region)))throw new IllegalArgumentException("STALE_REVISION: PB volume changed");
        var slots=NativeInventory.copy(p);var edits=new LinkedHashMap<UUID,PaintableBridge.Edit>();Direction direction=p.getDirection();
        for(var id:new TreeSet<>(s.pinned.keySet())){
            var target=s.pinned.get(id);if(s.slope){var b=target.worldOccupation().bounds();if(b.min().x()<s.region.min().x()||b.min().y()<s.region.min().y()||b.min().z()<s.region.min().z()||b.max().x()>s.region.max().x()||b.max().y()>s.region.max().y()||b.max().z()>s.region.max().z())throw new IllegalArgumentException("COMPLEXITY_LIMIT: slope selection must contain each whole piece");}
            var edit=bridge.volume(target,s.region,direction,p.isShiftKeyDown(),s.slope,slots);if(edit.after().equals(Optional.of(target)))continue;edits.put(id,edit);
            if(s.slope&&!p.isCreative()){int index=hand==InteractionHand.MAIN_HAND?p.getInventory().getSelectedSlot():net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND;var tool=slots.get(index);if(tool.isEmpty())throw new IllegalArgumentException("INVENTORY_MISSING: slope tool durability");tool.setDamageValue(tool.getDamageValue()+1);if(tool.getDamageValue()>=tool.getMaxDamage())slots.set(index,ItemStack.EMPTY);}
        }
        if(!edits.isEmpty())service.commitPaintable(p,hand,edits,captures(service,s.pinned),slots,operation);SELECTIONS.remove(p.getUUID());send(p,null);
    }
    public static void configure(WorldService service,ServerPlayer p,net.nicomar.nomoregrids.network.PaintableConfigPayload config)throws IOException{
        var s=SELECTIONS.get(p.getUUID());if(s==null||s.service!=service||!s.session.equals(config.session())||!s.slope||s.pinned==null||p.level().getGameTime()>s.expires||!ItemStack.matches(s.tool,p.getItemInHand(s.hand)))throw new IllegalArgumentException("STALE_REVISION: PB volume session");
        if(config.action()==2){SELECTIONS.remove(p.getUUID());send(p,null);return;}if(config.create()&&!p.isCreative())throw new IllegalArgumentException("PROTECTED");
        var bridge=PaintableBridge.available().orElseThrow();s.create=config.create();s.upper=config.upper();s.direction=config.direction();s.resolution=config.resolution();s.region=s.create?Box16.of(s.creationAnchor.x(),s.creationAnchor.y(),s.creationAnchor.z(),s.creationAnchor.x()+config.width()*16L,s.creationAnchor.y()+config.height()*16L,s.creationAnchor.z()+config.depth()*16L):s.transformRegion;
        var current=s.create?Map.<UUID,PieceRecord>of():targets(service,s.region);if(!s.create&&!s.pinned.equals(current))throw new IllegalArgumentException("STALE_REVISION: PB selection changed");
        if(config.action()==0){send(p,s);return;}var slots=NativeInventory.copy(p);var edits=new LinkedHashMap<UUID,PaintableBridge.Edit>();var originals=new LinkedHashMap<UUID,PieceRecord>(current);var direction=new Direction[]{Direction.EAST,Direction.SOUTH,Direction.WEST,Direction.NORTH}[s.direction];
        if(s.create){if((long)config.width()*config.height()*config.depth()>128)throw new IllegalArgumentException("COMPLEXITY_LIMIT: PB creation batch");
            for(int x=0;x<config.width();x++)for(int y=0;y<config.height();y++)for(int z=0;z<config.depth();z++){
                var q=s.creationAnchor.add(new GridPoint(x*16L,y*16L,z*16L));var at=WorldService.pos(q.cell());if(!p.level().isLoaded(at)||!p.level().getBlockState(at).isAir())throw new IllegalArgumentException("OVERLAP or UNLOADED_REGION: PB creation");var made=bridge.create(p.level(),at,s.resolution);made=new PieceRecord(made.pieceId(),0,made.adapterId(),made.adapterVersion(),made.sourceBlockId(),made.sourceState(),q,made.orientation(),made.pivot16(),made.geometry(),made.geometryPayload(),made.materialPayload(),made.itemPayload(),made.groupId(),made.flags());originals.put(made.pieceId(),made);
            }
        }
        for(var target:originals.values()){
            var edit=bridge.volume(target,s.region,direction,s.upper,true,slots);if(edit.after().equals(Optional.of(target))||s.create&&edit.after().isEmpty())continue;edits.put(target.pieceId(),edit);
            if(!p.isCreative()){int slot=s.hand==InteractionHand.MAIN_HAND?p.getInventory().getSelectedSlot():net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND;var tool=slots.get(slot);if(tool.isEmpty())throw new IllegalArgumentException("INVENTORY_MISSING: former durability");tool.setDamageValue(tool.getDamageValue()+1);if(tool.getDamageValue()>=tool.getMaxDamage())slots.set(slot,ItemStack.EMPTY);}
        }
        if(!edits.isEmpty())service.commitPaintable(p,s.hand,edits,captures(service,originals),slots,UUID.randomUUID());SELECTIONS.remove(p.getUUID());send(p,null);
    }
    private static Map<UUID,PieceRecord> captures(WorldService service,Map<UUID,PieceRecord> records){var captures=new HashMap<UUID,PieceRecord>();records.forEach((id,p)->{if(!service.snapshot().records().containsKey(id))captures.put(id,p);});return Map.copyOf(captures);}
    private static Map<UUID,PieceRecord> targets(WorldService service,Box16 region){
        var result=new TreeMap<UUID,PieceRecord>();long primitives=0;var ids=new HashSet<UUID>();var bridge=PaintableBridge.available().orElseThrow();var level=service.level();for(var cell:region.cells(8192)){var at=WorldService.pos(cell);if(!level.isLoaded(at))throw new IllegalArgumentException("UNLOADED_REGION");ids.addAll(service.snapshot().at(cell));if(bridge.nativeBlock(level.getBlockState(at))){var p=bridge.nativeSelection(level,at);result.put(p.pieceId(),p);}}
        for(var id:ids){var p=service.snapshot().records().get(id);if(p.adapterId().value().equals("nomoregrids:paintableblocks")&&p.worldOccupation().bounds().overlaps(region))result.put(id,p);}
        for(var p:result.values()){if(!service.available(p))throw new IllegalArgumentException("UNSUPPORTED_ADAPTER");primitives+=service.adapter(p).occupation(p).size();if(result.size()>128||primitives>32768)throw new IllegalArgumentException("COMPLEXITY_LIMIT: PB selection geometry");}return Map.copyOf(result);
    }
}
