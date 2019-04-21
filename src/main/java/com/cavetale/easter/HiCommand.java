package com.cavetale.easter;

import com.winthier.generic_events.GenericEvents;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

@RequiredArgsConstructor
final class HiCommand implements CommandExecutor {
    private final EasterPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 0) return false;
        List<UUID> uuids = new ArrayList<>(plugin.scores.scores.keySet());
        Collections.sort(uuids, (a, b) -> Integer.compare(plugin.scores.scores.get(b),
                                                          plugin.scores.scores.get(a)));
        int i = 0;
        sender.sendMessage("");
        sender.sendMessage(ChatColor.DARK_PURPLE + "* * * "
                           + ChatColor.LIGHT_PURPLE + "Easter Highscore"
                           + ChatColor.DARK_PURPLE + " * * *");
        for (UUID uuid : uuids) {
            i += 1;
            sender.sendMessage(""
                               + ChatColor.GOLD + i + ") "
                               + ChatColor.YELLOW + plugin.scores.scores.get(uuid) + " "
                               + ChatColor.LIGHT_PURPLE + GenericEvents.cachedPlayerName(uuid));
            if (i >= 10) break;
        }
        return true;
    }
}
