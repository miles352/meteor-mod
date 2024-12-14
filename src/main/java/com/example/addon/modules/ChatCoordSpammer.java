package com.example.addon.modules;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;


public class ChatCoordSpammer extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("The delay between messages in milliseconds.")
            .defaultValue(60 * 1000)
            .build()
    );


    private long timer;

    public ChatCoordSpammer() {
        super(Addon.CATEGORY, "ChatCoordSpammer", "Sends your current coords to chat.");
        timer = System.currentTimeMillis();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (System.currentTimeMillis() - timer >= delay.get()) {
            timer = System.currentTimeMillis();
            ChatUtils.sendPlayerMsg("I'm currently at " + mc.player.getBlockPos().getX() + " " + mc.player.getBlockPos().getY() + " " + mc.player.getBlockPos().getZ() + " in the " +
                ((mc.player.getWorld().getDimension().hasCeiling()) ? "nether" : "overworld") + "!");
        }
    }
}
