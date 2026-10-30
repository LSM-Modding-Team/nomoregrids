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
        PaintableAdapter optional=null;try{optional=new PaintableAdapter(CellModel.class.getClassLoader());}catch(ReflectiveOperationException|IllegalArgumentException ignored){}paintable=optional;}
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
            try{
            if(p.flags().contains(PieceRecord.Flag.SUBSTITUTE)){placeholder(p,pos,output);continue;}
            if(p.adapterId().value().equals("nomoregrids:paintableblocks")&&paintable!=null){paintable(p,pos,output);continue;}
            BlockState source=NativeStates.resolve(p);var nativeModel=Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(source);
            var nativeParts=new ArrayList<BlockStateModelPart>();nativeModel.collectParts(RandomSource.create(0),nativeParts);
            for(var face:BoxMesh.extract(p.geometry().occupation(),131072)){
                Direction direction=Direction.valueOf(face.normal().name());List<BakedQuad> samples=new ArrayList<>();
                for(var part:nativeParts){samples.addAll(part.getQuads(direction));for(var q:part.getQuads(null))if(q.direction()==direction)samples.add(q);}
                if(samples.isEmpty())samples.addAll(fallback.getQuads(direction));
                for(var sample:samples)emit(p,pos,face,sample,output);
                if(output.size()>131072)throw new IllegalArgumentException("Section mesh triangle budget");
            }
            }catch(RuntimeException ex){placeholder(p,pos,output);}
        }
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
        Direction6 transformed=face.rotated(piece.orientation());Rotation rotation=Rotation.of(piece.orientation());
        var p0=source.position(0);var p1=source.position(1);var p3=source.position(3);
        double a=component(p1,uAxis)-component(p0,uAxis),b=component(p3,uAxis)-component(p0,uAxis),c=component(p1,vAxis)-component(p0,vAxis),d=component(p3,vAxis)-component(p0,vAxis),det=a*d-b*c;
        if(Math.abs(det)<1e-12)return;
        float u0=UVPair.unpackU(source.packedUV(0)),v0=UVPair.unpackV(source.packedUV(0));
        var vertices=new ArrayList<Vertex>();
        for(var p:local){double u=coord(p,uAxis)/16-component(p0,uAxis),v=coord(p,vAxis)/16-component(p0,vAxis),s=(u*d-b*v)/det,t=(a*v-u*c)/det;
            float texU=(float)(u0+s*(UVPair.unpackU(source.packedUV(1))-u0)+t*(UVPair.unpackU(source.packedUV(3))-u0));
            float texV=(float)(v0+s*(UVPair.unpackV(source.packedUV(1))-v0)+t*(UVPair.unpackV(source.packedUV(3))-v0));
            GridPoint aX=rotation.apply(new GridPoint(1,0,0)),aY=rotation.apply(new GridPoint(0,1,0)),aZ=rotation.apply(new GridPoint(0,0,1));
            double lx=p.x-piece.pivot16().x(),ly=p.y-piece.pivot16().y(),lz=p.z-piece.pivot16().z();
            Vec3 world=new Vec3(aX.x()*lx+aY.x()*ly+aZ.x()*lz+piece.pivot16().x()+piece.origin16().x(),aX.y()*lx+aY.y()*ly+aZ.y()*lz+piece.pivot16().y()+piece.origin16().y(),aX.z()*lx+aY.z()*ly+aZ.z()*lz+piece.pivot16().z()+piece.origin16().z());
            vertices.add(new Vertex(new Vec3(world.x/16-cell.getX(),world.y/16-cell.getY(),world.z/16-cell.getZ()),texU,texV,source.bakedColors().color(0)));
        }
        // On a cell boundary emit only in the cell on the solid's interior side.
        double plane=coord(vertices.getFirst().point,transformed.axis());if(transformed.sign()>0&&plane==0||transformed.sign()<0&&plane==1)return;
        List<Vertex> polygon=vertices;
        for(int clipAxis=0;clipAxis<3;clipAxis++){polygon=clip(polygon,clipAxis,0,true);polygon=clip(polygon,clipAxis,1,false);}
        if(polygon.size()<3)return;
        var material=source.materialInfo();Direction normal=Direction.valueOf(transformed.name());
        for(int i=1;i+1<polygon.size();i++){
            Vertex x=polygon.getFirst(),y=polygon.get(i),z=polygon.get(i+1);if(y.point.subtract(x.point).cross(z.point.subtract(x.point)).lengthSqr()<1e-18)continue;
            output.add(new BakedQuad(vector(x),vector(y),vector(z),vector(z),UVPair.pack(x.u,x.v),UVPair.pack(y.u,y.v),UVPair.pack(z.u,z.v),UVPair.pack(z.u,z.v),normal,material,
                normals(x,y,z),BakedColors.of(x.color,y.color,z.color,z.color)));
        }
    }
    private static BakedNormals normals(Vertex a,Vertex b,Vertex c){var n=b.point.subtract(a.point).cross(c.point.subtract(a.point)).normalize();return BakedNormals.of(BakedNormals.pack((float)n.x,(float)n.y,(float)n.z));}
    private void paintable(PieceRecord piece,BlockPos cell,List<BakedQuad> output){
        var primitives=paintable.primitives(piece.geometryPayload());
        for(var primitive:primitives)for(int index=0;index<primitive.geometry().planes().size();index++){
            var plane=primitive.geometry().planes().get(index);var vertices=primitive.geometry().vertices().stream().filter(p->Math.abs(plane.normal().dot(p)-plane.distance())<ConvexVolume.EPS).toList();
            if(vertices.size()<3)continue;
            var center=new ConvexVolume.Vec(0,0,0);for(var p:vertices)center=center.add(p.scale(1.0/vertices.size()));
            var outside=center.add(plane.normal().scale(ConvexVolume.EPS*4));
            if(primitives.stream().anyMatch(other->other!=primitive&&other.geometry().planes().stream().allMatch(f->f.normal().dot(outside)<=f.distance())))continue;
            var n=plane.normal();var basis=n.cross(Math.abs(n.x())<0.8?new ConvexVolume.Vec(1,0,0):new ConvexVolume.Vec(0,1,0));basis=basis.scale(1/basis.length());
            var u=basis;var v=n.cross(u);var c=center;
            var sorted=vertices.stream().sorted(Comparator.comparingDouble(p->Math.atan2(p.sub(c).dot(v),p.sub(c).dot(u)))).map(p->new Vec3(p.x(),p.y(),p.z())).toList();
            Direction6 face=Arrays.stream(Direction6.values()).max(Comparator.comparingDouble(d->d.x*n.x()+d.y*n.y()+d.z*n.z())).orElseThrow();
            int key=switch(index){case 0->4;case 1->5;case 2->0;case 3->1;case 4->2;case 5->3;default->6;};
            BlockState material=NativeStates.resolve(piece);int paint=primitive.paint().getOrDefault(key,0);
            if(paint!=0)try{Object entry=Class.forName("net.nicomar.paintableblocks.paint.PaintCatalog").getMethod("byId",int.class).invoke(null,paint);material=(BlockState)entry.getClass().getMethod("sampleState").invoke(entry);}catch(ReflectiveOperationException ex){throw new IllegalArgumentException("PB paint API mismatch",ex);}
            var model=Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(material);var parts=new ArrayList<BlockStateModelPart>();model.collectParts(RandomSource.create(0),parts);
            Direction normal=Direction.valueOf(face.name());var samples=new ArrayList<BakedQuad>();for(var part:parts){samples.addAll(part.getQuads(normal));for(var q:part.getQuads(null))if(q.direction()==normal)samples.add(q);}
            if(samples.isEmpty())samples.addAll(fallback.getQuads(normal));for(var source:samples)emitPolygon(piece,cell,sorted,face,source,output);
        }
    }
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
