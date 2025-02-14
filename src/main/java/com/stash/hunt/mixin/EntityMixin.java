package com.stash.hunt.mixin;

import com.stash.hunt.modules.GrimEfly;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Entity.class)
public class EntityMixin
{
    @Shadow
    protected UUID uuid;

    Module grimEfly = Modules.get().get(GrimEfly.class);

    @Inject(at = @At("HEAD"), method = "Lnet/minecraft/entity/Entity;getPose()Lnet/minecraft/entity/EntityPose;", cancellable = true)
    private void getPose(CallbackInfoReturnable<EntityPose> cir)
    {
        if (mc.player != null && grimEfly != null && grimEfly.isActive() && this.uuid == mc.player.getUuid() && !((Setting<Boolean>)grimEfly.settings.get("paused")).get())
        {
            cir.setReturnValue(EntityPose.STANDING);
        }
    }

    @Inject(at = @At("HEAD"), method = "Lnet/minecraft/entity/Entity;isSprinting()Z", cancellable = true)
    private void isSprinting(CallbackInfoReturnable<Boolean> cir)
    {
        if (mc.player != null && grimEfly != null && mc.player.isOnGround() && grimEfly.isActive() && this.uuid == mc.player.getUuid() && !((Setting<Boolean>)grimEfly.settings.get("paused")).get())
        {
            cir.setReturnValue(true);
        }
    }
}


