package com.example.addon.modules.searcharea.modes;

import com.example.addon.modules.searcharea.SearchAreaMode;
import com.example.addon.modules.searcharea.SearchAreaModes;
import net.minecraft.util.math.BlockPos;

import java.io.*;

public class Spiral extends SearchAreaMode
{
    private PathingDataSpiral pd;
    private boolean goingToStart = true;

    public Spiral()
    {
        super(SearchAreaModes.Spiral);
    }

    @Override
    public void onActivate()
    {
        goingToStart = true;
        File file = getJsonFile(super.toString());
        if (file.exists())
        {
            try {
                FileReader reader = new FileReader(file);
                pd = GSON.fromJson(reader, PathingDataSpiral.class);
                reader.close();
            } catch (Exception ignored) {

            }
        }
        else
        {
            pd = new PathingDataSpiral(mc.player.getBlockPos(), mc.player.getBlockPos(), -90.0f, true, 0, 0);
        }
    }

    @Override
    public void onDeactivate()
    {
        super.onDeactivate();
        super.saveToJson(goingToStart, pd);

    }

    @Override
    public void onTick()
    {
        if (goingToStart)
        {
            if (Math.sqrt(mc.player.getBlockPos().getSquaredDistance(pd.currPos)) < 5)
            {
                goingToStart = false;
                mc.player.setVelocity(0, 0, 0);
            }
            else
            {
                pointTowards(pd.currPos);
                setPressed(mc.options.forwardKey, true);
            }
            return;
        }

        setPressed(mc.options.forwardKey, true);
        mc.player.setYaw(pd.yawDirection);
        int blockGap = 16 * searchArea.rowGap.get();
        if (pd.mainPath && Math.abs(mc.player.getX() - pd.initialPos.getX()) >= (blockGap + pd.spiralWidth))
        {
            pd.yawDirection += 90.0f;
            pd.initialPos = new BlockPos((int)mc.player.getX(), pd.initialPos.getY(), pd.initialPos.getZ());
            pd.spiralWidth += blockGap;
            pd.mainPath = false;
            mc.player.setVelocity(0, 0, 0);
        }
        else if (!pd.mainPath && Math.abs(mc.player.getZ() - pd.initialPos.getZ()) >= (blockGap + pd.spiralHeight))
        {
            pd.yawDirection += 90.0f;
            pd.initialPos = new BlockPos(pd.initialPos.getX(), pd.initialPos.getY(), (int)mc.player.getZ());
            pd.spiralHeight += blockGap;
            pd.mainPath = true;
            mc.player.setVelocity(0, 0, 0);
        }
    }

    public static class PathingDataSpiral extends PathingData
    {
        public int spiralWidth = 0;
        public int spiralHeight = 0;

        public PathingDataSpiral(BlockPos initialPos, BlockPos currPos, float yawDirection, boolean mainPath, int spiralWidth, int spiralHeight)
        {
            this.initialPos = initialPos;
            this.currPos = currPos;
            this.yawDirection = yawDirection;
            this.mainPath = mainPath;
            this.spiralWidth = spiralWidth;
            this.spiralHeight = spiralHeight;
        }
    }
}
