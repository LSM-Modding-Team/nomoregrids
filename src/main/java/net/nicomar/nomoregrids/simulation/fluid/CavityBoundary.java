package net.nicomar.nomoregrids.simulation.fluid;

/** Visibility of a fluid microcell against an optional adjacent cavity. */
public final class CavityBoundary {
    private CavityBoundary(){}
    public static boolean visible(CavityCell local,CavityCell neighbor,CavityCell.Kind kind,int x,int y,int z,int dx,int dy,int dz){
        int nx=x+dx,ny=y+dy,nz=z+dz;
        if(nx>=0&&nx<16&&ny>=0&&ny<16&&nz>=0&&nz<16)return local.at(nx+16*(ny+16*nz))!=kind;
        return neighbor==null||neighbor.at(Math.floorMod(nx,16)+16*(Math.floorMod(ny,16)+16*Math.floorMod(nz,16)))!=kind;
    }
}
