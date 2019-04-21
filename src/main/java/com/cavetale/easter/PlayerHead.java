package com.cavetale.easter;

import com.cavetale.dirty.Dirty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
final class PlayerHead {
    final String id, url;

    ItemStack makeItem(int index) {
        ItemStack result = Dirty.newCraftItemStack(Material.PLAYER_HEAD);
        Optional<Object> tag = Dirty.accessItemNBT(result, true);
        tag = Dirty.setNBT(tag, "SkullOwner", new HashMap<String, Object>());
        Dirty.setNBT(tag, "Name", "Easter Egg (" + index + ")");
        Dirty.setNBT(tag, "Id", this.id);
        tag = Dirty.setNBT(tag, "Properties", new HashMap<String, Object>());
        tag = Dirty.setNBT(tag, "textures", new ArrayList<Object>());
        tag = Dirty.addNBT(tag, new HashMap<>());
        Dirty.setNBT(tag, "Value", this.url);
        return result;
    }
}
