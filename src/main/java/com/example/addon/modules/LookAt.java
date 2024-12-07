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
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import static com.example.addon.Utils.posToYaw;

public class LookAt extends Module
{
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private Entity currentEntity;


    public LookAt()
    {
        super(Addon.CATEGORY, "LookAt", "Locks view to the nearest player");
    }

    @Override
    public void onActivate()
    {

    }

    @Override
    public void onDeactivate()
    {

    }


    @EventHandler
    private void onTick(TickEvent.Post event)
    {
        double closest = 999;
        Vec3d pos = null;
        for (Entity entity : mc.world.getEntities())
        {
            if (entity instanceof PlayerEntity && entity != mc.player)
            {
                double distance = Math.sqrt(entity.squaredDistanceTo(mc.player.getPos()));
                if (distance < closest)
                {
                    closest = distance;
                    pos = entity.getPos();
                }
            }
        }
        if (pos != null) mc.player.setYaw(posToYaw(pos, mc));
    }
}
