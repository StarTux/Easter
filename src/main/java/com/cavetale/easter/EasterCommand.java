package com.cavetale.easter;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.mytems.item.font.Glyph;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import static com.cavetale.core.font.Unicode.subscript;
import static com.cavetale.easter.util.EasterText.easterify;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class EasterCommand extends AbstractCommand<EasterPlugin> {
    protected EasterCommand(final EasterPlugin plugin) {
        super(plugin, "easter");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("hi")
            .description("Easter highscore")
            .senderCaller(this::hi);
    }

    private void tp(Player player) {
        World world = plugin.save.getRegion().toWorld();
        if (world == null) throw new CommandWarn("Easter is over!");
        player.teleport(world.getSpawnLocation(), TeleportCause.COMMAND);
        player.sendMessage(text("Teleporting to the Easter world", LIGHT_PURPLE));
    }

    private void hi(CommandSender sender) {
        List<UUID> uuids = new ArrayList<>(plugin.save.getUsers().keySet());
        Collections.sort(uuids, (a, b) -> Integer.compare(plugin.save.userOf(b).getTotalEggsDiscovered(),
                                                          plugin.save.userOf(a).getTotalEggsDiscovered()));
        sender.sendMessage(easterify("Easter Highscore - Eggs Discovered"));
        int rank = 0;
        int lastScore = -1;
        for (int i = 0; i < 10; i += 1) {
            if (i >= uuids.size()) break;
            UUID uuid = uuids.get(i);
            int score = plugin.save.userOf(uuid).getTotalEggsDiscovered();
            if (score != lastScore) {
                rank += 1;
                lastScore = score;
            }
            Player player = Bukkit.getPlayer(uuid);
            sender.sendMessage(textOfChildren(Glyph.toComponent("" + rank),
                                              text(subscript("" + score), GRAY),
                                              space(),
                                              (player != null
                                               ? player.displayName()
                                               : text("" + PlayerCache.nameForUuid(uuid), WHITE))));
        }
    }
}
