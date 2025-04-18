package com.cavetale.easter.struct;

import com.cavetale.core.struct.Cuboid;
import lombok.Value;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

@Value
public final class Region {
    public static final Region ZERO = new Region("", Cuboid.ZERO);
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

    public boolean isInWorld(World bukkitWorld) {
        return world.equals(bukkitWorld.getName());
    }

    @Override
    public String toString() {
        return world + ":" + cuboid;
    }
}
