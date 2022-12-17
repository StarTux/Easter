package com.cavetale.easter.struct;

import com.cavetale.core.struct.Vec3i;
import lombok.Value;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;

@Value
public final class BlockVector {
    protected String world;
    protected Vec3i vector;

    public Block toBlock() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : vector.toBlock(w);
    }
}
