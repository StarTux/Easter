package com.cavetale.easter;

import com.cavetale.mirage.DataVar;
import com.cavetale.mirage.EntityFlag;
import com.cavetale.mirage.Mirage;
import com.cavetale.mirage.MirageData;
import com.cavetale.mirage.MirageType;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Gsonable
 */
@Data
final class EasterEgg {
    private int index;
    private PlayerHead playerHead;
    private Mirage mirage;
    private boolean disabled = false;
    // Location
    private String world = "spawn";
    private double x, y, z;
    private int cx, cz;

    void setLocation(Location loc) {
        if (loc == null) throw new NullPointerException("loc cannot be null");
        this.world = loc.getWorld().getName();
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.cx = (int)Math.floor(x) >> 4;
        this.cz = (int)Math.floor(z) >> 4;
    }

    Location toLocation() {
        if (this.world == null) throw new NullPointerException("world cannot be null");
        World w = Bukkit.getWorld(this.world);
        if (w == null) throw new NullPointerException("World not found: " + this.world);
        return new Location(w, this.x, this.y, this.z, 0.0f, 0.0f);
    }

    void tick(EasterPlugin plugin) {
        if (this.mirage == null) {
            // Create skull
            ItemStack item = this.playerHead.makeItem(this.index);
            // Put on head
            this.mirage = new Mirage(plugin);
            MirageData mirageData = new MirageData();
            mirageData.type = MirageType.MOB;
            mirageData.debugName = "easter" + this.index;
            mirageData.entityType = EntityType.ARMOR_STAND;
            mirageData.location = MirageData.Location.fromBukkitLocation(toLocation().add(0, -1.35, 0));
            this.mirage.setup(mirageData);
            this.mirage.setEquipment(EquipmentSlot.HEAD, item);
            this.mirage.setMetadata(DataVar.ENTITY_FLAGS, (byte)EntityFlag.ENTITY_INVISIBLE.bitMask);
        } else {
            this.mirage.updateObserverList();
            if (this.mirage.getObservers().isEmpty()) return;
            float yaw = (float)(System.nanoTime() / 10000000L) * 0.33f;
            this.mirage.look(yaw % 360.0f, 0.0f);
        }
    }

    void clearMirage() {
        if (this.mirage == null) return;
        this.mirage.removeAllObservers();
        this.mirage = null;
    }

    boolean isId(int id) {
        return this.mirage != null && this.mirage.getEntity().getId() == id;
    }
}
