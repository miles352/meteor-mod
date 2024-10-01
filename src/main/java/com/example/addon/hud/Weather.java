package com.example.addon.hud;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Alignment;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;

import static meteordevelopment.meteorclient.utils.player.ChatUtils.info;


public class Weather extends HudElement {
    public static final HudElementInfo<Weather> INFO = new HudElementInfo<>(Addon.HUD_GROUP, "Weather", "Displays current weather", Weather::new);
    private MinecraftClient mc;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private boolean recalculateSize;

    public final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Custom scale.")
        .defaultValue(1)
        .min(0.5)
        .sliderRange(0.5, 3)
        .build()
    );

    public Weather() {
        super(INFO);
        mc = MinecraftClient.getInstance();
    }

    @Override
    public void render(HudRenderer renderer) {
        String textWidth = "Weather:            ";
        setSize(renderer.textWidth(textWidth, true, scale.get()), renderer.textHeight(true, scale.get()));
        String weather = "None";
        if (mc.world.getDimension().bedWorks())
        {
            if (mc.world.isThundering())
            {
                weather = "Thundering";
            }
            else if (mc.world.isRaining())
            {
                weather = "Raining";
            }
            else
            {
                weather = "Clear";
            }
        }
        renderer.text("Weather: ", x, y, Color.WHITE, true, scale.get());
        renderer.text(weather, renderer.textWidth("Weather: ", scale.get()) + x, y, Color.LIGHT_GRAY, true, scale.get());
    }


}
