package com.example.addon.modules;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;


public class Pitch40Util extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> boundGap = sgGeneral.add(new DoubleSetting.Builder()
        .name("Bound Gap")
        .description("The gap between the upper and lower bounds.")
        .defaultValue(60)
        .build()
    );

    public Pitch40Util() {
        super(Addon.CATEGORY, "Pitch40Util", "Makes sure pitch 40 stays on when reconnecting to 2b2t, and sets your bounds as you reach highest point each climb.");
    }

    @Override
    public void onActivate()
    {

    }

    @Override
    public void onDeactivate()
    {

    }


    boolean goingUp = true;
    Class<? extends Module> elytraFly = ElytraFly.class;
    Module module = Modules.get().get(elytraFly);

    private void resetBounds()
    {
        Setting<Double> upperBounds = (Setting<Double>) module.settings.get("pitch40-upper-bounds");
        upperBounds.set(mc.player.getY() - 5);
        Setting<Double> lowerBounds = (Setting<Double>) module.settings.get("pitch40-lower-bounds");
        lowerBounds.set(mc.player.getY() - 5 - boundGap.get());
    }

    @EventHandler
    private void onTick(TickEvent.Pre event)
    {
        if (module.isActive())
        {
            if (mc.player.getY() < (double)module.settings.get("pitch40-lower-bounds").get() - 10)
            {
//                ChatUtils.info("Pitch40Util: You are below the lower bounds, resetting bounds.");
                resetBounds();
                return;
            }
            if (mc.player.getPitch() == -40)
            {
                goingUp = true;
            }
            // waits until your at the highest point, when y velocity is 0, then sets min and max bounds based on your position
            else if (goingUp && mc.player.getVelocity().y <= 0) {
                goingUp = false;
                resetBounds();
//                ChatUtils.info("Y: " + mc.player.getY());
            }
//            ChatUtils.info("Pitch: " + mc.player.getPitch() + " Speed Y: " + mc.player.getVelocity().y);
            return;
        }

        // waits for you to not be in queue, then turns elytrafly back on
        if (!mc.player.getAbilities().allowFlying)
        {
            module.toggle();
            resetBounds();
        }
    }

}
