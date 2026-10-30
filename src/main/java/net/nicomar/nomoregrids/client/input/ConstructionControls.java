package net.nicomar.nomoregrids.client.input;

import java.util.*;
import net.minecraft.client.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.*;
import net.minecraft.gizmos.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.network.ModePayload;
import net.nicomar.nomoregrids.network.ActionPayload;
import net.nicomar.nomoregrids.network.ActionResultPayload;
import net.nicomar.nomoregrids.compat.vanilla.VanillaCatalog;
import net.nicomar.nomoregrids.platform.ConstructionCandidate;
import org.lwjgl.glfw.GLFW;

/** Rebindable controls and 26.2 Gizmo preview, depth tested and never visible through walls. */
public final class ConstructionControls {
    private static final KeyMapping.Category CATEGORY=new KeyMapping.Category(Identifier.fromNamespaceAndPath("nomoregrids","controls"));
    private static final KeyMapping PRECISE=new KeyMapping("key.nomoregrids.toggle_precision",GLFW.GLFW_KEY_P,CATEGORY),EXPANDED=new KeyMapping("key.nomoregrids.toggle_orientation",GLFW.GLFW_KEY_O,CATEGORY),
        ADJUSTED=new KeyMapping("key.nomoregrids.toggle_resize",GLFW.GLFW_KEY_J,CATEGORY),ALIGN=new KeyMapping("key.nomoregrids.lock_alignment",GLFW.GLFW_KEY_K,CATEGORY),
        LATERAL=new KeyMapping("key.nomoregrids.side_orientation",GLFW.GLFW_KEY_LEFT_ALT,CATEGORY),REPLACE=new KeyMapping("key.nomoregrids.replace",GLFW.GLFW_KEY_R,CATEGORY),LOCK=new KeyMapping("key.nomoregrids.lock_connections",GLFW.GLFW_KEY_L,CATEGORY);
    private static final PlayerModes MODES=new PlayerModes();
    private static final VanillaCatalog CATALOG=new VanillaCatalog();
    private static ModePayload lastSent;
    private static Object lastDimension;
    private static PieceRecord preview;
    private static int previewColor=0xffffbf00;
    private static UUID probe;
    private static PieceRecord probedPreview;
    private static int ticks;
    private static int alignmentAxis=1,alignmentSign=1,alignmentRotation;
    private static long alignmentPlane;
    private static int lengthSelection;
    private static Map<String,String> alignmentProperties=Map.of();
    private ConstructionControls(){}
    public static void register(IEventBus modBus){modBus.addListener(ConstructionControls::keys);modBus.addListener(ConstructionControls::payloads);NeoForge.EVENT_BUS.addListener(ConstructionControls::tick);NeoForge.EVENT_BUS.addListener(ConstructionControls::scroll);NeoForge.EVENT_BUS.addListener(ConstructionControls::extract);NeoForge.EVENT_BUS.addListener(ConstructionControls::interact);NeoForge.EVENT_BUS.addListener(ConstructionControls::middle);}
    private static void payloads(net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent event){
        event.register(net.nicomar.nomoregrids.network.SessionPayload.TYPE,(packet,context)->context.enqueueWork(()->{
            if(!packet.profileHash().equals(net.nicomar.nomoregrids.network.SessionPayload.localHash())){context.disconnect(net.minecraft.network.chat.Component.translatable("message.nomoregrids.profile_mismatch"));return;}
            MODES.precise=packet.precise();MODES.expanded=packet.expanded();MODES.adjusted=packet.adjusted();MODES.changedDimension();lengthSelection=0;lastSent=null;
            if(Minecraft.getInstance().player!=null)Minecraft.getInstance().player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.nomoregrids.session"),true);
        }));
        event.register(ActionResultPayload.TYPE,(packet,context)->context.enqueueWork(()->{
        if(packet.operation().equals(probe)){if(Objects.equals(preview,probedPreview))previewColor=packet.accepted()?0xff44cc55:0xffff3333;}
    }));}
    private static void interact(InputEvent.InteractionKeyMappingTriggered event){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.gui.screen()!=null)return;
        boolean cell=mc.hitResult instanceof BlockHitResult hit&&mc.level.getBlockEntity(hit.getBlockPos()) instanceof net.nicomar.nomoregrids.world.CompositeCellEntity;
        boolean slab=mc.hitResult instanceof BlockHitResult hit&&mc.level.getBlockState(hit.getBlockPos()).getBlock() instanceof net.minecraft.world.level.block.SlabBlock&&mc.player.getItemInHand(event.getHand()).getItem() instanceof net.minecraft.world.item.BlockItem item&&item.getBlock() instanceof net.minecraft.world.level.block.SlabBlock;
        var held=mc.player.getItemInHand(event.getHand());boolean fragment=held.getItem() instanceof net.nicomar.nomoregrids.material.FragmentItem;
        boolean cuttable=fragment||held.getItem() instanceof net.minecraft.world.item.BlockItem item&&net.nicomar.nomoregrids.material.FragmentItem.cuttable(item.getBlock());
        if(event.isUseItem()&&(MODES.precise||MODES.expanded||LATERAL.isDown()||cell||slab||fragment||MODES.adjusted&&cuttable||MODES.alignment().isPresent())&&(held.getItem() instanceof net.minecraft.world.item.BlockItem||fragment)){
            ClientPacketDistributor.sendToServer(packet(),new ActionPayload(UUID.randomUUID(),ActionPayload.Action.PLACE,event.getHand()==InteractionHand.OFF_HAND));event.setCanceled(true);event.setSwingHand(false);
        }else if(event.isAttack()&&cell&&mc.player.isCreative()){
            ClientPacketDistributor.sendToServer(new ActionPayload(UUID.randomUUID(),ActionPayload.Action.REMOVE,false));event.setCanceled(true);event.setSwingHand(false);
        }
    }
    private static void keys(RegisterKeyMappingsEvent event){event.registerCategory(CATEGORY);for(var key:List.of(PRECISE,EXPANDED,ADJUSTED,ALIGN,LATERAL,REPLACE,LOCK))event.register(key);}
    private static ModePayload packet(){return new ModePayload(MODES.precise,MODES.expanded,MODES.adjusted,LATERAL.isDown(),REPLACE.isDown(),MODES.axis(),MODES.offset(),Math.floorMod(MODES.candidate(),64),MODES.alignment().isPresent(),alignmentAxis,alignmentSign,alignmentPlane,alignmentRotation,lengthSelection,MODES.alignment().isPresent()?alignmentProperties:Map.of());}
    private static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null){preview=null;lastSent=null;lastDimension=null;return;}
        if(!mc.level.dimension().equals(lastDimension)){MODES.changedDimension();lengthSelection=0;lastDimension=mc.level.dimension();}
        if(mc.gui.screen()!=null){preview=null;var p=packet();var inactive=new ModePayload(p.precise(),p.expanded(),p.adjusted(),false,false,p.axis(),p.offset(),p.candidate(),p.aligned(),p.alignmentAxis(),p.alignmentSign(),p.alignmentPlane(),p.alignmentRotation(),p.length(),p.alignmentProperties());if(!inactive.equals(lastSent)){ClientPacketDistributor.sendToServer(inactive);lastSent=inactive;}return;}
        while(PRECISE.consumeClick())MODES.precise=!MODES.precise;while(EXPANDED.consumeClick())MODES.expanded=!MODES.expanded;while(ADJUSTED.consumeClick())MODES.adjusted=!MODES.adjusted;
        while(ALIGN.consumeClick())if(mc.hitResult instanceof BlockHitResult hit&&(preview!=null||MODES.alignment().isPresent())){
            if(MODES.alignment().isEmpty()&&preview!=null){Direction6 normal=Direction6.valueOf(hit.getDirection().name());alignmentAxis=normal.axis();alignmentSign=normal.sign();alignmentRotation=preview.orientation();
                var properties=new HashMap<String,String>();preview.sourceState().forEach((key,value)->{if(Set.of("axis","facing","half","type").contains(key))properties.put(key,value);});alignmentProperties=Map.copyOf(properties);
                var bounds=preview.worldOccupation().bounds();GridPoint face=normal.sign()>0?bounds.min():bounds.max();alignmentPlane=normal.axis()==0?face.x():normal.axis()==1?face.y():face.z();}
            MODES.align(new GridPoint(Math.round(hit.getLocation().x*16),Math.round(hit.getLocation().y*16),Math.round(hit.getLocation().z*16)));
        }
        while(LOCK.consumeClick())ClientPacketDistributor.sendToServer(new ActionPayload(UUID.randomUUID(),ActionPayload.Action.LOCK,false));
        ModePayload packet=packet();if(!packet.equals(lastSent)){ClientPacketDistributor.sendToServer(packet);lastSent=packet;}
        PieceRecord previous=preview;preview=null;if(!(MODES.precise||MODES.expanded||MODES.adjusted||MODES.alignment().isPresent()||LATERAL.isDown()||REPLACE.isDown()||mc.player.getMainHandItem().getItem() instanceof net.nicomar.nomoregrids.material.FragmentItem||mc.player.getOffhandItem().getItem() instanceof net.nicomar.nomoregrids.material.FragmentItem)||!(mc.hitResult instanceof BlockHitResult hit))return;
        try{
            var main=mc.player.getMainHandItem().getItem();InteractionHand hand=main instanceof net.minecraft.world.item.BlockItem||main instanceof net.nicomar.nomoregrids.material.FragmentItem?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;
            preview=ConstructionCandidate.build(mc.player,hand,hit,packet,CATALOG,new UUID(0,0));boolean invalid=false,missing=false;
            for(var cell:preview.worldOccupation().cells(8)){
                var pos=new net.minecraft.core.BlockPos(Math.toIntExact(cell.x()),Math.toIntExact(cell.y()),Math.toIntExact(cell.z()));
                if(!mc.level.isLoaded(pos)){missing=true;continue;}
                var state=mc.level.getBlockState(pos);if(!state.isAir()&&!(mc.level.getBlockEntity(pos) instanceof net.nicomar.nomoregrids.world.CompositeCellEntity)){
                    var nativePiece=net.nicomar.nomoregrids.platform.NativeStates.capture(state,mc.level,pos,UUID.randomUUID());if(preview.worldOccupation().overlaps(nativePiece.worldOccupation()))invalid=true;
                }
                if(mc.level.getBlockEntity(pos) instanceof net.nicomar.nomoregrids.world.CompositeCellEntity composite)for(var p:composite.view().replicas())if(preview.worldOccupation().overlaps(p.worldOccupation()))invalid=true;
            }
            // Amber is deliberate until server validation covers permissions, inventory, entities and support.
            if(invalid)previewColor=0xffff3333;else if(!Objects.equals(previous,preview))previewColor=0xffffbf00;
            if(++ticks%5==0&&!invalid){probe=UUID.randomUUID();probedPreview=preview;ClientPacketDistributor.sendToServer(new ActionPayload(probe,ActionPayload.Action.PROBE,hand==InteractionHand.OFF_HAND));}
        }catch(RuntimeException ex){preview=null;}
    }
    private static void scroll(InputEvent.MouseScrollingEvent event){
        var mc=Minecraft.getInstance();if(mc.gui.screen()!=null||mc.player==null)return;
        if(ADJUSTED.isDown()){
            int direction=event.getScrollDeltaY()>0?1:event.getScrollDeltaY()<0?-1:0;lengthSelection=Math.floorMod((lengthSelection==0?16:lengthSelection)-1+direction,16)+1;MODES.adjusted=true;
            mc.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.nomoregrids.length",lengthSelection),true);event.setCanceled(true);return;
        }
        if(mc.hasAltDown()||mc.hasControlDown()){
            int steps=event.getScrollDeltaY()>0?1:event.getScrollDeltaY()<0?-1:0;
            try{MODES.scroll(steps,mc.hasAltDown(),mc.hasControlDown(),false);}catch(ArithmeticException ignored){}event.setCanceled(true);
        }
    }
    private static void middle(InputEvent.MouseButton.Pre event){var mc=Minecraft.getInstance();if(mc.player==null||mc.gui.screen()!=null||event.getButton()!=GLFW.GLFW_MOUSE_BUTTON_MIDDLE||event.getAction()!=GLFW.GLFW_PRESS)return;
        if(ADJUSTED.isDown()){lengthSelection=0;MODES.adjusted=true;mc.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.nomoregrids.length_auto"),true);event.setCanceled(true);}
        else if(mc.hasAltDown()){MODES.cycleAxis(false);event.setCanceled(true);}}
    private static void extract(ExtractLevelRenderStateEvent event){
        PieceRecord p=preview;if(p==null)return;
        try(var ignored=event.getLevelExtractor().collectPerFrameMainThreadGizmos()){
            for(var b:p.worldOccupation().boxes())Gizmos.cuboid(new AABB(b.min().x()/16.0,b.min().y()/16.0,b.min().z()/16.0,b.max().x()/16.0,b.max().y()/16.0,b.max().z()/16.0),GizmoStyle.strokeAndFill(previewColor,2f,(previewColor&0xffffff)|0x33000000));
        }
    }
}
