package com.cavetale.easter;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandWarn;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class EasterCommand extends AbstractCommand<EasterPlugin> {
    protected EasterCommand(final EasterPlugin plugin) {
        super(plugin, "easter");
    }

    @Override
    protected void onEnable() {
        rootNode.denyTabCompletion()
            .description("Warp to the Easter world")
            .playerCaller(this::tp);
    }

    private void tp(Player player) {
        World world = plugin.save.getRegion().toWorld();
        if (world == null) throw new CommandWarn("Easter is over!");
        player.teleport(world.getSpawnLocation(), TeleportCause.COMMAND);
        player.sendMessage(text("Teleporting to the Easter world", LIGHT_PURPLE));
    }
}
