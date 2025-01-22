package com.stash.hunt.modules;

import com.stash.hunt.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;

import static com.stash.hunt.Utils.firework;


public class Pitch40Util extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Double> boundGap = sgGeneral.add(new DoubleSetting.Builder()
        .name("Bound Gap")
        .description("The gap between the upper and lower bounds.")
        .defaultValue(60)
        .build()
    );

    public final Setting<Boolean> autoFirework = sgGeneral.add(new BoolSetting.Builder()
        .name("Auto Firework")
        .description("Uses a firework automatically if your velocity is too low.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> velocityThreshold = sgGeneral.add(new DoubleSetting.Builder()
        .name("Auto Firework Velocity Threshold")
        .description("Velocity must be below this value when going up for firework to activate.")
        .defaultValue(-0.05)
        .visible(autoFirework::get)
        .build()
    );

    public final Setting<Integer> fireworkCooldownTicks = sgGeneral.add(new IntSetting.Builder()
        .name("Auto Firework Cooldown (ticks)")
        .description("Cooldown after using a firework in ticks.")
        .defaultValue(300)
        .visible(autoFirework::get)
        .build()
    );

    public final Setting<Boolean> grimDura = sgGeneral.add(new BoolSetting.Builder()
        .name("Require Elytra")
        .description("Swaps to elytra before rocketing, fixes issues on mio grim dura.")
        .defaultValue(false)
        .build()
    );

    public Pitch40Util() {
        super(Addon.CATEGORY, "Pitch40Util", "Makes sure pitch 40 stays on when reconnecting to 2b2t, and sets your bounds as you reach highest point each climb.");
    }

    Class<? extends Module> elytraFly = ElytraFly.class;
    Module elytraFlyModule = Modules.get().get(elytraFly);

    @Override
    public void onActivate()
    {

    }

    @Override
    public void onDeactivate()
    {
        if (elytraFlyModule.isActive())
        {
            elytraFlyModule.toggle();
        }
    }

    int fireworkCooldown = 0;

    boolean goingUp = true;

    int elytraSwapSlot = -1;

    private void resetBounds()
    {
        Setting<Double> upperBounds = (Setting<Double>) elytraFlyModule.settings.get("pitch40-upper-bounds");
        upperBounds.set(mc.player.getY() - 5);
        Setting<Double> lowerBounds = (Setting<Double>) elytraFlyModule.settings.get("pitch40-lower-bounds");
        lowerBounds.set(mc.player.getY() - 5 - boundGap.get());
    }

    @EventHandler
    private void onTick(TickEvent.Pre event)
    {
        if (elytraFlyModule.isActive())
        {

            if (fireworkCooldown > 0) {
                fireworkCooldown--;
            }

            if (elytraSwapSlot != -1)
            {
                InvUtils.swap(elytraSwapSlot, true);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                InvUtils.swapBack();
                elytraSwapSlot = -1;
            }

            // this means the player fell below the lower bound, so we reset the bounds. this will only really happen if not using fireworks
            if (mc.player.getY() <= (double)elytraFlyModule.settings.get("pitch40-lower-bounds").get() - 10)
            {
                resetBounds();
                return;
            }

            // -40 pitch is facing upwards
            if (mc.player.getPitch() == -40)
            {
//                info("Velocity less than target: " + (mc.player.getVelocity().y < velocityThreshold.get()));
//                info("Y less than upper bounds: " + (mc.player.getY() < (double)elytraFlyModule.settings.get("pitch40-upper-bounds").get()));
                goingUp = true;
                if (autoFirework.get() && mc.player.getVelocity().y < velocityThreshold.get() && mc.player.getY() < (double)elytraFlyModule.settings.get("pitch40-upper-bounds").get())
                {
                    if (fireworkCooldown == 0) {
                        int launchStatus = firework(mc, grimDura.get());
                        if (launchStatus >= 0)
                        {
                            fireworkCooldown = fireworkCooldownTicks.get();
                            // cant swap back to chestplate on the same tick
                            // stupid solution, but we need a number for non swapping return value.
                            if (launchStatus != 200) elytraSwapSlot = launchStatus;

                        }
                    }
                }
            }
            // waits until your at the highest point, when y velocity is 0, then sets min and max bounds based on your position
            else if (goingUp && mc.player.getVelocity().y <= 0) {
                goingUp = false;
                resetBounds();
            }
        }
        else
        {
            // waits for you to not be in queue, then turns elytrafly back on
            if (!mc.player.getAbilities().allowFlying)
            {
                elytraFlyModule.toggle();
                resetBounds();
            }
        }

    }



}
