package com.example.addon.modules.searcharea;

import com.example.addon.Addon;
import com.example.addon.modules.searcharea.modes.Rectangle;
import com.example.addon.modules.searcharea.modes.Spiral;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;

public class SearchArea extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    public final Setting<SearchAreaModes> chunkLoadMode = sgGeneral.add(new EnumSetting.Builder<SearchAreaModes>()
        .name("Mode")
        .description("The mode chunks are loaded.")
        .defaultValue(SearchAreaModes.Rectangle)
        .onModuleActivated(chunkMode -> onModeChanged(chunkMode.get()))
        .onChanged(this::onModeChanged)
        .build()
    );

    public final Setting<BlockPos> startPos = sgGeneral.add(new BlockPosSetting.Builder()
        .name("Start Position")
        .description("The coordinates to start the rectangle at. Y Pos is ignored")
        .defaultValue(new BlockPos(0,0,0))
        .visible(() -> chunkLoadMode.get() == SearchAreaModes.Rectangle)
        .build()
    );

    public final Setting<BlockPos> targetPos = sgGeneral.add(new BlockPosSetting.Builder()
        .name("End Position")
        .description("The coordinates to end the rectangle at. Y Pos is ignored")
        .defaultValue(new BlockPos(0,0,0))
        .visible(() -> chunkLoadMode.get() == SearchAreaModes.Rectangle)
        .build()
    );

    public final Setting<Integer> rowGap = sgGeneral.add(new IntSetting.Builder()
        .name("Path Gap")
        .description("The amount of chunks to space between each chunk path.")
        .defaultValue(12)
        .min(0)
        .sliderRange(0, 32)
        .build()
    );

    public final Setting<Boolean> logToWebhook = sgGeneral.add(new BoolSetting.Builder()
        .name("Log To Webhook")
        .description("If you would like to log the chat to a webhook (for stash finder or just for chat)")
        .defaultValue(false)
        .build()
    );

    public final Setting<String> webhookLink = sgGeneral.add(new StringSetting.Builder()
        .name("Webhook Link")
        .description("A discord webhook link. Looks like this: https://discord.com/api/webhooks/webhookUserId/webHookTokenOrSomething")
        .defaultValue("")
        .visible(logToWebhook::get)
        .build()
    );

    public final Setting<Boolean> disconnectOnCompletion = sgGeneral.add(new BoolSetting.Builder()
        .name("Disconnect on Completion")
        .description("Whether to disconnect after the path is complete. This will turn autoreconnect off when disconnecting.")
        .defaultValue(false)
        .visible(() -> chunkLoadMode.get() == SearchAreaModes.Rectangle)
        .build()
    );


    public SearchArea() {
        super(Addon.CATEGORY, "Search Area", "Either loads chunks in a rectangle to a certain point from you, or spirals endlessly from you. Useful with Stash Finder or other map saving mods.");
    }

    private SearchAreaMode currentMode = new Rectangle();

    @Override
    public WWidget getWidget(GuiTheme theme)
    {
        WVerticalList list = theme.verticalList();
        WButton clear = list.add(theme.button("Clear Currently Selected")).widget();

        clear.action = () -> currentMode.clear();

        WButton clearAll = list.add(theme.button("Clear All")).widget();

        clearAll.action = () -> currentMode.clearAll();

        return list;
    }

    @Override
    public void onActivate() {
        currentMode.onActivate();
    }

    @Override
    public void onDeactivate()
    {
        currentMode.onDeactivate();
    }

    @EventHandler
    private void onTick(TickEvent.Post event)
    {
        currentMode.onTick();
    }

    private void onModeChanged(SearchAreaModes mode) {
        switch (mode) {
            case Rectangle -> currentMode = new Rectangle();
            case Spiral -> currentMode = new Spiral();
        }
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event)
    {
        currentMode.onMessageReceive(event);
    }

}
