package com.example.addon.modules;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.systems.modules.Module;

public class NoJumpDelay extends Module
{
    public NoJumpDelay()
    {
        super(
            Addon.CATEGORY,
            "NoJumpDelay",
            "Removes the delay between jumps."
        );
    }
}
