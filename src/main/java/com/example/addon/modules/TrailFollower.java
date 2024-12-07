package com.example.addon.modules;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ChunkDataEvent;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.PaletteNewChunks;

import static com.example.addon.Utils.posToYaw;

public class TrailFollower extends Module
{
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<FollowModes> chunkLoadMode = sgGeneral.add(new EnumSetting.Builder<FollowModes>()
        .name("Mode")
        .description("The mode that chunks are followed")
        .defaultValue(FollowModes.Basic)
        .build()
    );

    public final Setting<Double> rotateScaling = sgGeneral.add(new DoubleSetting.Builder()
        .name("Rotate Scaling")
        .description("Scaling of how fast the yaw changes. 1 = instant, 0 = doesn't change")
        .defaultValue(0.5)
        .build()
    );

    public TrailFollower()
    {
        super(Addon.CATEGORY, "TrailFollower", "Points you in the direction of newly loaded old chunks.");
    }

    @Override
    public void onActivate()
    {
        XaeroPlus.EVENT_BUS.register(this);
        if (mc.player != null)
        {
            goalChunkPos = mc.player.getChunkPos();
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
    }

    float targetYaw = 16.0f;

    @EventHandler
    private void onTick(TickEvent.Post event)
    {
        double dYaw = (targetYaw - mc.player.getYaw());
        dYaw = ((dYaw + 180) % 360) - 180;
        double yaw = mc.player.getYaw() + dYaw * rotateScaling.get();
        mc.player.setYaw((float) yaw);
    }

    ChunkPos goalChunkPos;


    private void basicLoad(boolean newChunk, ChunkDataEvent event, boolean seen)
    {
        if (seen) return;
        if (!newChunk &&
            mc.player.squaredDistanceTo(event.chunk().getPos().getBlockPos(0, 70, 0).toCenterPos()) > mc.player.squaredDistanceTo(goalChunkPos.getBlockPos(0, 70, 0).toCenterPos()))
        {
            // Attempt to stay in the direction of the trail by not setting goal to more than 90 degrees off
            Vec3d pos = mc.player.getPos();
            Vec3d vec1 = event.chunk().getPos().getCenterAtY((int)pos.y).toCenterPos().subtract(pos).normalize();
            Vec3d vec2 = goalChunkPos.getCenterAtY((int)pos.y).toCenterPos().subtract(pos).negate().normalize();
            double dotProduct = vec1.dotProduct(vec2);
            if (dotProduct > 0) goalChunkPos = event.chunk().getPos();
        }
        targetYaw = posToYaw(goalChunkPos.getCenterAtY(70).toCenterPos(), mc);
    }



    private void fullyLoad(boolean newChunk, ChunkDataEvent event, boolean seen)
    {
        // TODO

    }

    @net.lenni0451.lambdaevents.EventHandler(priority = -1)
    public void onChunkData(ChunkDataEvent event)
    {

        boolean isNewChunk = ModuleManager.getModule(PaletteNewChunks.class)
            .isNewChunk(
                event.chunk().getPos().x,
                event.chunk().getPos().z,
                event.chunk().getWorld().getRegistryKey()
            );

        switch (chunkLoadMode.get())
        {
            case Basic:
                basicLoad(isNewChunk, event, event.seenChunk());
                break;
            case FullyLoad:
                fullyLoad(isNewChunk, event, event.seenChunk());
                break;
        }
    }

    private enum FollowModes
    {
        Basic,
        FullyLoad,
    }

}
