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
import net.fabricmc.loader.api.FabricLoader;

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
        Modules.get().add(new AFKBoostFly());
        Modules.get().add(new ChatCoordSpammer());
        Modules.get().add(new Pitch40Util());

        if (FabricLoader.getInstance().isModLoaded("xaeroplus"))
        {
            Modules.get().add(new TrailFollower());
            Modules.get().add(new OldChunkNotifier());
        }
        else
        {
            LOG.info("XaeroPlus not found, disabling TrailFollower and OldChunkNotifier");
        }


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
