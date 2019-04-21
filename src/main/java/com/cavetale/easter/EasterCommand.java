package com.cavetale.easter;

import com.google.gson.GsonBuilder;
import com.winthier.generic_events.GenericEvents;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
final class EasterCommand implements CommandExecutor {
    private final EasterPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) return false;
        switch (args[0]) {
        case "reload":
            plugin.reloadConfig();
            sender.sendMessage("config.yml reloaded");
            return true;
        case "start":
            plugin.startNewRound();
            plugin.setupRound();
            sender.sendMessage("New round started");
            return true;
        case "info":
            sender.sendMessage("=== Round info");
            sender.sendMessage(new GsonBuilder().setPrettyPrinting().create().toJson(plugin.round));
            return true;
        case "tp": {
            if (args.length != 2) return false;
            if (!(sender instanceof Player)) throw new IllegalStateException("Player expected");
            int idx = Integer.parseInt(args[1]);
            if (idx < 0 || idx >= plugin.easterEggs.size()) throw new ArrayIndexOutOfBoundsException("Illegal egg index: " + idx);
            EasterEgg easterEgg = plugin.easterEggs.get(idx);
            Player player = (Player)sender;
            Location loc = easterEgg.toLocation();
            player.teleport(loc);
            player.sendMessage("Teleported to egg #" + idx + " (" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + ") [" + (easterEgg.isDisabled() ? "disabled" : "enabled") + "]");
            return true;
        }
        case "hi": {
            List<UUID> uuids = new ArrayList<>(plugin.scores.scores.keySet());
            Collections.sort(uuids, (a, b) -> Integer.compare(plugin.scores.scores.get(b),
                                                              plugin.scores.scores.get(a)));
            int i = 0;
            sender.sendMessage("");
            sender.sendMessage("Easter Highscore");
            for (UUID uuid : uuids) {
                i += 1;
                sender.sendMessage("" + i + ") "
                                   + plugin.scores.scores.get(uuid) + " "
                                   + GenericEvents.cachedPlayerName(uuid));
                if (i >= 10) break;
            }
            return true;
        }
        default:
            return false;
        }
    }
}
