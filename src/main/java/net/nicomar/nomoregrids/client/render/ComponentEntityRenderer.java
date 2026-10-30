package net.nicomar.nomoregrids.client.render;

import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.Rotation;
import net.nicomar.nomoregrids.platform.WorldDecorations;
import net.nicomar.nomoregrids.world.*;
import org.joml.Matrix4f;

/** Native decorative render data are extracted once per visible logical piece, then transformed without temporary world installation. */
public final class ComponentEntityRenderer implements BlockEntityRenderer<CompositeCellEntity,ComponentEntityRenderer.State> {
    private record Part(PieceRecord piece,BlockEntityRenderState state){}
    public static final class State extends BlockEntityRenderState{final List<Part> parts=new ArrayList<>();}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(CompositeCellEntity cell,State state,float partial,Vec3 camera,net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay overlay){
        BlockEntityRenderer.super.extractRenderState(cell,state,partial,camera,overlay);state.parts.clear();if(!cell.valid()||cell.getLevel()==null)return;var dispatcher=Minecraft.getInstance().getBlockEntityRenderDispatcher();for(var p:cell.view().replicas())if(WorldDecorations.piece(p)&&!p.flags().contains(PieceRecord.Flag.SUBSTITUTE)){
            var owner=p.worldOccupation().cells(8).stream().map(c->new BlockPos(Math.toIntExact(c.x()),Math.toIntExact(c.y()),Math.toIntExact(c.z()))).filter(cell.getLevel()::isLoaded).min(Comparator.comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ));if(owner.isEmpty()||!owner.orElseThrow().equals(cell.getBlockPos()))continue;
            var nativeBE=WorldDecorations.detached(p,cell.getLevel(),true);var extracted=dispatcher.tryExtractRenderState(nativeBE,partial,overlay,false);if(extracted!=null)state.parts.add(new Part(p,extracted));
        }
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        var dispatcher=Minecraft.getInstance().getBlockEntityRenderDispatcher();for(var part:state.parts){var p=part.piece;pose.pushPose();pose.translate(p.origin16().x()/16.0-state.blockPos.getX(),p.origin16().y()/16.0-state.blockPos.getY(),p.origin16().z()/16.0-state.blockPos.getZ());pose.translate(p.pivot16().x()/16.0,p.pivot16().y()/16.0,p.pivot16().z()/16.0);var r=Rotation.of(p.orientation());var x=r.apply(new GridPoint(1,0,0));var y=r.apply(new GridPoint(0,1,0));var z=r.apply(new GridPoint(0,0,1));pose.mulPose(new Matrix4f().m00(x.x()).m01(x.y()).m02(x.z()).m10(y.x()).m11(y.y()).m12(y.z()).m20(z.x()).m21(z.y()).m22(z.z()));pose.translate(-p.pivot16().x()/16.0,-p.pivot16().y()/16.0,-p.pivot16().z()/16.0);dispatcher.submit(part.state,pose,collector,camera);pose.popPose();}
    }
}
