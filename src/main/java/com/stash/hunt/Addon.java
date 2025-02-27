package com.stash.hunt;

import com.stash.hunt.hud.Weather;
import com.stash.hunt.modules.*;
import com.stash.hunt.modules.searcharea.SearchArea;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.fabricmc.loader.api.FabricLoader;
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
        Modules.get().add(new GotoPosition());
        Modules.get().add(new ChestIndex());
        Modules.get().add(new HighlightOldLava());
//        Modules.get().add(new FollowBaritonePath());
        Modules.get().add(new AFKBoostFly());
        Modules.get().add(new Pitch40Util());
//        Modules.get().add(new AutoTrade());
//        Modules.get().add(new XPBot());
//        Modules.get().add(new UnknownAccountNotifier());
        Modules.get().add(new GrimEfly());
        Modules.get().add(new NoJumpDelay());
        Modules.get().add(new GrimAirPlace());
        Modules.get().add(new DiscordNotifs());
//        Modules.get().add(new ChunkSizeCalculator());

//        Modules.get().add(new EndermanItemDetector());


        if (FabricLoader.getInstance().isModLoaded("xaeroworldmap") && FabricLoader.getInstance().isModLoaded("xaerominimap"))
        {
            Modules.get().add(new BetterStashFinder());
            Modules.get().add(new OldChunkNotifier());
            Modules.get().add(new TrailFollower());
        }
        else
        {
            LOG.info("Xaeros minimap and world map not found, disabling modules that require it.");
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
        return "com.stash.hunt";
    }
}
