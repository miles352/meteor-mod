package com.example.addon.modules;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;


public class AutoJoinServer extends Module
{
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private long startTime = 0;
    private boolean joined = false;

    private final Setting<String> address = sgGeneral.add(new StringSetting.Builder()
        .name("Address")
        .description("The address of the server you wish to connect to.")
        .defaultValue("")
        .build()
    );



    public AutoJoinServer()
    {
        super(Addon.CATEGORY, "AutoJoinServer", "Joins the server as soon as minecraft is launched.");
        runInMainMenu = true;
    }

    @EventHandler
    private void onTick(TickEvent.Post event)
    {
        if (startTime == 0) startTime = System.nanoTime();
        if (!joined && System.nanoTime() - startTime > 1e10)
        {
            joined = true;
            ConnectScreen.connect(new TitleScreen(), mc, ServerAddress.parse(address.get()), new ServerInfo("Minecraft Server", address.get(), ServerInfo.ServerType.OTHER), false);
        }


    }



}
