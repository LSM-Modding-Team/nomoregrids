package net.nicomar.nomoregrids.platform;

import java.io.IOException;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.*;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.*;
import net.nicomar.nomoregrids.geometry.Rotation;
import net.nicomar.nomoregrids.operation.OperationPlan;
import net.nicomar.nomoregrids.simulation.movement.*;

/** Native piston trigger, transactional piece motion and native base/head images. No vanilla CELL push. */
public final class WorldPistons {
    private WorldPistons(){}
    public static void register(){NeoForge.EVENT_BUS.addListener(WorldPistons::pre);}
    private static CellPos cell(BlockPos p){return new CellPos(p.getX(),p.getY(),p.getZ());}
    private static void pre(PistonEvent.Pre event){
        if(!(event.getLevel() instanceof ServerLevel level))return;var service=NmgRuntime.existing(level);if(service==null||service.writing())return;boolean extending=event.getPistonMoveType().isExtend;var direction=event.getDirection();var face=event.getFaceOffsetPos();var contacted=extending?face:face.relative(direction);
        if(service.snapshot().at(cell(contacted)).isEmpty())return;event.setCanceled(true);if(service.quarantined())return;
        try{move(level,service,event.getPos(),direction,extending);NeoForge.EVENT_BUS.post(new PistonEvent.Post(level,event.getPos(),direction,event.getPistonMoveType()));}
        catch(IOException|RuntimeException ex){if(service.quarantined())NmgRuntime.failed(level,ex);else net.nicomar.nomoregrids.NoMoreGrids.LOGGER.debug("NMG piston blocked: {}",ex.toString());}
    }
    private static void move(ServerLevel level,WorldService service,BlockPos base,net.minecraft.core.Direction facing,boolean extending)throws IOException{
        var state=level.getBlockState(base);if(!(state.getBlock() instanceof PistonBaseBlock))throw new IllegalArgumentException("UNSUPPORTED_ADAPTER");boolean sticky=state.is(Blocks.STICKY_PISTON);var head=base.relative(facing);var contacts=new TreeSet<UUID>();if(extending||sticky)contacts.addAll(service.snapshot().at(cell(extending?head:head.relative(facing))));
        var movement=Direction6.valueOf((extending?facing:facing.getOpposite()).name());
        var changes=PistonPlanner.plan(service.snapshot(),contacts,movement,!extending,new GroupRegistry(),id->{var p=service.snapshot().records().get(id);return service.adapter(p).capabilities().contains(net.nicomar.nomoregrids.api.adapter.PieceAdapter.Capability.MOVE)||p.adapterId().value().equals("nomoregrids:box");},c->level.isLoaded(WorldService.pos(c))&&!level.isOutsideBuildHeight(WorldService.pos(c))&&level.getWorldBorder().isWithinBounds(WorldService.pos(c)),box->{
            var bounds=new AABB(box.min().x()/16.0,box.min().y()/16.0,box.min().z()/16.0,box.max().x()/16.0,box.max().y()/16.0,box.max().z()/16.0);
            if(!level.getEntityCollisions(null,bounds).isEmpty())return true;
            for(var c:box.cells(32)){var at=WorldService.pos(c);if(!extending&&at.equals(head))continue;if(!service.snapshot().at(c).isEmpty())continue;var nativeState=level.getBlockState(at);if(nativeState.isAir()||nativeState.getBlock() instanceof LiquidBlock)continue;for(var b:NativeStates.boxes(nativeState.getCollisionShape(level,at)).boxes()){var world=b.transform(Rotation.of(0),GridPoint.ZERO,new GridPoint(c.x()*16,c.y()*16,c.z()*16));if(box.overlaps(world))return true;}}return false;
        },(a,b)->{String x=a.sourceBlockId().value(),y=b.sourceBlockId().value();if(x.equals("minecraft:slime_block")&&y.equals("minecraft:honey_block")||x.equals("minecraft:honey_block")&&y.equals("minecraft:slime_block"))return false;return x.equals("minecraft:slime_block")||y.equals("minecraft:slime_block")||x.equals("minecraft:honey_block")||y.equals("minecraft:honey_block");});
        var planned=new ArrayList<OperationPlan.Change>();for(var c:changes)planned.add(new OperationPlan.Change(c.pieceId(),Optional.of(service.snapshot().records().get(c.pieceId())),c.replacement()));
        var auxiliary=new ArrayList<OperationPlan.Effect>();auxiliary.add(service.nativeChange(cell(base),state.setValue(PistonBaseBlock.EXTENDED,extending)));
        var headState=extending?Blocks.PISTON_HEAD.defaultBlockState().setValue(PistonHeadBlock.FACING,facing).setValue(PistonHeadBlock.TYPE,sticky?PistonType.STICKY:PistonType.DEFAULT):Blocks.AIR.defaultBlockState();auxiliary.add(service.nativeChange(cell(head),headState));
        service.executeWorld(planned,auxiliary);
    }
}
