package net.nicomar.nomoregrids.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.*;
import net.nicomar.nomoregrids.diagnostic.MeasurementWindow;
import net.nicomar.nomoregrids.persistence.SectionStore;

/** Opt-in render-thread observations. Intervals include frame limiting; CPU render time is not GPU time. */
public final class ClientMeasurements {
    private static final class Run {
        final String label;final Object world;final String dimension;
        final MeasurementWindow intervals,cpu;final net.minecraft.world.phys.Vec3 position;final float yaw,pitch;final JsonObject options;
        long previous,start,measuredEpochMillis;boolean record;
        Run(String label,int warmup,int frames){var mc=Minecraft.getInstance();this.label=label;world=mc.level;dimension=mc.level.dimension().identifier().toString();intervals=new MeasurementWindow(warmup,frames);cpu=new MeasurementWindow(warmup,frames);position=mc.player.position();yaw=mc.player.getYRot();pitch=mc.player.getXRot();options=options();}
    }
    private static Run active;
    private ClientMeasurements(){}
    public static void register(){if(!Boolean.getBoolean("nomoregrids.profileFrames"))return;NeoForge.EVENT_BUS.addListener(ClientMeasurements::commands);NeoForge.EVENT_BUS.addListener(ClientMeasurements::pre);NeoForge.EVENT_BUS.addListener(ClientMeasurements::post);}
    private static void commands(RegisterClientCommandsEvent event){
        event.getDispatcher().register(Commands.literal("nmg_frames")
            .then(Commands.literal("start").then(Commands.argument("label",StringArgumentType.word()).then(Commands.argument("warmup",IntegerArgumentType.integer(0,12000)).then(Commands.argument("frames",IntegerArgumentType.integer(1,12000)).executes(ctx->{
                if(active!=null||Minecraft.getInstance().level==null||Minecraft.getInstance().player==null){ctx.getSource().sendFailure(Component.literal("A world and no active measurement are required"));return 0;}
                active=new Run(StringArgumentType.getString(ctx,"label"),IntegerArgumentType.getInteger(ctx,"warmup"),IntegerArgumentType.getInteger(ctx,"frames"));return 1;
            })))))
            .then(Commands.literal("stop").executes(ctx->{if(active==null)return 0;finish(true,"cancelled");return 1;})));
    }
    private static void pre(RenderFrameEvent.Pre event){
        var run=active;if(run==null)return;
        var mc=Minecraft.getInstance();
        if(mc.level!=run.world||mc.player==null||mc.gui.screen()!=null){finish(true,"world_changed");return;}
        if(mc.player.position().distanceToSqr(run.position)>.0001||Math.abs(mc.player.getYRot()-run.yaw)>.1||Math.abs(mc.player.getXRot()-run.pitch)>.1||!run.options.equals(options())){finish(true,"camera_or_options_changed");return;}
        long now=System.nanoTime();run.start=now;run.record=run.previous!=0;
        if(run.record){if(!run.intervals.warmingUp()&&run.measuredEpochMillis==0)run.measuredEpochMillis=System.currentTimeMillis();run.intervals.add(now-run.previous);}run.previous=now;
    }
    private static void post(RenderFrameEvent.Post event){var run=active;if(run==null||!run.record)return;if(run.cpu.add(System.nanoTime()-run.start)&&run.intervals.summary().complete())finish(false,"complete");}
    private static JsonObject options(){var mc=Minecraft.getInstance();var json=new JsonObject();json.addProperty("renderDistance",mc.options.renderDistance().get());json.addProperty("simulationDistance",mc.options.simulationDistance().get());json.addProperty("maxFps",mc.options.framerateLimit().get());json.addProperty("fov",mc.options.fov().get());json.addProperty("cameraType",mc.options.getCameraType().toString());json.add("resourcePacks",new com.google.gson.Gson().toJsonTree(mc.options.resourcePacks));
        json.addProperty("width",mc.getWindow().getWidth());json.addProperty("height",mc.getWindow().getHeight());json.addProperty("vsync",mc.options.enableVsync().get());json.addProperty("graphicsPreset",mc.options.graphicsPreset().get().toString());json.addProperty("clouds",mc.options.cloudStatus().get().toString());json.addProperty("cloudRange",mc.options.cloudRange().get());json.addProperty("weatherRadius",mc.options.weatherRadius().get());json.addProperty("ambientOcclusion",mc.options.ambientOcclusion().get());json.addProperty("entityShadows",mc.options.entityShadows().get());json.addProperty("entityDistance",mc.options.entityDistanceScaling().get());json.addProperty("particles",mc.options.particles().get().toString());json.addProperty("mipmapLevels",mc.options.mipmapLevels().get());json.addProperty("textureFiltering",mc.options.textureFiltering().get().toString());json.addProperty("anisotropy",mc.options.maxAnisotropyBit().get());json.addProperty("biomeBlend",mc.options.biomeBlendRadius().get());json.addProperty("cutoutLeaves",mc.options.cutoutLeaves().get());json.addProperty("improvedTransparency",mc.options.improvedTransparency().get());return json;}
    private static void finish(boolean aborted,String reason){
        var run=active;active=null;if(run==null)return;
        var mc=Minecraft.getInstance();var json=new JsonObject();var gson=new GsonBuilder().setPrettyPrinting().create();
        json.addProperty("schema",1);json.addProperty("kind","client-frame-window");json.addProperty("label",run.label);json.addProperty("dimension",run.dimension);json.addProperty("reason",reason);json.addProperty("aborted",aborted);json.addProperty("startedEpochMillis",run.measuredEpochMillis);json.addProperty("finishedEpochMillis",System.currentTimeMillis());
        json.add("frameInterval",gson.toJsonTree(run.intervals.summary()));json.add("cpuRender",gson.toJsonTree(run.cpu.summary()));
        json.addProperty("java",System.getProperty("java.version"));json.addProperty("os",System.getProperty("os.name"));json.addProperty("processors",Runtime.getRuntime().availableProcessors());json.addProperty("heapUsedBytes",Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
        json.addProperty("gpu",com.mojang.blaze3d.systems.RenderSystem.getDevice().getDeviceInfo().name());json.addProperty("graphicsBackend",com.mojang.blaze3d.systems.RenderSystem.getDevice().getDeviceInfo().backendName());json.addProperty("gpuDriver",com.mojang.blaze3d.systems.RenderSystem.getDevice().getDeviceInfo().driverInfo());json.addProperty("gpuTimeMeasured",false);json.add("hardware",gson.toJsonTree(net.nicomar.nomoregrids.diagnostic.MeasurementContext.snapshot()));json.addProperty("profileHash",net.nicomar.nomoregrids.network.SessionPayload.fingerprint());json.add("options",run.options);var mods=new java.util.TreeMap<String,String>();net.neoforged.fml.ModList.get().getMods().forEach(m->mods.put(m.getModId(),m.getVersion().toString()));json.add("mods",gson.toJsonTree(mods));var camera=new com.google.gson.JsonArray();camera.add(run.position.x);camera.add(run.position.y);camera.add(run.position.z);json.add("camera",camera);json.addProperty("yaw",run.yaw);json.addProperty("pitch",run.pitch);json.addProperty("renderDistance",mc.options.renderDistance().get());json.addProperty("simulationDistance",mc.options.simulationDistance().get());json.addProperty("maxFps",mc.options.framerateLimit().get());
        var path=mc.gameDirectory.toPath().resolve("nomoregrids/profiles/client-"+UUID.randomUUID()+".json");
        try{SectionStore.atomic(path,gson.toJson(json).getBytes(StandardCharsets.UTF_8));if(mc.player!=null)mc.player.sendSystemMessage(Component.literal("NMG frame report: "+path));}
        catch(Exception ex){net.nicomar.nomoregrids.NoMoreGrids.LOGGER.error("Cannot save frame measurement",ex);}
    }
}
