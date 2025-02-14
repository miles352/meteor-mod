package com.stash.hunt;

import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownServiceException;

public class Utils
{

    // returns -1 if fails, 200 if successful, and slot of chestplate if it had to swap (needed for mio grimdura)
    public static int firework(MinecraftClient mc, boolean elytraRequired) {

        // cant use a rocket if not wearing an elytra
        int elytraSwapSlot = -1;
        if (elytraRequired && !mc.player.getInventory().getArmorStack(2).isOf(Items.ELYTRA))
        {
            FindItemResult itemResult = InvUtils.findInHotbar(Items.ELYTRA);
            if (!itemResult.found()) {
                return -1;
            }
            else
            {
                elytraSwapSlot = itemResult.slot();
                InvUtils.swap(itemResult.slot(), true);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                InvUtils.swapBack();
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
        }

        FindItemResult itemResult = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        if (!itemResult.found()) return -1;

        if (itemResult.isOffhand()) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            mc.player.swingHand(Hand.OFF_HAND);
        } else {
            InvUtils.swap(itemResult.slot(), true);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            InvUtils.swapBack();
        }
        if (elytraSwapSlot != -1)
        {
            return elytraSwapSlot;
        }
        return 200;
    }

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

    public static Vec3d positionInDirection(Vec3d pos, double yaw, double distance)
    {
        Vec3d offset = (new Vec3d(Math.sin(-yaw * Math.PI / 180), 0, Math.cos(-yaw * Math.PI / 180)).normalize()).multiply(distance);
        return pos.add(offset);
    }

    public static int totalInvCount(MinecraftClient mc, Item item) {
        if (mc.player == null) return 0;
        int itemCount = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                itemCount += stack.getCount();
            }
        }
        return itemCount;
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
