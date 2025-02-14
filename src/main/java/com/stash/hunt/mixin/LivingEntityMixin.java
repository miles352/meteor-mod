package com.stash.hunt.mixin;

import com.stash.hunt.modules.GrimEfly;
import com.stash.hunt.modules.NoJumpDelay;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin
{
    @Shadow
    protected int fallFlyingTicks;
    @Shadow
    private int jumpingCooldown;

    @Shadow
    public abstract Brain<?> getBrain();

    Module grimEfly = Modules.get().get(GrimEfly.class);
    Module noJumpDelay = Modules.get().get(NoJumpDelay.class);

    @Inject(at = @At("HEAD"), method = "Lnet/minecraft/entity/LivingEntity;tickMovement()V")
    private void tickMovement(CallbackInfo ci)
    {
        if (mc.player.getBrain().equals(this.getBrain()) && ((grimEfly != null && grimEfly.isActive() && !((Setting<Boolean>)grimEfly.settings.get("paused")).get()) || noJumpDelay.isActive()))
        {
            this.jumpingCooldown = 0;
        }
    }

    @Inject(at = @At("HEAD"), method = "Lnet/minecraft/entity/LivingEntity;tickFallFlying()V", cancellable = true)
    private void tickFallFlying(CallbackInfo ci)
    {
        if (mc.player.getBrain().equals(this.getBrain()) && grimEfly != null && grimEfly.isActive() && !((Setting<Boolean>)grimEfly.settings.get("paused")).get())
        {
            this.fallFlyingTicks++;
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "Lnet/minecraft/entity/LivingEntity;isFallFlying()Z", cancellable = true)
    private void isFallFlying(CallbackInfoReturnable<Boolean> cir)
    {
        if (mc.player.getBrain().equals(this.getBrain()) && grimEfly != null && grimEfly.isActive() && !((Setting<Boolean>)grimEfly.settings.get("paused")).get())
        {
            cir.setReturnValue(true);
        }
    }
}
