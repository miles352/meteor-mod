package com.example.addon.modules;

import com.example.addon.Addon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;

import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import static com.example.addon.Utils.sendWebhook;


public class HighlightOldLava extends Module
{
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> searchAbove = sgGeneral.add(new IntSetting.Builder()
        .name("Search Above")
        .description("Y value to look above")
        .range(0, 120)
        .build()
    );

    private final Setting<Integer> lavaHeight = sgGeneral.add(new IntSetting.Builder()
        .name("Lava Height")
        .description("The height of the lava to count as already loaded")
        .range(1, 30)
        .build()
    );

    private final Setting<Integer> renderDistance = sgGeneral.add(new IntSetting.Builder()
        .name("Render Distance")
        .description("How far away to render the blocks.")
        .range(0, 512)
        .build()
    );

    private final Setting<Boolean> disconnectOnFound = sgGeneral.add(new BoolSetting.Builder()
        .name("Disconnect on Found")
        .description("Will auto disconnect you if old lava is found. (Good for afking)")
        .defaultValue(false)
        .build()
    );

    public final Setting<Mode> logMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("Log Mode")
        .description("How results are shown.")
        .defaultValue(Mode.Highlight)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("Box Mode")
        .description("How the shape for the bounding box is rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Side Color")
        .description("The side color of the bounding box.")
        .defaultValue(new SettingColor(16,106,144, 100))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Line Color")
        .description("The line color of the bounding box.")
        .defaultValue(new SettingColor(16,106,144, 255))
        .build()
    );

    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private HashSet<BlockPos> oldLava;

    private HashSet<Vec3d> loadedChunks;

    public HighlightOldLava()
    {
        super(Addon.CATEGORY, "HighlightOldLava", "Highlights lava that has already flowed down");
        oldLava = new HashSet<BlockPos>();
        loadedChunks = new HashSet<Vec3d>();
    }

    @Override
    public WWidget getWidget(GuiTheme theme)
    {
        WVerticalList list = theme.verticalList();
        WButton clearLog = list.add(theme.button("Clear saved data.")).widget();

        clearLog.action = () -> {
            File lavaPosFile = new File(new File(new File(MeteorClient.FOLDER, "HighlightOldLava"), Utils.getFileWorldName()),  "lavaPos.json");
            File loadedChunksFile = new File(new File(new File(MeteorClient.FOLDER, "HighlightOldLava"), Utils.getFileWorldName()),  "loadedChunks.json");

            lavaPosFile.delete();
            loadedChunksFile.delete();
        };

        return list;
    }

    @Override
    public void onActivate()
    {
        oldLava = new HashSet<BlockPos>();
        loadedChunks = new HashSet<Vec3d>();


        try {
            File lavaPosFile = new File(new File(new File(MeteorClient.FOLDER, "HighlightOldLava"), Utils.getFileWorldName()),  "lavaPos.json");
            FileReader reader = new FileReader(lavaPosFile);
            oldLava = GSON.fromJson(reader, new TypeToken<HashSet<BlockPos>>(){}.getType());
            reader.close();

            File loadedChunksFile = new File(new File(new File(MeteorClient.FOLDER, "HighlightOldLava"), Utils.getFileWorldName()),  "loadedChunks.json");
            FileReader reader2 = new FileReader(loadedChunksFile);
            oldLava = GSON.fromJson(reader, new TypeToken<HashSet<Vec3d>>(){}.getType());
            reader2.close();
        } catch (Exception ignored) {

        }
    }

    @Override
    public void onDeactivate()
    {
        try
        {
            File lavaPosFile = new File(new File(new File(MeteorClient.FOLDER, "HighlightOldLava"), Utils.getFileWorldName()),  "lavaPos.json");
            lavaPosFile.getParentFile().mkdirs();
            Writer writer = new FileWriter(lavaPosFile);
            GSON.toJson(oldLava, writer);
            writer.close();

            File loadedChunksFile = new File(new File(new File(MeteorClient.FOLDER, "HighlightOldLava"), Utils.getFileWorldName()),  "loadedChunks.json");
            loadedChunksFile.getParentFile().mkdirs();
            Writer writer2 = new FileWriter(loadedChunksFile);
            GSON.toJson(loadedChunks, writer2);
            writer2.close();
        }
        catch (NullPointerException | IOException e) {

        }
    }


    @EventHandler
    private void onRender(Render3DEvent event) {
        if ((logMode.get() == Mode.Highlight) || (logMode.get() == Mode.Both)) {
            for (BlockPos blockPos : oldLava) {
                if (Math.sqrt(mc.player.squaredDistanceTo(blockPos.toCenterPos())) <= renderDistance.get()) {
                    RenderUtils.renderTickingBlock(blockPos.toImmutable(), sideColor.get(), lineColor.get(), shapeMode.get(), 0, 8, true, false);
                }
            }
        }
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event)
    {
        Chunk chunk = event.chunk();
        Vec3d chunkPos = chunk.getPos().getStartPos().toCenterPos();
        // don't check chunks loaded by player
        if (loadedChunks.contains(chunkPos)) return;
        loadedChunks.add(chunkPos);
        HashSet<BlockPos> toAdd = new HashSet<BlockPos>();
        boolean chunkLogged = false;
        for (int x = chunk.getPos().getStartX(); x <= chunk.getPos().getEndX(); x++)
        {
            for (int z = chunk.getPos().getStartZ(); z <= chunk.getPos().getEndZ(); z++)
            {
                int height = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE).get(x - chunk.getPos().getStartX(), z - chunk.getPos().getStartZ());

//                for (int y = searchAbove.get() + 1; y < height; y++)
                for (int y = height; y > searchAbove.get(); y--)
                {
                    BlockPos blockPos = new BlockPos(x, y, z);

                    BlockState blockState = chunk.getBlockState(blockPos);
                    // try to prevent bastions or nether fortresses being detected
                    if (blockState.getBlock() == Blocks.POLISHED_BLACKSTONE_BRICKS ||
                        blockState.getBlock() == Blocks.NETHER_BRICKS) return;


                    if (blockState.getBlock() == Blocks.LAVA)
                    {
                        boolean heightFound = true;
                        for (int i = 1; i < lavaHeight.get(); i++)
                        {
                            if (chunk.getBlockState(blockPos.add(0, i, 0)).getBlock() != Blocks.LAVA)
                            {
                                heightFound = false;
                                break;
                            }
                        }
                        if (heightFound)
                        {
                            if (logMode.get() == Mode.LogWebhook || logMode.get() == Mode.Both)
                            {
                                sendWebhook("https://discord.com/api/webhooks/1231507654841729034/KGDjnmYt9pGepy1o7NPuSiEQ0v8Qj6WIalJQJSsPRGANlyuL0WcJO4adrkfeFSaRCDMw", "Old Chunk Found", "At: " + blockPos.getX() + " " + blockPos.getZ(), "769415977439592468", mc.player.getGameProfile().getName());
                            }
                            if (disconnectOnFound.get()) mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("[HighlightOldLava] Old lava was found.")));
                            oldLava.add(blockPos);
                            return;
                        }
                    }


                }
            }
        }
//        oldLava.addAll(toAdd);
    }


    public enum Mode
    {
        Highlight,
        LogWebhook,
        Both
    }
}
