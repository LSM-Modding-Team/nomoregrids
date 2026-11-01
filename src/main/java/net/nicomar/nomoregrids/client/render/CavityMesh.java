package net.nicomar.nomoregrids.client.render;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.quad.*;
import net.nicomar.nomoregrids.core.Direction6;
import net.nicomar.nomoregrids.simulation.fluid.CavityCell;
import net.nicomar.nomoregrids.world.CompositeCellEntity;
import org.joml.Vector3f;

/** Greedy surface rectangles on cavity microcells; native fluid materials and biome tint. */
public final class CavityMesh {
    private CavityMesh(){}
    public static void append(CavityCell cell,BlockAndTintGetter world,BlockPos pos,List<BakedQuad> output){
        for(var kind:List.of(CavityCell.Kind.WATER,CavityCell.Kind.LAVA)){
            if(cell.amount(kind)==0)continue;
            var fluid=kind==CavityCell.Kind.WATER?Fluids.WATER.defaultFluidState():Fluids.LAVA.defaultFluidState();var model=Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid);var source=fluid.createLegacyBlock();int tint=model.tintSource()==null?0xffffffff:0xff000000|model.tintSource().colorInWorld(source,world,pos);
            var heights=net.nicomar.nomoregrids.simulation.fluid.CavitySurface.heights(cell,kind).orElse(null);
            for(var face:Direction6.values()){
                var adjacentPos=pos.relative(Direction.valueOf(face.name()));var adjacent=world.getModelData(adjacentPos).get(CompositeCellEntity.VIEW);
                CavityCell neighbor=adjacent==null?null:adjacent.cavity().orElse(null);
                var nativeState=world.getBlockState(adjacentPos);var nativeFluid=nativeState.getFluidState();
                boolean nativeMatch=neighbor==null&&nativeState.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock&&(kind==CavityCell.Kind.WATER?(nativeFluid.getType()==Fluids.WATER||nativeFluid.getType()==Fluids.FLOWING_WATER):(nativeFluid.getType()==Fluids.LAVA||nativeFluid.getType()==Fluids.FLOWING_LAVA));
                float nativeHeight=nativeMatch?nativeFluid.getHeight(world,adjacentPos):0;
                var material=face==Direction6.UP||face==Direction6.DOWN?model.stillMaterial():model.flowingMaterial();var sprite=material.sprite();var info=BakedQuad.MaterialInfo.of(material,kind==CavityCell.Kind.WATER?com.mojang.blaze3d.platform.Transparency.TRANSLUCENT:com.mojang.blaze3d.platform.Transparency.NONE,-1,true,kind==CavityCell.Kind.LAVA?15:0);boolean[] visible=new boolean[256];
                for(int plane=face.sign()>0?1:0;plane<=(face.sign()>0?16:15);plane++){
                Arrays.fill(visible,false);for(int v=0;v<16;v++)for(int u=0;u<16;u++){
                    int c=plane-(face.sign()>0?1:0);if(c<0||c>=16)continue;int x=face.axis()==0?c:u,y=face.axis()==1?c:v,z=face.axis()==0?u:face.axis()==1?v:c;
                    if(cell.at(x+16*(y+16*z))!=kind)continue;
                    visible[v*16+u]=net.nicomar.nomoregrids.simulation.fluid.CavityBoundary.visible(cell,neighbor,kind,x,y,z,face.x,face.y,face.z);
                    if(nativeMatch&&(face.sign()<0?c==0:c==15)){
                        if(face==Direction6.UP||nativeHeight>=1)visible[v*16+u]=false;
                    }
                    if(heights!=null&&face==Direction6.UP)visible[v*16+u]=false;
                    if(heights!=null&&face.axis()!=1&&c+face.sign()>=0&&c+face.sign()<16){int nx=x+face.x,nz=z+face.z;if(heights[x+16*z]>0&&heights[nx+16*nz]>0)visible[v*16+u]=false;}
                }
                for(int v=0;v<16;v++)for(int u=0;u<16;u++)if(visible[v*16+u]){
                    int width=1;while(u+width<16&&visible[v*16+u+width])width++;int height=1;outer:while(v+height<16){for(int i=0;i<width;i++)if(!visible[(v+height)*16+u+i])break outer;height++;}
                    for(int j=0;j<height;j++)for(int i=0;i<width;i++)visible[(v+j)*16+u+i]=false;
                    var points=new ArrayList<Vec3>(List.of(point(face,plane,u,v),point(face,plane,u+width,v),point(face,plane,u+width,v+height),point(face,plane,u,v+height)));if(face.axis()==2?face.sign()<0:face.sign()>0)Collections.reverse(points);
                    long[] uv=new long[4];for(int i=0;i<4;i++){var p=points.get(i);float a=(float)(face.axis()==0?p.z:p.x),b=(float)(face.axis()==1?p.z:p.y);uv[i]=UVPair.pack(sprite.getU(a),sprite.getV(b));}
                    var normal=Direction.valueOf(face.name());output.add(new BakedQuad(vec(points.get(0)),vec(points.get(1)),vec(points.get(2)),vec(points.get(3)),uv[0],uv[1],uv[2],uv[3],normal,info,BakedNormals.of(BakedNormals.pack(face.x,face.y,face.z)),BakedColors.of(tint,tint,tint,tint)));
                    if(output.size()>131072)throw new IllegalArgumentException("COMPLEXITY_LIMIT: cavity mesh");
                }
            }}
            if(heights!=null)appendSurface(cell,heights,kind,world,pos,model.stillMaterial(),tint,output);
        }
    }
    private static void appendSurface(CavityCell cell,int[] heights,CavityCell.Kind kind,BlockAndTintGetter world,BlockPos pos,net.minecraft.client.resources.model.sprite.Material.Baked material,int tint,List<BakedQuad> output){
        var abovePos=pos.above();var aboveView=world.getModelData(abovePos).get(CompositeCellEntity.VIEW);var above=aboveView==null?null:aboveView.cavity().orElse(null);var aboveState=world.getBlockState(abovePos);var aboveFluid=aboveState.getFluidState();
        boolean nativeAbove=aboveState.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock&&same(kind,aboveFluid);
        var info=BakedQuad.MaterialInfo.of(material,kind==CavityCell.Kind.WATER?com.mojang.blaze3d.platform.Transparency.TRANSLUCENT:com.mojang.blaze3d.platform.Transparency.NONE,-1,true,kind==CavityCell.Kind.LAVA?15:0);
        var neighborHeights=new HashMap<BlockPos,Optional<int[]>>();var sprite=material.sprite();
        for(int z=0;z<16;z++)for(int x=0;x<16;x++){
            int h=heights[x+16*z];if(h==0||h==16&&(nativeAbove||above!=null&&above.at(x+16*16*z)==kind))continue;
            float h00=corner(world,pos,heights,kind,x,z,neighborHeights),h10=corner(world,pos,heights,kind,x+1,z,neighborHeights),h11=corner(world,pos,heights,kind,x+1,z+1,neighborHeights),h01=corner(world,pos,heights,kind,x,z+1,neighborHeights);
            float x0=x/16f,x1=(x+1)/16f,z0=z/16f,z1=(z+1)/16f;
            output.add(new BakedQuad(new Vector3f(x0,h01,z1),new Vector3f(x1,h11,z1),new Vector3f(x1,h10,z0),new Vector3f(x0,h00,z0),UVPair.pack(sprite.getU(x0),sprite.getV(z1)),UVPair.pack(sprite.getU(x1),sprite.getV(z1)),UVPair.pack(sprite.getU(x1),sprite.getV(z0)),UVPair.pack(sprite.getU(x0),sprite.getV(z0)),Direction.UP,info,BakedNormals.of(BakedNormals.pack(0,1,0)),BakedColors.of(tint,tint,tint,tint)));
            if(output.size()>131072)throw new IllegalArgumentException("COMPLEXITY_LIMIT: cavity mesh");
        }
    }
    private static boolean same(CavityCell.Kind kind,net.minecraft.world.level.material.FluidState fluid){return kind==CavityCell.Kind.WATER?(fluid.getType()==Fluids.WATER||fluid.getType()==Fluids.FLOWING_WATER):(fluid.getType()==Fluids.LAVA||fluid.getType()==Fluids.FLOWING_LAVA);}
    private static float corner(BlockAndTintGetter world,BlockPos pos,int[] local,CavityCell.Kind kind,int x,int z,Map<BlockPos,Optional<int[]>> cache){
        float total=0;int count=0;for(int dz=-1;dz<=0;dz++)for(int dx=-1;dx<=0;dx++){
            int xx=x+dx,zz=z+dz;var at=pos.offset(Math.floorDiv(xx,16),0,Math.floorDiv(zz,16));float h;
            if(at.equals(pos))h=local[Math.floorMod(xx,16)+16*Math.floorMod(zz,16)]/16f;
            else{var view=world.getModelData(at).get(CompositeCellEntity.VIEW);if(view!=null&&view.cavity().isPresent()){
                var values=cache.computeIfAbsent(at,k->net.nicomar.nomoregrids.simulation.fluid.CavitySurface.heights(view.cavity().orElseThrow(),kind));if(values.isEmpty())continue;h=values.orElseThrow()[Math.floorMod(xx,16)+16*Math.floorMod(zz,16)]/16f;
            }else{var state=world.getBlockState(at);var fluid=state.getFluidState();if(!(state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)||!same(kind,fluid))continue;h=fluid.getHeight(world,at);}}
            if(h>0){total+=h;count++;}
        }return count==0?0:total/count;
    }
    private static Vec3 point(Direction6 face,int plane,int u,int v){return face.axis()==0?new Vec3(plane/16.0,v/16.0,u/16.0):face.axis()==1?new Vec3(u/16.0,plane/16.0,v/16.0):new Vec3(u/16.0,v/16.0,plane/16.0);}
    private static Vector3f vec(Vec3 p){return new Vector3f((float)p.x,(float)p.y,(float)p.z);}
}
