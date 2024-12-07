package com.example.addon.modules;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;

public class AFKBoostFly extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Double> upTime = sgGeneral.add(new DoubleSetting.Builder()
        .name("Millis Up")
        .defaultValue(1000.0)
        .sliderRange(100.0, 10000.0)
        .build()
    );

    public final Setting<Double> downTime = sgGeneral.add(new DoubleSetting.Builder()
        .name("Millis Down")
        .defaultValue(3000.0)
        .sliderRange(100.0, 10000.0)
        .build()
    );

    public final Setting<Double> upPitch = sgGeneral.add(new DoubleSetting.Builder()
        .name("Pitch Up")
        .defaultValue(-25.0)
        .sliderRange(-90.0, 0.0)
        .build()
    );

    public final Setting<Double> downPitch = sgGeneral.add(new DoubleSetting.Builder()
        .name("Pitch Down")
        .defaultValue(20.0)
        .sliderRange(0.0, 90.0)
        .build()
    );

    private long lastTime = System.currentTimeMillis();
    private boolean goingUp = false;

    public AFKBoostFly() {
        super(Addon.CATEGORY, "AFKBoostFly", "Looks up and down to gain momentum");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        if (goingUp && System.currentTimeMillis() - lastTime > upTime.get())
        {
            lastTime = System.currentTimeMillis();
            goingUp = false;
            mc.player.setPitch(downPitch.get().floatValue());
        } else if (!goingUp && System.currentTimeMillis() - lastTime > downTime.get())
        {
            lastTime = System.currentTimeMillis();
            goingUp = true;
            mc.player.setPitch(upPitch.get().floatValue());
        }
    }
}
