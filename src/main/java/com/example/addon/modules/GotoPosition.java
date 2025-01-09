package com.example.addon.modules;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;


public class GotoPosition extends Module
{
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<BlockPos> target = sgGeneral.add(new BlockPosSetting.Builder()
        .name("Target Position")
        .description("Coords to go to. Y is ignored.")
        .defaultValue(new BlockPos(0,0,0))
        .build()
    );

    public final Setting<Boolean> disconnectOnComplete = sgGeneral.add(new BoolSetting.Builder()
        .name("Disconnect when complete")
        .description("Disconnects when you get to the target")
        .defaultValue(false)
        .build()
    );


    public GotoPosition()
    {
        super(Addon.CATEGORY, "GotoPosition", "Goes in a straight line towards the position you give and stops once there.");
    }

    @Override
    public void onActivate()
    {
        double distance = Math.sqrt(mc.player.getBlockPos().getSquaredDistance(target.get().getX(), mc.player.getY(), target.get().getZ()));
        long totalSeconds = (long)(distance / 70);
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        info("Completion will take an estimated %02d hours %02d minutes %02d seconds at an average speed of 70bps", hours, minutes, seconds);
    }

    @Override
    public void onDeactivate()
    {
        mc.options.forwardKey.setPressed(false);
        Input.setKeyState(mc.options.forwardKey, false);
        mc.player.setVelocity(0, 0, 0);
    }

//    @Override
//    public WWidget getWidget(GuiTheme theme)
//    {
//        WVerticalList list = theme.verticalList();
//        WButton clear = list.add(theme.button("Clear Coordinates")).widget();
//
//        clear.action = () -> {
//            target.reset();
//        };
//
//        return list;
//    }

    @EventHandler
    private void onTick(TickEvent.Post event)
    {
        if (Math.sqrt(mc.player.getBlockPos().getSquaredDistance(target.get().getX(), mc.player.getY(), target.get().getZ())) > 5)
        {
            mc.player.setYaw((float) Rotations.getYaw(new Vec3d(target.get().getX(), (int) mc.player.getY(), target.get().getZ())));
            mc.options.forwardKey.setPressed(true);
            Input.setKeyState(mc.options.forwardKey, true);
        }
        else
        {
            mc.options.forwardKey.setPressed(false);
            Input.setKeyState(mc.options.forwardKey, false);
            mc.player.setVelocity(0, 0, 0);
            if (disconnectOnComplete.get())
            {
                mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("[GotoPosition] You are at your destination!")));
            }
            target.reset();
            this.toggle();
        }

    }

}
