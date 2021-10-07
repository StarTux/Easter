package com.cavetale.easter;

import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.easter.struct.Cuboid;
import com.cavetale.easter.struct.Region;
import com.cavetale.easter.struct.User;
import com.cavetale.easter.struct.Vec3i;
import com.cavetale.easter.util.WorldEdit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
final class EasterAdminCommand implements TabExecutor {
    private final EasterPlugin plugin;
    private CommandNode rootNode;

    public EasterAdminCommand enable() {
        rootNode = new CommandNode("easteradmin");
        rootNode.addChild("setarea").denyTabCompletion()
            .playerCaller(this::setArea);
        rootNode.addChild("debug").denyTabCompletion()
            .senderCaller(this::debug);
        rootNode.addChild("cheat").denyTabCompletion()
            .playerCaller(this::cheat);
        plugin.getCommand("easteradmin").setExecutor(this);
        return this;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        return rootNode.call(sender, command, alias, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return rootNode.complete(sender, command, alias, args);
    }

    boolean setArea(Player player, String[] args) {
        Cuboid cuboid = WorldEdit.getSelection(player);
        if (cuboid == null) throw new CommandWarn("Make a selection first!");
        Region region = new Region(player.getWorld().getName(), cuboid);
        plugin.save.setRegion(region);
        plugin.save();
        plugin.reloadChunks();
        player.sendMessage(ChatColor.YELLOW + "Region set: " + region);
        return true;
    }

    boolean debug(CommandSender sender, String[] args) {
        sender.sendMessage("dayId = " + Timer.getDayId());
        sender.sendMessage("easterDay = " + Timer.getEasterDay());
        sender.sendMessage("totalEggs = " + Timer.getTotalEggs());
        return true;
    }

    boolean cheat(Player player, String[] args) {
        User user = plugin.save.userOf(player.getUniqueId());
        Vec3i currentEgg = user.getCurrentEgg();
        if (currentEgg == null) {
            player.sendMessage(Component.text("No current egg!").color(NamedTextColor.RED));
            return true;
        }
        String cmd = "/tp"
            + " " + currentEgg.getX()
            + " " + currentEgg.getY()
            + " " + currentEgg.getZ();
        player.sendMessage(Component.text("Current egg: " + currentEgg)
                           .color(NamedTextColor.GREEN)
                           .clickEvent(ClickEvent.suggestCommand(cmd)));
        return true;
    }
}
