package com.example.addon;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownServiceException;

public class Utils
{
    public static void pointTowards(Vec3d pos, MinecraftClient mc) {
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
