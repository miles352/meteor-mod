package com.example.addon;

import meteordevelopment.meteorclient.utils.misc.input.Input;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownServiceException;
import java.util.ArrayDeque;

public class Utils
{

    public static void setPressed(KeyBinding key, boolean pressed)
    {
        key.setPressed(pressed);
        Input.setKeyState(key, pressed);
    }

    public static int emptyInvSlots(MinecraftClient mc) {
        int airCount = 0;
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.AIR) {
                airCount++;
            }
        }
        return airCount;
    }


    public static void sendWebhook(String webhookURL, String title, String message, String pingID, String playerName)
    {
        String json = "";
        json += "{\"embeds\": [{"
            + "\"title\": \""+ title +"\","
            + "\"description\": \""+ message +"\","
            + "\"color\": 15258703,"
            + "\"footer\": {"
            + "\"text\": \"From: " + playerName + "\"}"
            + "}]}";
        sendRequest(webhookURL, json);

        if (pingID != null)
        {
            json = "{\"content\": \"<@" + pingID + ">\"}";
            sendRequest(webhookURL, json);
        }
    }

    public static void sendWebhook(String webhookURL, String jsonObject, String pingID)
    {
        sendRequest(webhookURL, jsonObject);

        if (pingID != null)
        {
            jsonObject = "{\"content\": \"<@" + pingID + ">\"}";
            sendRequest(webhookURL, jsonObject);
        }
    }

    private static void sendRequest(String webhookURL, String json) {
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
        }
        catch (MalformedURLException | UnknownServiceException e)
        {
//            searchArea.logToWebhook.set(false);
//            searchArea.webhookLink.set("");
//            info("Issue with webhook link. It has been cleared, try again.");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
