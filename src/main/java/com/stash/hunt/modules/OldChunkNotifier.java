package com.stash.hunt.modules;

import com.stash.hunt.Addon;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ChunkDataEvent;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.OldChunks;
import xaeroplus.module.impl.PaletteNewChunks;

import static com.stash.hunt.Utils.sendWebhook;


public class OldChunkNotifier extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<String> webhookLink = sgGeneral.add(new StringSetting.Builder()
        .name("Webhook Link")
        .description("A discord webhook link. Looks like this: https://discord.com/api/webhooks/webhookUserId/webHookTokenOrSomething")
        .defaultValue("")
        .build()
    );

    public final Setting<String> discordId = sgGeneral.add(new StringSetting.Builder()
        .name("Discord ID")
        .description("Your discord ID")
        .defaultValue("")
        .build()
    );

    public OldChunkNotifier() {
        super(Addon.CATEGORY, "OldChunkNotifier", "Pings you on discord if you find old chunks");
    }

    @Override
    public void onActivate()
    {
        XaeroPlus.EVENT_BUS.register(this);
    }

    @Override
    public void onDeactivate()
    {
        XaeroPlus.EVENT_BUS.unregister(this);
    }

    @net.lenni0451.lambdaevents.EventHandler(priority = -1)
    public void onChunkData(ChunkDataEvent event)
    {
        // avoid 2b2t end loading screen
        if (mc.player.getAbilities().allowFlying) return;

        if (webhookLink.get().isEmpty() || discordId.get().isEmpty()) return;
        boolean is119NewChunk = ModuleManager.getModule(PaletteNewChunks.class)
            .isNewChunk(
                event.chunk().getPos().x,
                event.chunk().getPos().z,
                event.chunk().getWorld().getRegistryKey()
            );

        boolean is112OldChunk = ModuleManager.getModule(OldChunks.class)
            .isOldChunk(
                event.chunk().getPos().x,
                event.chunk().getPos().z,
                event.chunk().getWorld().getRegistryKey()
            );

        if (is119NewChunk && !is112OldChunk) return;

        String message = "";

        if (is112OldChunk && !is119NewChunk) {
            message = "1.12 Followed in 1.19+ Old Chunk Detected";
        } else if (is112OldChunk && is119NewChunk) {
            message = "1.12 Unfollowed in 1.19+ Old Chunk Detected";
        } else {
            message = "1.19+ Old Chunk Detected";
        }
        String finalMessage = message;
        new Thread(() -> sendWebhook(webhookLink.get(), "Old Chunk Detected", finalMessage + " at " + mc.player.getPos().toString(), discordId.get(), mc.player.getGameProfile().getName())).start();

    }

}
