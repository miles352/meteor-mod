package com.example.addon;

import com.example.addon.hud.Weather;
import com.example.addon.modules.*;
import com.example.addon.modules.searcharea.SearchArea;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class Addon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Jefff Mod");
    public static final HudGroup HUD_GROUP = new HudGroup("Jefff Mod");

    public final Settings settings = new Settings();

    @Override
    public void onInitialize() {
        LOG.info("Initializing Jefff Mod");

        // Modules
        Modules.get().add(new SearchArea());
        Modules.get().add(new AutoLogY());
        Modules.get().add(new AutoJoinServer());
        Modules.get().add(new GotoPosition());
        Modules.get().add(new ChestIndex());
        Modules.get().add(new LookAt());
        Modules.get().add(new HighlightOldLava());
        Modules.get().add(new FollowBaritonePath());


        // Commands
//        Commands.add(new CommandExample());

        // HUD
        Hud.get().register(Weather.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }
}
