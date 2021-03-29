package com.cavetale.easter;

import java.util.UUID;
import lombok.Value;
import org.bukkit.entity.ItemFrame;

/**
 * Spawned Easter Egg runtime.
 */
@Value
public final class EasterEgg {
    protected final UUID owner;
    protected final ItemFrame itemFrame;
}
