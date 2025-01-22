package com.stash.hunt.modules;

import com.stash.hunt.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;

public class AutoLogY extends Module
{
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> logOutY = sgGeneral.add(new DoubleSetting.Builder()
        .name("Auto Log out if below this Y")
        .defaultValue(256)
        .min(-128)
        .sliderRange(-128, 320)
        .build()
    );

    public AutoLogY()
    {
        super(Addon.CATEGORY, "AutoLogY", "Logs out if you go below a certain Y value.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event)
    {
        if (mc.player.getY() < logOutY.get())
        {
            mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("[AutoLogY] Player was at Y=" + mc.player.getY() + " which is below your limit of Y=" + logOutY.get())));
        }
    }
}
