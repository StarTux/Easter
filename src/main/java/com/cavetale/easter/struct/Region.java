package com.cavetale.easter.struct;

import lombok.Value;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

@Value
public final class Region {
    protected final String world;
    protected final Cuboid cuboid;

    public World toWorld() {
        return Bukkit.getWorld(world);
    }

    public boolean contains(Block block) {
        return block.getWorld().getName().equals(world) && cuboid.contains(block);
    }

    public boolean contains(Location location) {
        return location.getWorld().getName().equals(world) && cuboid.contains(location);
    }

    @Override
    public String toString() {
        return world + ":" + cuboid;
    }
}
