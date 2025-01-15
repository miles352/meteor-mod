package com.example.addon.modules;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalXZ;
import com.example.addon.Addon;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.WorldChunk;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ChunkDataEvent;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.OldChunks;
import xaeroplus.module.impl.PaletteNewChunks;
import xaeroplus.util.ChunkScanner;
import xaeroplus.util.ChunkUtils;

import java.util.ArrayDeque;

public class TrailFollower extends Module
{
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // TODO: Set this automatically either by looking at the rate of chunk loads or by using yaw instead of block pos so size doesnt negatively effect result
    public final Setting<Integer> maxTrailLength = sgGeneral.add(new IntSetting.Builder()
        .name("Max Trail Length")
        .description("The number of trail points to keep for the average. Adjust to change how quickly the average will change. More does not necessarily equal better because if the list is too long it will contain chunks behind you.")
        .defaultValue(20)
        .sliderRange(1, 100)
        .build()
    );

    public final Setting<Integer> chunksBeforeStarting = sgGeneral.add(new IntSetting.Builder()
        .name("Chunks Before Starting")
        .description("Useful for afking looking for a trail. The amount of chunks before it gets detected as a trail.")
        .defaultValue(10)
        .sliderRange(1, 50)
        .build()
    );

    public final Setting<Integer> chunkConsiderationWindow = sgGeneral.add(new IntSetting.Builder()
        .name("Chunk Timeframe")
        .description("The amount of time in seconds that the chunks must be found in before starting.")
        .defaultValue(5)
        .sliderRange(1, 20)
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
        .defaultValue(0.1)
        .sliderRange(0.0, 1.0)
        .build()
    );

    public final Setting<Boolean> autoElytra = sgGeneral.add(new BoolSetting.Builder()
        .name("[Baritone] Auto Start Baritone Elytra")
        .description("Starts baritone elytra for you.")
        .defaultValue(false)
        .build()
    );

    private final SettingGroup sgAdvanced = settings.createGroup("Advanced", false);

    public final Setting<Double> pathDistance = sgAdvanced.add(new DoubleSetting.Builder()
        .name("Path Distance")
        .description("The distance to add trail positions in the direction the player is facing.")
        .defaultValue(500)
        .sliderRange(100, 2000)
        .build()
    );

    public final Setting<Double> startDirectionWeighting = sgAdvanced.add(new DoubleSetting.Builder()
        .name("Start Direction Weight")
        .description("The weighting of the direction the player is facing when starting the trail. 0 for no weighting (not recommended) 1 for max weighting (will take a bit for direction to change)")
        .defaultValue(0.5)
        .min(0)
        .sliderMax(1)
        .build()
    );

    public final Setting<DirectionWeighting> directionWeighting = sgAdvanced.add(new EnumSetting.Builder<DirectionWeighting>()
        .name("Direction Weighting")
        .description("How the chunks found should be weighted. Useful for path splits. Left will weight chunks to the left of the player higher, right will weigh chunks to the right higher, and none will be in the middle/random. ")
        .defaultValue(DirectionWeighting.NONE)
        .build()
    );

    public final Setting<Integer> directionWeightingMultiplier = sgAdvanced.add(new IntSetting.Builder()
        .name("Direction Weighting Multiplier")
        .description("The multiplier for how much weight should be given to chunks in the direction specified. Values are capped to be in the range [2, maxTrailLength].")
        .defaultValue(2)
        .min(2)
        .sliderMax(10)
        .visible(() -> directionWeighting.get() != DirectionWeighting.NONE)
        .build()
    );

    public final Setting<Double> chunkFoundTimeout = sgAdvanced.add(new DoubleSetting.Builder()
        .name("Chunk Found Timeout")
        .description("The amount of MS without a chunk found to trigger circling.")
        .defaultValue(1000 * 5)
        .min(1000)
        .sliderMax(1000 * 10)
        .build()
    );

    public final Setting<Double> circlingDegPerTick = sgAdvanced.add(new DoubleSetting.Builder()
        .name("Circling Degrees Per Tick")
        .description("The amount of degrees to change per tick while circling.")
        .defaultValue(2.0)
        .min(1.0)
        .sliderMax(20.0)
        .build()
    );

    public final Setting<Double> trailTimeout = sgAdvanced.add(new DoubleSetting.Builder()
        .name("Trail Timeout")
        .description("The amount of MS without a chunk found to stop following the trail.")
        .defaultValue(1000 * 30)
        .min(1000 * 10)
        .sliderMax(1000 * 60)
        .build()
    );

    public final Setting<Integer> baritoneUpdateTicks = sgAdvanced.add(new IntSetting.Builder()
        .name("[Baritone] Baritone Path Update Ticks")
        .description("The amount of ticks between updates to the baritone goal. Low values may cause high instability.")
        .defaultValue(5 * 20) // 5 seconds
        .sliderRange(20, 30 * 20)
        .build()
    );

    // TODO: Auto disconnect at certain chunk load speed

    private boolean oldAutoFireworkValue;

    private FollowMode followMode;

    private boolean followingTrail = false;

    private ArrayDeque<Vec3d> trail = new ArrayDeque<>();
    private ArrayDeque<Vec3d> possibleTrail = new ArrayDeque<>();

    private long lastFoundTrailTime;
    private long lastFoundPossibleTrailTime;

    // Credit to WarriorLost: https://github.com/WarriorLost/meteor-client/tree/master

    public TrailFollower()
    {
        super(Addon.CATEGORY, "TrailFollower", "Automatically follows trails in all dimensions.");
    }

    void resetTrail()
    {
        baritoneSetGoalTicks = 0;
        followingTrail = false;
        trail = new ArrayDeque<>();
        possibleTrail = new ArrayDeque<>();
    }

    @Override
    public void onActivate()
    {
        resetTrail();
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
            Vec3d targetPos = mc.player.getPos().add(offset);
            for (int i = 0; i < (maxTrailLength.get() * startDirectionWeighting.get()); i++)
            {
                trail.add(targetPos);
            }
            targetYaw = getActualYaw(mc.player.getYaw());
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

    private double targetYaw;

    private int baritoneSetGoalTicks = 0;

    private void circle()
    {
        if (followMode == FollowMode.BARITONE && baritoneSetGoalTicks == 0)
        {
            baritoneSetGoalTicks = baritoneUpdateTicks.get();
        }
        else if (followMode == FollowMode.BARITONE) return;
        targetYaw = getActualYaw((float) (targetYaw + circlingDegPerTick.get()));
        if (mc.player.age % 100 == 0)
        {
            info("Circling to look for new chunks, abandoning trail in " + (trailTimeout.get() - (System.currentTimeMillis() - lastFoundTrailTime)) / 1000 + " seconds.");
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event)
    {
        if (mc.player == null || mc.world == null) return;
        if (followingTrail && System.currentTimeMillis() - lastFoundTrailTime > trailTimeout.get())
        {
            resetTrail();
            info("Trail timed out, stopping.");
            // TODO: Add options for what to do next
        }
        if (followingTrail && System.currentTimeMillis() - lastFoundTrailTime > chunkFoundTimeout.get()) circle();
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
                    Vec3d targetPos = positionInDirection(mc.player.getPos(), targetYaw, pathDistance.get());
                    BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ((int) targetPos.x, (int) targetPos.z));
                    if (autoElytra.get() && BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess().currentDestination() == null)
                    {
                        // TODO: Fix this
                        info("The auto elytra mode is broken right now. If it's not working just turn it off and manually use #elytra to start.");
                        BaritoneAPI.getSettings().elytraTermsAccepted.value = true;
                        BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("elytra");
                    }
                }
                break;
            }
            case YAWLOCK: {
                mc.player.setYaw(smoothRotation(getActualYaw(mc.player.getYaw()), targetYaw));
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

        // useless because old chunks are stored asynchrnously and will not be ready inside the chunk event
//        boolean is112OldChunk = ModuleManager.getModule(OldChunks.class)
//            .isOldChunk(
//                chunk.getPos().x,
//                chunk.getPos().z,
//                chunk.getWorld().getRegistryKey()
//            );


        // TODO: Find a better way to do this bc Xaero is already checking the chunk
        boolean is112OldChunk = false;
        ReferenceSet<Block> OVERWORLD_BLOCKS = ReferenceOpenHashSet.of(new Block[]{Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.AMETHYST_BLOCK, Blocks.SMOOTH_BASALT, Blocks.TUFF, Blocks.KELP, Blocks.KELP_PLANT, Blocks.POINTED_DRIPSTONE, Blocks.DRIPSTONE_BLOCK, Blocks.DEEPSLATE, Blocks.AZALEA, Blocks.BIG_DRIPLEAF, Blocks.BIG_DRIPLEAF_STEM, Blocks.SMALL_DRIPLEAF, Blocks.MOSS_BLOCK, Blocks.CAVE_VINES, Blocks.CAVE_VINES_PLANT});
        ReferenceSet<Block> NETHER_BLOCKS = ReferenceOpenHashSet.of(new Block[]{Blocks.ANCIENT_DEBRIS, Blocks.BLACKSTONE, Blocks.BASALT, Blocks.CRIMSON_NYLIUM, Blocks.WARPED_NYLIUM, Blocks.NETHER_GOLD_ORE, Blocks.CHAIN});
        // In the end
        if (!mc.world.getDimension().hasCeiling() && !mc.world.getDimension().bedWorks())
        {
            RegistryEntry<Biome> biomeHolder = this.mc.world.getBiome(new BlockPos(ChunkUtils.chunkCoordToCoord(chunk.getPos().x) + 8, 64, ChunkUtils.chunkCoordToCoord(chunk.getPos().z) + 8));
            if (biomeHolder.getKey().filter((biome) -> biome.equals(BiomeKeys.THE_END)).isPresent()) is112OldChunk = true;
        }
        else
        {
            // Not in the end
            is112OldChunk = !ChunkScanner.chunkContainsBlocks(chunk, !mc.world.getDimension().hasCeiling() ? OVERWORLD_BLOCKS : NETHER_BLOCKS, 5);
        }

        // TODO: Add options for following certain types of chunks.

        if (!is119NewChunk || is112OldChunk)
        {
            Vec3d pos = chunk.getPos().getCenterAtY(0).toCenterPos();

            if (!followingTrail)
            {
                if (System.currentTimeMillis() - lastFoundPossibleTrailTime > chunkConsiderationWindow.get() * 1000)
                {
                    possibleTrail.clear();
                }
                possibleTrail.add(pos);
                lastFoundPossibleTrailTime = System.currentTimeMillis();
                if (possibleTrail.size() > chunksBeforeStarting.get())
                {
                    followingTrail = true;
                    lastFoundTrailTime = System.currentTimeMillis();
                    trail.addAll(possibleTrail);
                    possibleTrail.clear();
                }
                return;
            }

            // TODO: Fix bug where trail instantly times out after starting real trail


            // add chunks to the list

            double chunkAngle = Rotations.getYaw(pos);
            double angleDiff = angleDifference(targetYaw, chunkAngle);

            // Ignore chunks not in your direction
            if (Math.abs(angleDiff) > 90) return;
            lastFoundTrailTime = System.currentTimeMillis();

            // free up one spot for a new chunk to be added
            while(trail.size() >= maxTrailLength.get())
            {
                trail.pollFirst();
            }

            if (angleDiff > 0 && angleDiff < 90 && directionWeighting.get() == DirectionWeighting.LEFT)
            {
                // add extra chunks to increase the weighting
                // TODO: Maybe redo this to use a map of chunk pos to weights
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

            Vec3d targetPos = mc.player.getPos().add(positionVec.multiply(pathDistance.get()));
            targetYaw = Rotations.getYaw(targetPos);
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

    private float getActualYaw(float yaw)
    {
        return (yaw % 360 + 360) % 360;
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

    private Vec3d positionInDirection(Vec3d pos, double yaw, double distance)
    {
        Vec3d offset = (new Vec3d(Math.sin(-yaw * Math.PI / 180), 0, Math.cos(-yaw * Math.PI / 180)).normalize()).multiply(distance);
        return pos.add(offset);
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
