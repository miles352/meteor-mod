package com.example.addon.mixin;

import com.example.addon.modules.NoJumpDelay;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin
{
    @Shadow
    private int jumpingCooldown;

    @Unique
    Class<? extends meteordevelopment.meteorclient.systems.modules.Module> noJumpDelay = NoJumpDelay.class;
    @Unique
    Module noJumpDelayModule = Modules.get().get(noJumpDelay);

    @Inject(at = @At("HEAD"), method = "Lnet/minecraft/entity/LivingEntity;tickMovement()V")
    private void tickMovement(CallbackInfo ci)
    {
        if (noJumpDelayModule.isActive())
        {
            this.jumpingCooldown = 0;
        }
    }
}
