package com.example.addon.modules.searcharea.modes;

import com.example.addon.modules.searcharea.SearchAreaMode;
import com.example.addon.modules.searcharea.SearchAreaModes;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import meteordevelopment.meteorclient.systems.modules.movement.BoatFly;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.io.*;

import static meteordevelopment.meteorclient.utils.player.ChatUtils.info;
import static com.example.addon.Utils.*;

public class Rectangle extends SearchAreaMode
{

    private PathingDataRectangle pd;
    private boolean goingToStart = true;
    private long startTime;

    public Rectangle() {
        super(SearchAreaModes.Rectangle);
    }

    @Override
    public void onActivate()
    {
        if (!searchArea.saveLocation.get().isBlank())
        {
            goingToStart = true;
            File file = getJsonFile(super.toString());
            if (file == null || !file.exists())
            {
                // set currPos to startpos if it is not read from file, so that the bot travels to the startpoint and not where the player currently is
                pd = new PathingDataRectangle(searchArea.startPos.get(), searchArea.targetPos.get(), searchArea.startPos.get(), 90, true, (int)mc.player.getZ());
            }
            else
            {
                try {
                    FileReader reader = new FileReader(file);
                    pd = GSON.fromJson(reader, PathingDataRectangle.class);
                    reader.close();
                } catch (Exception ignored) {

                }
            }
        }
    }

    @Override
    public void onDeactivate()
    {
        super.onDeactivate();
        super.saveToJson(goingToStart, pd);
    }

    private void printRectangleEstimate()
    {
        Class<? extends Module> boatFly = BoatFly.class;
        Module module = Modules.get().get(boatFly);
        double speedBPS = (double)module.settings.get("speed").get();
        double rowDistance = Math.abs(pd.initialPos.getX() - pd.targetPos.getX());
        int rowCount = Math.abs(pd.currPos.getZ() - pd.targetPos.getZ()) / 16 / searchArea.rowGap.get();
        double totalBlocks = rowCount * (rowDistance + (searchArea.rowGap.get() * 16));
        long totalSeconds = (long)(totalBlocks / speedBPS);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        info("Completion will take an estimated %02d hours %02d minutes %02d seconds with boatfly at a speed of %.2f and a gap of %d chunks between paths.", hours, minutes, seconds, speedBPS, searchArea.rowGap.get());
    }

    @Override
    public void onTick()
    {
        // autosave every 10 minutes in case of crashes
        if (System.nanoTime() - startTime > 6e11)
        {
            startTime = System.nanoTime();
            super.saveToJson(goingToStart, pd);
        }

        if (goingToStart)
        {
            if (Math.sqrt(mc.player.getBlockPos().getSquaredDistance(pd.currPos.getX(), mc.player.getY(), pd.currPos.getZ())) < 5)
            {
                goingToStart = false;
                mc.player.setVelocity(0, 0, 0);
                printRectangleEstimate();
            }
            else
            {
                mc.player.setYaw((float) Rotations.getYaw(pd.currPos.toCenterPos()));
                setPressed(mc.options.forwardKey, true);
            }
            return;
        }

        setPressed(mc.options.forwardKey, true);
        mc.player.setYaw(pd.yawDirection);
        if (Math.sqrt(mc.player.getBlockPos().getSquaredDistance(pd.targetPos.getX(), mc.player.getY(), pd.targetPos.getZ())) < 20) // end of rectangle
        {
            setPressed(mc.options.forwardKey, false);
//            path complete
            searchArea.toggle();
            if (searchArea.disconnectOnCompletion.get())
            {
                var autoReconnect = Modules.get().get(AutoReconnect.class);
                if (autoReconnect.isActive()) autoReconnect.toggle();
                mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("[Search Area] Path is complete")));
            }
        }//                                                                      if going in +X and currPos > the greater of the two sides of the rectangle
        else if (pd.mainPath && ((pd.yawDirection == -90.0f && mc.player.getX() >= (Math.max(pd.initialPos.getX(), pd.targetPos.getX())))) ||
            (pd.yawDirection == 90.0f && mc.player.getX() <= (Math.min(pd.initialPos.getX(), pd.targetPos.getX())))) // if at the end of a normal path
        {
            pd.yawDirection = (mc.player.getZ() < pd.targetPos.getZ()) ? 0.0f : 180.0f;
            pd.mainPath = false;
            mc.player.setVelocity(0, 0, 0);
        }
        else if (!pd.mainPath && Math.abs(mc.player.getZ() - pd.lastCompleteRowZ) >= (16 * searchArea.rowGap.get())) // if the path to go past loaded chunks is done
        {
            pd.lastCompleteRowZ = (int)mc.player.getZ();
            pd.yawDirection = (pd.initialPos.getX() > mc.player.getX() ? -90.0f : 90.0f);
            pd.mainPath = true;
            mc.player.setVelocity(0, 0, 0);
        }
    }

    public static class PathingDataRectangle extends PathingData
    {
        public BlockPos targetPos;
        public int lastCompleteRowZ;

        public PathingDataRectangle(BlockPos initialPos, BlockPos targetPos, BlockPos currPos, float yawDirection, boolean mainPath, int lastCompleteRowZ)
        {
            this.initialPos = initialPos;
            this.targetPos = targetPos;
            this.currPos = currPos;
            this.yawDirection = yawDirection;
            this.mainPath = mainPath;
            this.lastCompleteRowZ = lastCompleteRowZ;
        }
    }
}
