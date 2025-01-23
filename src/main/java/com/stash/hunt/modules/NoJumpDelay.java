package com.stash.hunt.modules;

import com.stash.hunt.Addon;
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
