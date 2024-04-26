package com.example.addon.modules.searcharea;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;

import static meteordevelopment.meteorclient.utils.player.ChatUtils.info;

public class SearchAreaMode
{

    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    protected final SearchArea searchArea;
    protected final MinecraftClient mc;
    private final SearchAreaModes type;

    public SearchAreaMode(SearchAreaModes type) {
        this.searchArea = Modules.get().get(SearchArea.class);
        this.mc = MinecraftClient.getInstance();
        this.type = type;
    }

    public void onTick()
    {

    }

    public void onActivate()
    {

    }

    public void onDeactivate()
    {
        setPressed(mc.options.forwardKey, false);
    }

    // stolen from autowalk
    protected void setPressed(KeyBinding key, boolean pressed)
    {
        key.setPressed(pressed);
        Input.setKeyState(key, pressed);
    }

    // thanks chatgpt
    protected void pointTowards(BlockPos pos)
    {
        double deltaX = pos.getX() - mc.player.getX();
        double deltaZ = pos.getZ() - mc.player.getZ();

        double angle = Math.atan2(deltaZ, deltaX);

        float yaw = (float)Math.toDegrees(angle) - 90.0f;

        yaw = yaw % 360.0f;
        if (yaw < 0) {
            yaw += 360.0f;
        }

        mc.player.setYaw(yaw);
    }

    public void onMessageReceive(ReceiveMessageEvent event)
    {
        if (searchArea.logToWebhook.get())
        {
            Text message = event.getMessage();
            sendWebhook(searchArea.webhookLink.get(), "Chat Message", message.getString());
        }

    }

    protected void sendWebhook(String webhookURL, String title, String message)
    {
        String json = "";
        json += "{\"embeds\": [{"
            + "\"title\": \""+ title +"\","
            + "\"description\": \""+ message +"\","
            + "\"color\": 15258703"
            + "}]}";
        try {
            URL url = new URL(webhookURL);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.addRequestProperty("Content-Type", "application/json");
            con.addRequestProperty("User-Agent", "Mozilla");
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            OutputStream stream = con.getOutputStream();
            stream.write(json.getBytes());
            stream.flush();
            stream.close();
            con.getInputStream().close();
            con.disconnect();
        } catch (Exception e) {
            searchArea.logToWebhook.set(false);
            searchArea.webhookLink.set("");
            info("Issue with webhook link. It has been cleared, try again.");
        }
    }

    protected File getJsonFile(String fileName) {
        return new File(new File(new File(MeteorClient.FOLDER, "search-area"), Utils.getFileWorldName()), fileName + ".json");
    }

    protected void saveToJson(boolean goingToStart, PathingData pd)
    {
        // last pos doesn't matter if disconnecting while going to start
        if (!goingToStart) pd.currPos = mc.player.getBlockPos();
        try {
            File file = getJsonFile(type.toString());
            file.getParentFile().mkdirs();
            Writer writer = new FileWriter(file);
            GSON.toJson(pd, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static class PathingData
    {
        public BlockPos initialPos;
        public BlockPos currPos;
        public float yawDirection;
        public boolean mainPath;
    }

    public void clear()
    {
        File file = getJsonFile(type.toString());
        file.delete();
    }

    public void clear(String mode)
    {
        File file = getJsonFile(mode);
        file.delete();
    }

    public void clearAll()
    {
        for (SearchAreaModes mode : SearchAreaModes.values())
        {
            clear(mode.toString());
        }
    }

    public String toString()
    {
        return type.toString();
    }

}
