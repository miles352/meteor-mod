package com.stash.hunt.modules;

import com.stash.hunt.Addon;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static com.stash.hunt.Utils.sendWebhook;


public class DiscordNotifs extends Module
{
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> webhookURL = sgGeneral.add(new StringSetting.Builder()
        .name("Webhook Link")
        .description("The discord webhook to use, looks like this: https://discord.com/api/webhooks/webhookUserId/webHookTokenOrSomething")
        .defaultValue("")
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("Message Delay (MS)")
        .description("The delay between messages in milliseconds.")
        .defaultValue(2000)
        .build()
    );

    private final Setting<Boolean> timestamp = sgGeneral.add(new BoolSetting.Builder()
        .name("Timestamp")
        .description("If the message should have a timestamp.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> queue = sgGeneral.add(new BoolSetting.Builder()
        .name("2b2t Queue")
        .description("If your position in queue should be logged.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> connections = sgGeneral.add(new BoolSetting.Builder()
        .name("Disconnect")
        .description("If a message should be logged when leaving.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> dm = sgGeneral.add(new BoolSetting.Builder()
        .name("Whisper")
        .description("If whispers should be logged.")
        .defaultValue(false)
        .build()
    );

    private final Setting<ChatType> chat = sgGeneral.add(new EnumSetting.Builder<ChatType>()
        .name("Chat Messages")
        .description("The type of chat messages to log.")
        .defaultValue(ChatType.NONE)
        .build()
    );

    public DiscordNotifs()
    {
        super(Addon.CATEGORY, "DiscordNotifs", "Sends notifications to a discord webhook.");
//        MeteorClient.EVENT_BUS.subscribe(new StaticListener());
    }

    private long delayTimer = 0;
    private int lastQueuePos;
//    private String connectedServer;

    @EventHandler
    private void onTick(TickEvent.Post event)
    {
        if (delayTimer > 0) delayTimer--;
    }

    public void handleMessage(String message)
    {
        if (webhookURL.get().isBlank() || delayTimer > 0) return;

        // \n\nPosition in queue: 299\nYou can purchase priority queue status to join the server faster, visit shop.2b2t.org
        int queueIndex = message.indexOf("Position in queue: ");
        if (queue.get() && queueIndex != -1)
        {
            int queuePos = Integer.parseInt(message.substring(queueIndex + 19, message.indexOf("\n", queueIndex)));
            if (queuePos != lastQueuePos)
            {
                sendWebhookMessage("Queue position: " + queuePos);
                lastQueuePos = queuePos;
                return;
            }
        }

        message = message.replaceAll("\n", "\\n").trim();

        switch (chat.get())
        {
            case ALL:
            {
                sendWebhookMessage(message);
                return;
            }
            case PLAYER_MESSAGE:
            {
                if (message.startsWith("<"))
                {
                    sendWebhookMessage(message);
                    return;
                }
                break;
            }
            case NON_PLAYER_MESSAGE:
            {
                if (!message.startsWith("<"))
                {
                    sendWebhookMessage(message);
                    return;
                }
                break;
            }
            case NONE:
                break;
        }

        if (dm.get())
        {
            // from player
            if (!message.startsWith("<") && message.contains("whispers: ")
                || message.startsWith("to ")) // to player
            {
                sendWebhookMessage(message);
                return;
            }
        }
    }
//
//    private class StaticListener {
//        // Does not trigger when joining 2b2t from queue server. Not sure why
//        @EventHandler
//        private void onGameJoined(ServerConnectEndEvent event) {
//            if (connections.get())
//            {
//                connectedServer = event.address.getAddress().getHostAddress();
//                sendWebhookMessage("Joined " + connectedServer);
//            }
//        }
//    }

    @EventHandler
    private void onDisconnect(GameLeftEvent event)
    {
        if (connections.get())
        {
//            String server = connectedServer == null ? "" : " from " + connectedServer;
            sendWebhookMessage("Disconnected");
        }
    }

    private void sendWebhookMessage(String message)
    {
        delayTimer = delay.get() / 1000 * 20;
        if (timestamp.get())
        {
            LocalTime now = LocalTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String timestamp = now.format(formatter);
            message = "[" + timestamp + "] " + message;
        }
        String json = "{\n" +
            "\"content\": \"```" + message + "```\"\n" +
            "}";
        // use threads so the game doesnt lag when sending a ton of webhooks
        new Thread(() -> sendWebhook(webhookURL.get(), json, null)).start();
    }

    private enum ChatType
    {
        NONE,
        ALL,
        PLAYER_MESSAGE,
        NON_PLAYER_MESSAGE,
    }

}
