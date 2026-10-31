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
import org.joml.Vector3f;

/** Greedy surface rectangles on cavity microcells; native fluid materials and biome tint. */
public final class CavityMesh {
    private CavityMesh(){}
    public static void append(CavityCell cell,BlockAndTintGetter world,BlockPos pos,List<BakedQuad> output){
        for(var kind:List.of(CavityCell.Kind.WATER,CavityCell.Kind.LAVA)){
            if(cell.amount(kind)==0)continue;
            var fluid=kind==CavityCell.Kind.WATER?Fluids.WATER.defaultFluidState():Fluids.LAVA.defaultFluidState();var model=Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid);var source=fluid.createLegacyBlock();int tint=model.tintSource()==null?0xffffffff:0xff000000|model.tintSource().colorInWorld(source,world,pos);
            for(var face:Direction6.values()){
                var material=face==Direction6.UP||face==Direction6.DOWN?model.stillMaterial():model.flowingMaterial();var sprite=material.sprite();var info=BakedQuad.MaterialInfo.of(material,kind==CavityCell.Kind.WATER?com.mojang.blaze3d.platform.Transparency.TRANSLUCENT:com.mojang.blaze3d.platform.Transparency.NONE,-1,true,kind==CavityCell.Kind.LAVA?15:0);boolean[] visible=new boolean[256];
                for(int plane=face.sign()>0?1:0;plane<=(face.sign()>0?16:15);plane++){
                Arrays.fill(visible,false);for(int v=0;v<16;v++)for(int u=0;u<16;u++){
                    int c=plane-(face.sign()>0?1:0);if(c<0||c>=16)continue;int x=face.axis()==0?c:u,y=face.axis()==1?c:v,z=face.axis()==0?u:face.axis()==1?v:c;
                    if(cell.at(x+16*(y+16*z))!=kind)continue;int nx=x+face.x,ny=y+face.y,nz=z+face.z;
                    visible[v*16+u]=nx<0||nx>=16||ny<0||ny>=16||nz<0||nz>=16||cell.at(nx+16*(ny+16*nz))!=kind;
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
        }
    }
    private static Vec3 point(Direction6 face,int plane,int u,int v){return face.axis()==0?new Vec3(plane/16.0,v/16.0,u/16.0):face.axis()==1?new Vec3(u/16.0,plane/16.0,v/16.0):new Vec3(u/16.0,v/16.0,plane/16.0);}
    private static Vector3f vec(Vec3 p){return new Vector3f((float)p.x,(float)p.y,(float)p.z);}
}
