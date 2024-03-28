package com.cavetale.easter.struct;

import com.cavetale.core.struct.Vec3i;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.Data;

@Data
public final class Save {
    private Region region = Region.ZERO;
    private Map<UUID, User> users = new HashMap<>();

    public User userOf(UUID uuid) {
        return users.computeIfAbsent(uuid, User::new);
    }

    public boolean userHasCurrentEgg(Vec3i vector) {
        for (User user : users.values()) {
            if (Objects.equals(vector, user.currentEgg)) return true;
        }
        return false;
    }
}
