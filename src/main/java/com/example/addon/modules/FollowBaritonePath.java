package com.example.addon.modules;

import com.example.addon.Addon;
import com.example.addon.modules.searcharea.SearchArea;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.io.*;
import java.util.ArrayList;

import static com.example.addon.Utils.sendWebhook;
import static java.lang.System.currentTimeMillis;

public class FollowBaritonePath extends Module
{
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> pathName = sgGeneral.add(new StringSetting.Builder()
        .name("Path Name")
        .description("The name to save to local file.")
        .defaultValue("")
        .build()
    );

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("Log Mode")
        .description("How results are shown.")
        .defaultValue(Mode.Logging)
        .build()
    );

    private final Setting<Integer> saveInterval = sgGeneral.add(new IntSetting.Builder()
        .name("Save Interval")
        .description("Time in seconds between coord saves")
        .defaultValue(5)
        .sliderRange(2, 30)
        .visible(() -> mode.get() == Mode.Logging)
        .build()
    );

    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private long millis;

    private ArrayList<BlockPos> coordList;

    private int currGoalIndex;

    public FollowBaritonePath()
    {
        super(Addon.CATEGORY, "FollowBaritonePath", "Follows or logs a baritone elytra path through the nether. Useful for having multiple people travel the same path.");
    }

    @Override
    public void onActivate()
    {
//        ChatUtils.sendPlayerMsg("#goal 0 0 0");
        millis = currentTimeMillis();
        if (pathName.get().isBlank())
        {
            info("Path name must not be blank, disabling.");
            this.toggle();
            return;
        }
        if (mode.get() == Mode.Following)
        {
            try {
                File file = new File(new File(new File(MeteorClient.FOLDER, "FollowBaritonePath"), Utils.getWorldName()), pathName.get() + ".json");
                FileReader reader = new FileReader(file);
                coordList = GSON.fromJson(reader, new TypeToken<ArrayList<BlockPos>>(){}.getType());
                reader.close();
                // find goal index based on current position
                BlockPos currPos = mc.player.getBlockPos();
                int closest = 0;
                for (int i = 1; i < coordList.size(); i++)
                {
                    if (coordList.get(i).getSquaredDistance(currPos) < coordList.get(closest).getSquaredDistance(currPos))
                    {
                        closest = i;
                    }
                }
                currGoalIndex = closest;
                BlockPos currGoal = coordList.get(currGoalIndex);
                ChatUtils.sendPlayerMsg("#goal " + currGoal.getX() + " " + currGoal.getY() + " " + currGoal.getZ());
                ChatUtils.sendPlayerMsg("#elytra");
            } catch (IOException e) {
                info("Could not find file, disabling.");
                this.toggle();
            }
        }
        else
        {
            info("Assuming you are baritone pathfinding in the nether.");
            info("Saving coords every " + saveInterval.get() + " seconds.");
            try {
                File file = new File(new File(new File(MeteorClient.FOLDER, "FollowBaritonePath"), Utils.getWorldName()), pathName.get() + ".json");
                FileReader reader = new FileReader(file);
                coordList = GSON.fromJson(reader, new TypeToken<ArrayList<BlockPos>>(){}.getType());
                reader.close();
            } catch (IOException e) {
                coordList = new ArrayList<BlockPos>();
            }
        }
    }

    @Override
    public void onDeactivate()
    {
        if (mode.get() == Mode.Logging) {
            try {
                File file = new File(new File(new File(MeteorClient.FOLDER, "FollowBaritonePath"), Utils.getWorldName()), pathName.get() + ".json");

                file.getParentFile().mkdirs();
                Writer writer = new FileWriter(file);
                GSON.toJson(coordList, writer);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @EventHandler
    private void onTick(TickEvent.Post event)
    {
        // log coords every x seconds
        if (mode.get() == Mode.Logging && (currentTimeMillis() - millis >= saveInterval.get() * 1000))
        {
            // dont log coords if they are close to the last ones
            if (!coordList.isEmpty())
            {
                BlockPos last = coordList.get(coordList.size() - 1);
                if (Math.sqrt(mc.player.squaredDistanceTo(last.getX(), mc.player.getY(), last.getZ())) < 10)
                {
                    return;
                }
            }

            millis = currentTimeMillis();
            coordList.add(mc.player.getBlockPos());
        }

        if (mode.get() == Mode.Following)
        {
            if (currGoalIndex == coordList.size())
            {
//                info("Path complete, disabling");
                this.toggle();
            }
        }
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event)
    {
        Text message = event.getMessage();
//        info("test");

        if (mode.get() == Mode.Following)
        {


            if (message.getString().contains("Above the landing spot"))
            {
                BlockPos currGoal = coordList.get(currGoalIndex);
                ChatUtils.sendPlayerMsg("#goal " + currGoal.getX() + " " + currGoal.getY() + " " + currGoal.getZ());
                currGoalIndex++;
            }
        }


    }


    public enum Mode
    {
        Logging,
        Following
    }

}
