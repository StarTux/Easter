package com.cavetale.easter.struct;

import java.util.UUID;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data
public final class User {
    protected final UUID uuid;
    protected Vec3i currentEgg;
    protected int regularEggsDiscovered;
    protected int totalEggsDiscovered;
    protected long eggCooldown; // millis
    protected long spawnCooldown; // evil mobs
}
