package com.example.addon.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.jetbrains.annotations.Nullable;
import com.example.addon.modules.NoJumpDelay;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin
{
    @Shadow
    private int jumpingCooldown;

    @Unique
    @Nullable
    private NoJumpDelay noJumpDelayModule = null;

    @Inject(at = @At("HEAD"), method = "Lnet/minecraft/entity/LivingEntity;tickMovement()V")
    private void tickMovement(CallbackInfo ci)
    {
        if (noJumpDelayModule == null)
        {
            Modules mods = Modules.get();
            if (mods == null) return;
            noJumpDelayModule = mods.get(NoJumpDelay.class);

            if (noJumpDelayModule == null) return;
        }

        if (noJumpDelayModule.isActive())
        {
            this.jumpingCooldown = 0;
        }
    }
}
