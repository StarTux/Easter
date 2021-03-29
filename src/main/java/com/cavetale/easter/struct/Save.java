package com.cavetale.easter.struct;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.Data;

@Data
public final class Save {
    private Region region = null;
    private List<User> users = new ArrayList<>();

    public User userOf(UUID uuid) {
        for (User user : users) {
            if (Objects.equals(uuid, user.uuid)) return user;
        }
        User user = new User(uuid);
        users.add(user);
        return user;
    }

    public boolean userHasCurrentEgg(Vec3i vector) {
        for (User user : users) {
            if (Objects.equals(vector, user.currentEgg)) return true;
        }
        return false;
    }
}
