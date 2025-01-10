package com.example.addon.modules;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalXZ;
import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ChunkDataEvent;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.OldChunks;
import xaeroplus.module.impl.PaletteNewChunks;

import java.util.ArrayDeque;

public class TrailFollower extends Module
{
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> pitch40 = sgGeneral.add(new BoolSetting.Builder()
        .name("Auto Pitch 40")
        .description("Incorporates pitch 40 into the follower.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Integer> maxTrailLength = sgGeneral.add(new IntSetting.Builder()
        .name("Max Trail Length")
        .description("The number of trail points to keep for the average. Adjust to change how quickly the average will change.")
        .defaultValue(20)
        .min(1)
        .sliderMax(100)
        .build()
    );

    public final Setting<Boolean> pitch40Firework = sgGeneral.add(new BoolSetting.Builder()
        .name("Auto Firework")
        .description("Uses a firework automatically if your velocity is too low.")
        .defaultValue(true)
        .visible(pitch40::get)
        .build()
    );

    public final Setting<Double> rotateScaling = sgGeneral.add(new DoubleSetting.Builder()
        .name("Rotate Scaling")
        .description("Scaling of how fast the yaw changes. 1 = instant, 0 = doesn't change")
        .defaultValue(0.5)
        .build()
    );

    private boolean oldAutoFireworkValue;

    // Credit to WarriorLost: https://github.com/WarriorLost/meteor-client/tree/master

    public TrailFollower()
    {
        super(Addon.CATEGORY, "TrailFollower", "Automatically follows trails in all dimensions.");
    }

    @Override
    public void onActivate()
    {
        XaeroPlus.EVENT_BUS.register(this);
        if (mc.player != null)
        {
            Class<? extends Module> pitch40Util = Pitch40Util.class;
            Module pitch40UtilModule = Modules.get().get(pitch40Util);
            if (pitch40.get() && !pitch40UtilModule.isActive())
            {
                pitch40UtilModule.toggle();
                if (pitch40Firework.get())
                {
                    Setting<Boolean> setting = ((Setting<Boolean>)pitch40UtilModule.settings.get("Auto Firework"));
                    oldAutoFireworkValue = setting.get();
                    setting.set(true);
                }
            }

            targetYaw = mc.player.getYaw();
        }
        else
        {
            this.toggle();
        }
    }

    @Override
    public void onDeactivate()
    {
        XaeroPlus.EVENT_BUS.unregister(this);
        Class<? extends Module> pitch40Util = Pitch40Util.class;
        Module pitch40UtilModule = Modules.get().get(pitch40Util);
        if (pitch40.get() && pitch40UtilModule.isActive())
        {
            pitch40UtilModule.toggle();
        }
        ((Setting<Boolean>)pitch40UtilModule.settings.get("Auto Firework")).set(oldAutoFireworkValue);
    }

    float targetYaw;

    @EventHandler
    private void onTick(TickEvent.Post event)
    {
        mc.player.setYaw(smoothRotation(mc.player.getYaw(), targetYaw));
    }

    ArrayDeque<Vec3d> trail = new ArrayDeque<>();

    @net.lenni0451.lambdaevents.EventHandler(priority = -1)
    public void onChunkData(ChunkDataEvent event)
    {
        if (event.seenChunk()) return;
        WorldChunk chunk = event.chunk();
        boolean is119NewChunk = ModuleManager.getModule(PaletteNewChunks.class)
            .isNewChunk(
                chunk.getPos().x,
                chunk.getPos().z,
                chunk.getWorld().getRegistryKey()
            );

        boolean is112OldChunk = ModuleManager.getModule(OldChunks.class)
            .isOldChunk(
                chunk.getPos().x,
                chunk.getPos().z,
                chunk.getWorld().getRegistryKey()
            );

        if (!is119NewChunk)
        {
            // add chunks to the list
            Vec3d pos = chunk.getPos().getCenterAtY(0).toCenterPos();
            while(trail.size() >= maxTrailLength.get())
            {
                trail.pollFirst();
            }
            trail.add(pos);

            // get average pos
            Vec3d averagePos = calculateAveragePosition(trail);

            // set yaw to face average pos
            targetYaw = (float) Rotations.getYaw(averagePos);
        }
    }

    private Vec3d calculateAveragePosition(ArrayDeque<Vec3d> positions)
    {
        double sumX = 0, sumZ = 0;
        for (Vec3d pos : positions) {
            sumX += pos.x;
            sumZ += pos.z;
        }
        return new Vec3d(sumX / positions.size(), 0, sumZ / positions.size());
    }

    private float smoothRotation(double current, double target)
    {
        double difference = angleDifference(target, current);
        return (float) (current + difference * rotateScaling.get());
    }

    private double angleDifference(double target, double current)
    {
        double diff = (target - current + 180) % 360 - 180;
        return diff < -180 ? diff + 360 : diff;
    }

}
