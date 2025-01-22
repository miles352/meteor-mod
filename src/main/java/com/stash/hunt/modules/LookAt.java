package com.stash.hunt.modules;

import com.stash.hunt.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

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
        if (pos != null) mc.player.setYaw((float) Rotations.getYaw(pos));
    }
}
