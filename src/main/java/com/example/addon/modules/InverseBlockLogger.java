package com.example.addon.modules;

import com.example.addon.Addon;
import com.example.addon.modules.searcharea.SearchArea;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;

import meteordevelopment.orbit.EventHandler;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;

import java.util.List;

import static com.example.addon.Utils.sendWebhook;


public class InverseBlockLogger extends Module
{
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("Blocks")
        .description("Blocks to search for.")
        .build()
    );

    public final Setting<Boolean> useWebhook = sgGeneral.add(new BoolSetting.Builder()
        .name("Webhook Mode")
        .description("The mode for discord webhooks.")
        .defaultValue(false)
        .build()
    );

    public final Setting<String> webhookLink = sgGeneral.add(new StringSetting.Builder()
        .name("Webhook Link")
        .description("A discord webhook link. Looks like this: https://discord.com/api/webhooks/webhookUserId/webHookTokenOrSomething")
        .defaultValue("")
        .visible(useWebhook::get)
        .build()
    );

    public final Setting<Boolean> pingWebhook = sgGeneral.add(new BoolSetting.Builder()
        .name("Ping")
        .description("Ping you when a block is not found")
        .defaultValue(false)
        .visible(useWebhook::get)
        .build()
    );

    public final Setting<String> discordId = sgGeneral.add(new StringSetting.Builder()
        .name("Discord ID")
        .description("Your discord ID")
        .defaultValue("")
        .visible(() -> useWebhook.get() && pingWebhook.get())
        .build()
    );


    public InverseBlockLogger()
    {
        super(Addon.CATEGORY, "InverseBlockLogger", "Logs if a block is not found in a loaded chunk. Does not log which block specifically is not found.");
    }

    @Override
    public void onActivate()
    {

    }

    @Override
    public void onDeactivate()
    {

    }


    @EventHandler
    private void onTick(TickEvent.Post event)
    {

    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event)
    {
        boolean found = false;
        for (BlockEntity blockEntity : event.chunk().getBlockEntities().values())
        {
            BlockState blockState = event.chunk().getBlockState(blockEntity.getPos());
            if (blocks.get().contains(blockState.getBlock()))
            {
                found = true;
                break;
            }
        }
        if (!found)
        {
//            sendWebhook(webhookLink.get(), "Chunk found without selected blocks", "Chunk found at ")
        }
    }
}
