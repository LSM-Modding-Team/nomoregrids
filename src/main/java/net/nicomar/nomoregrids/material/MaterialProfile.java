package net.nicomar.nomoregrids.material;

/** Physical properties are independent of cosmetic paint and source texture. */
public record MaterialProfile(float hardness,float resistance,float friction,float restitution,
        int emission,int attenuation,boolean opaque,boolean supports,boolean combustible) {
    public MaterialProfile {
        if(!Float.isFinite(hardness)||!Float.isFinite(resistance)||!Float.isFinite(friction)||!Float.isFinite(restitution)
            ||resistance<0||friction<0||restitution<0||emission<0||emission>15||attenuation<0||attenuation>15)throw new IllegalArgumentException("Invalid material profile");
    }
}
