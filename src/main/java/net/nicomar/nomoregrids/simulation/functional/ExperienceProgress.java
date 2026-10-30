package net.nicomar.nomoregrids.simulation.functional;

/** Vanilla level curve and fractional progress, used in the same resource receipt as a furnace withdrawal. */
public record ExperienceProgress(int level,int total,float progress){
    public ExperienceProgress{if(level<0||total<0||!Float.isFinite(progress)||progress<0||progress>=1)throw new IllegalArgumentException("Invalid XP frame");}
    private static int cost(int level){return level>=30?Math.addExact(112,Math.multiplyExact(level-30,9)):level>=15?37+(level-15)*5:7+level*2;}
    public ExperienceProgress add(int points){if(points<0)throw new IllegalArgumentException("Negative XP");int next=level;float fraction=progress+(float)points/cost(next);while(fraction>=1){float excess=(fraction-1)*cost(next);next=Math.incrementExact(next);fraction=excess/cost(next);}return new ExperienceProgress(next,Math.addExact(total,points),fraction);}
}
