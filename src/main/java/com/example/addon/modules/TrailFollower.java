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
import java.util.List;

public class TrailFollower extends Module
{
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Integer> maxTrailLength = sgGeneral.add(new IntSetting.Builder()
        .name("Max Trail Length")
        .description("The number of trail points to keep for the average. Adjust to change how quickly the average will change.")
        .defaultValue(20)
        .min(1)
        .sliderMax(100)
        .build()
    );

    public final Setting<Boolean> pitch40 = sgGeneral.add(new BoolSetting.Builder()
        .name("Auto Pitch 40")
        .description("Incorporates pitch 40 into the follower.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> pitch40Firework = sgGeneral.add(new BoolSetting.Builder()
        .name("Auto Firework")
        .description("Uses a firework automatically if your velocity is too low.")
        .defaultValue(true)
        .visible(() -> pitch40.get())
        .build()
    );

    public final Setting<Double> rotateScaling = sgGeneral.add(new DoubleSetting.Builder()
        .name("Rotate Scaling")
        .description("Scaling of how fast the yaw changes. 1 = instant, 0 = doesn't change")
        .defaultValue(0.5)
        .build()
    );

    public final Setting<Double> pathDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("Path Distance")
        .description("The distance to add trail positions in the direction the player is facing.")
        .defaultValue(500)
        .min(10)
        .sliderMax(1000)
        .build()
    );

    public final Setting<Double> startDirectionWeighting = sgGeneral.add(new DoubleSetting.Builder()
        .name("Start Direction Weight")
        .description("The weighting of the direction the player is facing when starting the trail. 0 for no weighting (not recommended) 1 for max weighting (will take a bit for direction to change)")
        .defaultValue(0.5)
        .min(0)
        .sliderMax(1)
        .build()
    );

    public final Setting<DirectionWeighting> directionWeighting = sgGeneral.add(new EnumSetting.Builder<DirectionWeighting>()
        .name("Direction Weighting")
        .description("How the chunks found should be weighted. Useful for path splits. Left will weight chunks to the left of the player higher, right will weigh chunks to the right higher, and none will be in the middle/random. ")
        .defaultValue(DirectionWeighting.NONE)
        .build()
    );

    public final Setting<Integer> directionWeightingMultiplier = sgGeneral.add(new IntSetting.Builder()
        .name("Direction Weighting Multiplier")
        .description("The multiplier for how much weight should be given to chunks in the direction specified. Values are capped to be in the range [2, maxTrailLength].")
        .defaultValue(2)
        .min(2)
        .sliderMax(10)
        .visible(() -> directionWeighting.get() != DirectionWeighting.NONE)
        .build()
    );

    public final Setting<Double> chunkFoundTimeout = sgGeneral.add(new DoubleSetting.Builder()
        .name("Chunk Found Timeout")
        .description("The amount of MS without a chunk found to trigger circling.")
        .defaultValue(1000 * 5)
        .min(1000)
        .sliderMax(1000 * 10)
        .build()
    );

    public final Setting<Boolean> autoElytra = sgGeneral.add(new BoolSetting.Builder()
        .name("[Baritone] Auto Start Baritone Elytra")
        .description("Starts baritone elytra for you.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Integer> baritoneUpdateTicks = sgGeneral.add(new IntSetting.Builder()
        .name("[Baritone] Baritone Path Update Ticks")
        .description("The amount of ticks between updates to the baritone goal. Low values may cause high instability.")
        .defaultValue(5 * 20) // 5 seconds
        .sliderRange(20, 30 * 20)
        .build()
    );

    private boolean oldAutoFireworkValue;
    private FollowMode followMode;
    private ArrayDeque<Vec3d> trail = new ArrayDeque<>();
    private long lastFoundTime;

    // Credit to WarriorLost: https://github.com/WarriorLost/meteor-client/tree/master

    public TrailFollower()
    {
        super(Addon.CATEGORY, "TrailFollower", "Automatically follows trails in all dimensions.");
    }

    @Override
    public void onActivate()
    {
        baritoneSetGoalTicks = 0;
        XaeroPlus.EVENT_BUS.register(this);
        if (mc.player != null && mc.world != null)
        {
            if (!mc.world.getDimension().hasCeiling())
            {
                followMode = FollowMode.YAWLOCK;
                info("You are in the overworld or end, basic yaw mode will be used.");
            }
            else
            {
                followMode = FollowMode.BARITONE;
                info("You are in the nether, baritone mode will be used.");
            }

            if (followMode == FollowMode.YAWLOCK)
            {
                Class<? extends Module> pitch40Util = Pitch40Util.class;
                Module pitch40UtilModule = Modules.get().get(pitch40Util);
                if (pitch40.get() && !pitch40UtilModule.isActive())
                {
                    pitch40UtilModule.toggle();
                    if (pitch40Firework.get())
                    {
                        Setting<Boolean> setting = ((Setting<Boolean>)pitch40UtilModule.settings.get("Auto Firework"));
                        info("Auto Firework enabled, if you want to change the velocity threshold or the firework cooldown check the settings under Pitch40Util.");
                        oldAutoFireworkValue = setting.get();
                        setting.set(true);
                    }
                }
            }
            // set original pos to pathDistance blocks in the direction the player is facing
            Vec3d offset = (new Vec3d(Math.sin(-mc.player.getYaw() * Math.PI / 180), 0, Math.cos(-mc.player.getYaw() * Math.PI / 180)).normalize()).multiply(pathDistance.get());
            targetPos = mc.player.getPos().add(offset);
            for (int i = 0; i < (maxTrailLength.get() * startDirectionWeighting.get()); i++)
            {
                trail.add(targetPos);
            }
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
        switch (followMode)
        {
            case BARITONE:
            {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("cancel");
                break;
            }
            case YAWLOCK:
            {
                Class<? extends Module> pitch40Util = Pitch40Util.class;
                Module pitch40UtilModule = Modules.get().get(pitch40Util);
                if (pitch40.get() && pitch40UtilModule.isActive())
                {
                    pitch40UtilModule.toggle();
                }
                ((Setting<Boolean>)pitch40UtilModule.settings.get("Auto Firework")).set(oldAutoFireworkValue);
            }
        }
        trail.clear();
    }

    private Vec3d targetPos;

    private int baritoneSetGoalTicks = 0;

    private void circle()
    {
        if (followMode == FollowMode.BARITONE && baritoneSetGoalTicks == 0)
        {
            baritoneSetGoalTicks = baritoneUpdateTicks.get();
        }
        else if (followMode == FollowMode.BARITONE) return;
        double angle = (mc.player.age % 360) * Math.PI / 180; // Convert age to radians
        targetPos = new Vec3d(Math.cos(angle), 0, Math.sin(angle)).normalize();
    }

    @EventHandler
    private void onTick(TickEvent.Post event)
    {
        if (mc.player == null || mc.world == null) return;
        if (System.currentTimeMillis() - lastFoundTime > chunkFoundTimeout.get())
        {
            circle();
            return;
        }
        switch (followMode)
        {
            case BARITONE:
            {
                if (baritoneSetGoalTicks > 0)
                {
                    baritoneSetGoalTicks--;
                }
                else if (baritoneSetGoalTicks == 0)
                {
                    baritoneSetGoalTicks = baritoneUpdateTicks.get();
                    BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ((int) targetPos.x, (int) targetPos.z));
                    if (autoElytra.get() && BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess().currentDestination() == null)
                    {
                        // TODO: Fix this
                        BaritoneAPI.getSettings().elytraTermsAccepted.value = true;
                        BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("elytra");
                    }
                }
                break;
            }
            case YAWLOCK: {
                mc.player.setYaw(smoothRotation(mc.player.getYaw(), Rotations.getYaw(targetPos)));
                break;
            }
        }

    }

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

        // TODO: Add options for following certain types of chunks.
        if (!is119NewChunk)
        {
            lastFoundTime = System.currentTimeMillis();
            // add chunks to the list
            Vec3d pos = chunk.getPos().getCenterAtY(0).toCenterPos();
            while(trail.size() >= maxTrailLength.get())
            {
                trail.pollFirst();
            }

            double trailAngle = Rotations.getYaw(targetPos);
            double chunkAngle = Rotations.getYaw(pos);
            double angleDiff = angleDifference(trailAngle, chunkAngle);

            if (angleDiff > 0 && angleDiff < 90 && directionWeighting.get() == DirectionWeighting.LEFT)
            {
                for (int i = 0; i < directionWeightingMultiplier.get() - 1; i++)
                {
                    trail.pollFirst();
                    trail.add(pos);
                }
                trail.add(pos);
            }
            else if (angleDiff < 0 && angleDiff > -90 && directionWeighting.get() == DirectionWeighting.RIGHT)
            {
                for (int i = 0; i < directionWeightingMultiplier.get() - 1; i++)
                {
                    trail.pollFirst();
                    trail.add(pos);
                }
                trail.add(pos);
            }
            else
            {
                trail.add(pos);
            }


            // get average pos
            Vec3d averagePos = calculateAveragePosition(trail);

            Vec3d positionVec = averagePos.subtract(mc.player.getPos()).normalize();

            targetPos = mc.player.getPos().add(positionVec.multiply(pathDistance.get()));
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

    private enum FollowMode
    {
        BARITONE,
        YAWLOCK
    }

    public enum DirectionWeighting
    {
        LEFT,
        NONE,
        RIGHT
    }

}
