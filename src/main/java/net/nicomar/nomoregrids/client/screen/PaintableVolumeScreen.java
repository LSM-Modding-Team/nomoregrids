package net.nicomar.nomoregrids.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.nicomar.nomoregrids.network.*;

/** Server-owned PB volume preview/configuration; confirmation is one complete operation. */
public final class PaintableVolumeScreen extends Screen {
    private static PaintableSelectionPayload latest;private final java.util.UUID session;private int widthBlocks=1,heightBlocks=1,depthBlocks=1,resolution,direction;private boolean create,upper;
    public PaintableVolumeScreen(PaintableSelectionPayload state){super(Component.translatable("item.paintableblocks.slope_former"));session=state.session();resolution=state.resolution();direction=state.direction();upper=state.upper();create=state.create();}
    public static void offer(PaintableSelectionPayload state){latest=state;var mc=Minecraft.getInstance();if(state.phase()==0){if(mc.gui.screen() instanceof PaintableVolumeScreen)mc.gui.setScreen(null);return;}if(state.phase()==3&&!(mc.gui.screen() instanceof PaintableVolumeScreen))mc.gui.setScreen(new PaintableVolumeScreen(state));}
    private void action(int action){ClientPacketDistributor.sendToServer(new PaintableConfigPayload(session,action,create,widthBlocks,heightBlocks,depthBlocks,resolution,direction,upper));}
    private void button(String text,int y,Runnable run){addRenderableWidget(Button.builder(Component.literal(text),b->{run.run();action(0);rebuildWidgets();}).bounds(width/2-130,height/2+y,260,18).build());}
    @Override protected void init(){
        button(Component.translatable("button.paintableblocks.slope_direction").getString()+": "+new String[]{"+X","+Z","−X","−Z"}[direction],-80,()->direction=(direction+1)%4);
        button(Component.translatable(upper?"button.paintableblocks.upper":"button.paintableblocks.lower").getString(),-59,()->upper=!upper);
        if(minecraft.player!=null&&minecraft.player.isCreative()){button(Component.translatable(create?"button.paintableblocks.create":"button.paintableblocks.transform").getString(),-38,()->create=!create);if(create){button("X: "+widthBlocks,-17,()->widthBlocks=widthBlocks%16+1);button("Y: "+heightBlocks,4,()->heightBlocks=heightBlocks%16+1);button("Z: "+depthBlocks,25,()->depthBlocks=depthBlocks%16+1);button(resolution+"×"+resolution,46,()->resolution=resolution==16?1:resolution*2);}}
        addRenderableWidget(Button.builder(Component.translatable("button.paintableblocks.confirm"),b->{action(1);minecraft.gui.setScreen(null);}).bounds(width/2-130,height/2+70,126,20).build());addRenderableWidget(Button.builder(Component.translatable("button.paintableblocks.cancel"),b->onClose()).bounds(width/2+4,height/2+70,126,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float tick){g.fill(width/2-140,height/2-110,width/2+140,height/2+100,0xdd222222);g.text(font,title,width/2-130,height/2-101,0xffffffff);super.extractRenderState(g,x,y,tick);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){action(2);super.onClose();}
}
