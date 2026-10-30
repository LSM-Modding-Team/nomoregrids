package net.nicomar.nomoregrids.client.screen;

import java.util.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.nicomar.nomoregrids.network.SignEditorPayload;

public final class ComponentSignScreen extends Screen {
    private final SignEditorPayload state;private final List<EditBox> lines=new ArrayList<>();
    public ComponentSignScreen(SignEditorPayload state){super(Component.translatable("sign.edit"));this.state=state;}
    @Override protected void init(){lines.clear();for(int i=0;i<4;i++){var line=new EditBox(font,width/2-120,height/2-55+i*24,240,20,Component.empty());line.setMaxLength(384);line.setValue(state.lines().get(i));addRenderableWidget(line);lines.add(line);}addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->{ClientPacketDistributor.sendToServer(new SignEditorPayload(state.piece(),state.revision(),state.front(),lines.stream().map(EditBox::getValue).toList()));minecraft.gui.setScreen(null);}).bounds(width/2-60,height/2+55,120,20).build());}
    @Override public boolean isPauseScreen(){return false;}
}
