package com.stash.hunt.mixin;

import com.stash.hunt.modules.DiscordNotifs;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MessageHandler.class)
public class MessageHandlerMixin
{
    // 2b2t queue wasn't appearing using meteors receiveMessageEvent so i did this
    @Inject(at = @At("HEAD"), method = "Lnet/minecraft/client/network/message/MessageHandler;onGameMessage(Lnet/minecraft/text/Text;Z)V")
    void onGameMessage(Text message, boolean overlay, CallbackInfo ci)
    {
        DiscordNotifs discordNotifs = Modules.get().get(DiscordNotifs.class);
        if (discordNotifs.isActive() && message != null)
        {
            String packetString = message.getString();
            discordNotifs.handleMessage(packetString);
        }
    }

}
