package com.cavetale.easter.struct;

import lombok.Value;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

@Value
public final class Vec3i {
    public static final Vec3i ZERO = new Vec3i(0, 0, 0);
    public static final Vec3i ONE = new Vec3i(1, 1, 1);
    public final int x;
    public final int y;
    public final int z;

    public static Vec3i of(Block block) {
        return new Vec3i(block.getX(), block.getY(), block.getZ());
    }

    public static Vec3i of(Location location) {
        return new Vec3i(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public Block toBlock(World world) {
        return world.getBlockAt(x, y, z);
    }

    public boolean isAt(Block block) {
        return x == block.getX()
            && y == block.getY()
            && z == block.getZ();
    }

    public Vec3i add(int dx, int dy, int dz) {
        return new Vec3i(x + dx, y + dy, z + dz);
    }

    public Vec3i multiply(int mul) {
        return new Vec3i(x * mul, y * mul, z * mul);
    }

    public int distanceSquared(Vec3i other) {
        int dx = other.x - x;
        int dy = other.y - y;
        int dz = other.z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public String toString() {
        return "" + x + "," + y + "," + z;
    }
}
