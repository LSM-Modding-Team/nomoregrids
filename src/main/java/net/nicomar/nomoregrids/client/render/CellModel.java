package net.nicomar.nomoregrids.client.render;

import com.mojang.serialization.MapCodec;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.*;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.client.model.quad.*;
import net.nicomar.nomoregrids.core.*;
import net.nicomar.nomoregrids.geometry.Rotation;
import net.nicomar.nomoregrids.geometry.ConvexVolume;
import net.nicomar.nomoregrids.geometry.BoxMesh;
import net.nicomar.nomoregrids.compat.paintableblocks.PaintableAdapter;
import net.nicomar.nomoregrids.platform.NativeStates;
import net.nicomar.nomoregrids.world.*;
import org.joml.Vector3f;

/** Section-baked multipart model; emits only the visible fragment owned by the current cell. */
public final class CellModel implements DynamicBlockStateModel {
    private final BlockStateModelPart fallback;
    private final PaintableAdapter paintable;
    public CellModel(BlockStateModelPart fallback){this.fallback=fallback;
        PaintableAdapter optional=null;try{optional=new PaintableAdapter(CellModel.class.getClassLoader());}catch(ReflectiveOperationException|RuntimeException|LinkageError ignored){}paintable=optional;}
    private record Key(CellView view,BlockPos cell,CellModel resourceGeneration){}
    @Override public Object createGeometryKey(BlockAndTintGetter world,BlockPos pos,BlockState state,RandomSource random){return new Key(world.getModelData(pos).get(CompositeCellEntity.VIEW),pos.immutable(),this);}
    @Override public Material.Baked particleMaterial(){return fallback.particleMaterial();}
    @Override public int materialFlags(){return 3;}
    private record Part(List<BakedQuad> quads,Material.Baked particleMaterial,int materialFlags) implements BlockStateModelPart{
        @Override public List<BakedQuad> getQuads(Direction side){return side==null?quads:List.of();}
        @Override public boolean useAmbientOcclusion(){return true;}
    }
    @Override public void collectParts(BlockAndTintGetter world,BlockPos pos,BlockState state,RandomSource random,List<BlockStateModelPart> parts){
        CellView view=world.getModelData(pos).get(CompositeCellEntity.VIEW);if(view==null){parts.add(fallback);return;}
        List<BakedQuad> output=new ArrayList<>();int flags=0;
        for(var p:view.replicas()){
            int start=output.size();try{
            if(net.nicomar.nomoregrids.platform.WorldDecorations.rendered(p)&&!p.flags().contains(PieceRecord.Flag.SUBSTITUTE))continue;
            if(p.flags().contains(PieceRecord.Flag.SUBSTITUTE)){placeholder(p,pos,output);continue;}
            if(p.adapterId().value().equals("nomoregrids:paintableblocks")&&paintable!=null){paintable(p,pos,world,output);continue;}
            BlockState source=NativeStates.resolve(p);var nativeModel=Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(source);
            var nativeParts=new ArrayList<BlockStateModelPart>();nativeModel.collectParts(RandomSource.create(0),nativeParts);
            var samples=new ArrayList<BakedQuad>();
            for(var part:nativeParts){samples.addAll(part.getQuads(null));for(var face:Direction.values())samples.addAll(part.getQuads(face));}
            int cut=net.nicomar.nomoregrids.material.FragmentItem.cuttable(source.getBlock())?Math.toIntExact(p.geometry().occupation().bounds().max().y()):16;
            for(var sample:samples)emitNative(p,pos,tint(sample,source,world,pos),cut,output);
            if(cut<16)for(var face:BoxMesh.extract(p.geometry().occupation(),131072))if(face.normal()==Direction6.UP&&face.plane()==cut){
                for(var sample:samples)if(sample.direction()==Direction.UP)emitCap(p,pos,face,tint(sample,source,world,pos),output);
            }
            if(output.size()>131072)throw new IllegalArgumentException("Section mesh triangle budget");
            }catch(RuntimeException ex){output.subList(start,output.size()).clear();placeholder(p,pos,output);}
        }
        view.cavity().ifPresent(c->CavityMesh.append(c,world,pos,output));
        for(var q:output)flags|=q.materialInfo().flags();parts.add(new Part(List.copyOf(output),particleMaterial(),flags));
    }
    private void placeholder(PieceRecord piece,BlockPos cell,List<BakedQuad> output){
        var shape=new net.nicomar.nomoregrids.geometry.BoxUnion(List.of(piece.geometry().occupation().bounds()));
        for(var face:BoxMesh.extract(shape,100))for(var sample:fallback.getQuads(Direction.valueOf(face.normal().name())))emit(piece,cell,face,sample,output);
    }
    private record Vertex(Vec3 point,float u,float v,int color){
        Vertex mix(Vertex b,double t){int c=0;for(int shift=0;shift<32;shift+=8)c|=(int)Math.round(((color>>>shift)&255)+(((b.color>>>shift)&255)-((color>>>shift)&255))*t)<<shift;
            return new Vertex(point.add(b.point.subtract(point).scale(t)),(float)(u+(b.u-u)*t),(float)(v+(b.v-v)*t),c);}
    }
    private static double coord(Vec3 p,int axis){return axis==0?p.x:axis==1?p.y:p.z;}
    private static Vec3 point(int axis,double plane,double u,double v){return axis==0?new Vec3(plane,v,u):axis==1?new Vec3(u,plane,v):new Vec3(u,v,plane);}
    private static void emit(PieceRecord piece,BlockPos cell,BoxMesh.Face face,BakedQuad source,List<BakedQuad> output){
        int axis=face.normal().axis();
        var r=face.rect();List<Vec3> local=new ArrayList<>(List.of(point(axis,face.plane(),r.u0(),r.v0()),point(axis,face.plane(),r.u1(),r.v0()),point(axis,face.plane(),r.u1(),r.v1()),point(axis,face.plane(),r.u0(),r.v1())));
        if(axis==2?face.normal().sign()<0:face.normal().sign()>0)Collections.reverse(local);
        emitPolygon(piece,cell,local,face.normal(),source,output);
    }
    private static void emitPolygon(PieceRecord piece,BlockPos cell,List<Vec3> local,Direction6 face,BakedQuad source,List<BakedQuad> output){
        int axis=face.axis(),uAxis=axis==0?2:0,vAxis=axis==1?2:1;
        var p0=source.position(0);var p1=source.position(1);var p3=source.position(3);
        double a=component(p1,uAxis)-component(p0,uAxis),b=component(p3,uAxis)-component(p0,uAxis),c=component(p1,vAxis)-component(p0,vAxis),d=component(p3,vAxis)-component(p0,vAxis),det=a*d-b*c;
        if(Math.abs(det)<1e-12)return;
        float u0=UVPair.unpackU(source.packedUV(0)),v0=UVPair.unpackV(source.packedUV(0));
        var vertices=new ArrayList<Vertex>();
        for(var p:local){double u=coord(p,uAxis)/16-component(p0,uAxis),v=coord(p,vAxis)/16-component(p0,vAxis),s=(u*d-b*v)/det,t=(a*v-u*c)/det;
            float texU=(float)(u0+s*(UVPair.unpackU(source.packedUV(1))-u0)+t*(UVPair.unpackU(source.packedUV(3))-u0));
            float texV=(float)(v0+s*(UVPair.unpackV(source.packedUV(1))-v0)+t*(UVPair.unpackV(source.packedUV(3))-v0));
            vertices.add(new Vertex(p,texU,texV,source.bakedColors().color(0)));
        }
        finish(piece,cell,vertices,face,source,output);
    }
    private static void emitNative(PieceRecord piece,BlockPos cell,BakedQuad source,int cut,List<BakedQuad> output){
        var vertices=new ArrayList<Vertex>();for(int i=0;i<4;i++){
            var p=source.position(i);vertices.add(new Vertex(new Vec3(p.x()*16,p.y()*16,p.z()*16),UVPair.unpackU(source.packedUV(i)),UVPair.unpackV(source.packedUV(i)),source.bakedColors().color(i)));
        }
        List<Vertex> polygon=cut<16?clip(vertices,1,cut,false):vertices;
        finish(piece,cell,polygon,Direction6.valueOf(source.direction().name()),source,output);
    }
    private static void emitCap(PieceRecord piece,BlockPos cell,BoxMesh.Face face,BakedQuad source,List<BakedQuad> output){
        double x0=Double.POSITIVE_INFINITY,x1=Double.NEGATIVE_INFINITY,z0=x0,z1=x1;
        for(int i=0;i<4;i++){var p=source.position(i);x0=Math.min(x0,p.x()*16);x1=Math.max(x1,p.x()*16);z0=Math.min(z0,p.z()*16);z1=Math.max(z1,p.z()*16);}
        var r=face.rect();double u0=Math.max(r.u0(),x0),u1=Math.min(r.u1(),x1),v0=Math.max(r.v0(),z0),v1=Math.min(r.v1(),z1);
        if(u0>=u1||v0>=v1)return;
        var local=new ArrayList<>(List.of(new Vec3(u0,face.plane(),v0),new Vec3(u1,face.plane(),v0),new Vec3(u1,face.plane(),v1),new Vec3(u0,face.plane(),v1)));Collections.reverse(local);
        emitPolygon(piece,cell,local,Direction6.UP,source,output);
    }
    private static void finish(PieceRecord piece,BlockPos cell,List<Vertex> local,Direction6 face,BakedQuad source,List<BakedQuad> output){
        if(local.size()<3)return;Direction6 transformed=face.rotated(piece.orientation());Rotation rotation=Rotation.of(piece.orientation());
        GridPoint aX=rotation.apply(new GridPoint(1,0,0)),aY=rotation.apply(new GridPoint(0,1,0)),aZ=rotation.apply(new GridPoint(0,0,1));
        var vertices=new ArrayList<Vertex>();for(var vertex:local){var p=vertex.point;
            double lx=p.x-piece.pivot16().x(),ly=p.y-piece.pivot16().y(),lz=p.z-piece.pivot16().z();
            Vec3 world=new Vec3(aX.x()*lx+aY.x()*ly+aZ.x()*lz+piece.pivot16().x()+piece.origin16().x(),aX.y()*lx+aY.y()*ly+aZ.y()*lz+piece.pivot16().y()+piece.origin16().y(),aX.z()*lx+aY.z()*ly+aZ.z()*lz+piece.pivot16().z()+piece.origin16().z());
            vertices.add(new Vertex(new Vec3(world.x/16-cell.getX(),world.y/16-cell.getY(),world.z/16-cell.getZ()),vertex.u,vertex.v,vertex.color));
        }
        // A planar boundary face belongs to the cell on its solid interior side.
        double plane=coord(vertices.getFirst().point,transformed.axis());boolean planar=vertices.stream().allMatch(v->Math.abs(coord(v.point,transformed.axis())-plane)<1e-10);
        if(planar&&(transformed.sign()>0&&plane==0||transformed.sign()<0&&plane==1))return;
        List<Vertex> polygon=vertices;
        for(int axis=0;axis<3;axis++){polygon=clip(polygon,axis,0,true);polygon=clip(polygon,axis,1,false);}
        if(polygon.size()<3)return;var material=source.materialInfo();Direction normal=Direction.valueOf(transformed.name());
        for(int i=1;i+1<polygon.size();i++){
            Vertex x=polygon.getFirst(),y=polygon.get(i),z=polygon.get(i+1);if(y.point.subtract(x.point).cross(z.point.subtract(x.point)).lengthSqr()<1e-18)continue;
            if(output.size()>=131072)throw new IllegalArgumentException("Section mesh triangle budget");
            output.add(new BakedQuad(vector(x),vector(y),vector(z),vector(z),UVPair.pack(x.u,x.v),UVPair.pack(y.u,y.v),UVPair.pack(z.u,z.v),UVPair.pack(z.u,z.v),normal,material,
                normals(x,y,z),BakedColors.of(x.color,y.color,z.color,z.color)));
        }
    }
    private static BakedNormals normals(Vertex a,Vertex b,Vertex c){var n=b.point.subtract(a.point).cross(c.point.subtract(a.point)).normalize();return BakedNormals.of(BakedNormals.pack((float)n.x,(float)n.y,(float)n.z));}
    private record PaintedFace(BlockState state,Direction face){}
    private void paintable(PieceRecord piece,BlockPos cell,BlockAndTintGetter world,List<BakedQuad> output){
        var primitives=paintable.primitives(piece.geometryPayload());
        var pointIndex=paintable.surfaceIndex(piece.geometryPayload());
        var materials=new HashMap<Integer,BlockState>();materials.put(0,NativeStates.resolve(piece));var models=new HashMap<PaintedFace,List<BakedQuad>>();
        for(int primitiveIndex=0;primitiveIndex<primitives.size();primitiveIndex++){var primitive=primitives.get(primitiveIndex);for(int index=0;index<primitive.geometry().planes().size();index++){
            var plane=primitive.geometry().planes().get(index);var vertices=primitive.geometry().vertices().stream().filter(p->Math.abs(plane.normal().dot(p)-plane.distance())<ConvexVolume.EPS).toList();
            if(vertices.size()<3)continue;
            var center=new ConvexVolume.Vec(0,0,0);for(var p:vertices)center=center.add(p.scale(1.0/vertices.size()));
            var outside=center.add(plane.normal().scale(ConvexVolume.EPS*4));
            if(pointIndex.covered(outside,primitiveIndex))continue;
            var n=plane.normal();var basis=n.cross(Math.abs(n.x())<0.8?new ConvexVolume.Vec(1,0,0):new ConvexVolume.Vec(0,1,0));basis=basis.scale(1/basis.length());
            var u=basis;var v=n.cross(u);var c=center;
            var sorted=vertices.stream().sorted(Comparator.comparingDouble(p->Math.atan2(p.sub(c).dot(v),p.sub(c).dot(u)))).map(p->new Vec3(p.x(),p.y(),p.z())).toList();
            Direction6 face=Arrays.stream(Direction6.values()).max(Comparator.comparingDouble(d->d.x*n.x()+d.y*n.y()+d.z*n.z())).orElseThrow();
            int key=switch(index){case 0->4;case 1->5;case 2->0;case 3->1;case 4->2;case 5->3;default->6;};
            int paint=primitive.paint().getOrDefault(key,0);BlockState material=materials.get(paint);
            if(material==null)try{Object entry=Class.forName("net.nicomar.paintableblocks.paint.PaintCatalog").getMethod("byId",int.class).invoke(null,paint);material=(BlockState)entry.getClass().getMethod("sampleState").invoke(entry);materials.put(paint,material);}catch(ReflectiveOperationException ex){throw new IllegalArgumentException("PB paint API mismatch",ex);}
            Direction normal=Direction.valueOf(face.name());var modelKey=new PaintedFace(material,normal);var samples=models.get(modelKey);
            if(samples==null){var model=Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(material);var parts=new ArrayList<BlockStateModelPart>();model.collectParts(RandomSource.create(0),parts);var fresh=new ArrayList<BakedQuad>();for(var part:parts){fresh.addAll(part.getQuads(normal));for(var q:part.getQuads(null))if(q.direction()==normal)fresh.add(q);}if(fresh.isEmpty())fresh.addAll(fallback.getQuads(normal));var tinted=new ArrayList<BakedQuad>();for(var source:fresh)tinted.add(tint(source,material,world,cell));samples=List.copyOf(tinted);models.put(modelKey,samples);}
            for(var source:samples)emitPolygon(piece,cell,sorted,face,source,output);
        }}
    }
    private static BakedQuad tint(BakedQuad source,BlockState state,BlockAndTintGetter world,BlockPos pos){
        var old=source.materialInfo();if(!old.isTinted())return source;
        var tint=Minecraft.getInstance().getBlockColors().getTintSource(state,old.tintIndex());int color=tint==null?0xffffffff:0xff000000|tint.colorInWorld(state,world,pos);
        var material=new BakedQuad.MaterialInfo(old.sprite(),old.layer(),old.itemRenderType(),-1,old.shade(),old.lightEmission(),old.ambientOcclusion());
        return new BakedQuad(source.position0(),source.position1(),source.position2(),source.position3(),source.packedUV0(),source.packedUV1(),source.packedUV2(),source.packedUV3(),source.direction(),material,source.bakedNormals(),
            BakedColors.of(multiply(source.bakedColors().color(0),color),multiply(source.bakedColors().color(1),color),multiply(source.bakedColors().color(2),color),multiply(source.bakedColors().color(3),color)));
    }
    private static int multiply(int a,int b){int result=0;for(int shift=0;shift<32;shift+=8)result|=(((a>>>shift)&255)*((b>>>shift)&255)/255)<<shift;return result;}
    private static double component(org.joml.Vector3fc p,int axis){return axis==0?p.x():axis==1?p.y():p.z();}
    private static Vector3f vector(Vertex v){return new Vector3f((float)v.point.x,(float)v.point.y,(float)v.point.z);}
    private static List<Vertex> clip(List<Vertex> input,int axis,double limit,boolean minimum){
        if(input.isEmpty())return input;var out=new ArrayList<Vertex>();Vertex last=input.getLast();double previous=(coord(last.point,axis)-limit)*(minimum?1:-1);
        for(Vertex next:input){double distance=(coord(next.point,axis)-limit)*(minimum?1:-1);if((distance>=0)!=(previous>=0))out.add(last.mix(next,previous/(previous-distance)));if(distance>=0)out.add(next);last=next;previous=distance;}return out;
    }
    public record Unbaked(Identifier baseModel) implements CustomUnbakedBlockStateModel {
        public static final MapCodec<Unbaked> CODEC=Identifier.CODEC.fieldOf("base_model").xmap(Unbaked::new,Unbaked::baseModel);
        @Override public BlockStateModel bake(ModelBaker baker){return new CellModel(new Variant(baseModel).bake(baker));}
        @Override public void resolveDependencies(Resolver resolver){resolver.markDependency(baseModel);}
        @Override public MapCodec<? extends CustomUnbakedBlockStateModel> codec(){return CODEC;}
    }
}
