package com.cavetale.easter;

import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.easter.struct.Cuboid;
import com.cavetale.easter.struct.Region;
import com.cavetale.easter.struct.User;
import com.cavetale.easter.struct.Vec3i;
import com.cavetale.easter.util.WorldEdit;
import com.winthier.playercache.PlayerCache;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

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
        rootNode.addChild("info").arguments("[player]")
            .description("Player info")
            .senderCaller(this::info);
        rootNode.addChild("eggmerchant").arguments("<player>")
            .description("Open egg merchant")
            .completers(CommandArgCompleter.NULL)
            .senderCaller(this::eggMerchant);
        rootNode.addChild("tokenmerchant").arguments("<player>")
            .description("Open token merchant")
            .completers(CommandArgCompleter.NULL)
            .senderCaller(this::tokenMerchant);
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

    private boolean setArea(Player player, String[] args) {
        Cuboid cuboid = WorldEdit.getSelection(player);
        if (cuboid == null) throw new CommandWarn("Make a selection first!");
        Region region = new Region(player.getWorld().getName(), cuboid);
        plugin.save.setRegion(region);
        plugin.save();
        plugin.reloadChunks();
        player.sendMessage(ChatColor.YELLOW + "Region set: " + region);
        return true;
    }

    private boolean debug(CommandSender sender, String[] args) {
        sender.sendMessage("dayId: " + Timer.getDayId());
        sender.sendMessage("easterDay: " + Timer.getEasterDay() + "/" + Timer.DAYS);
        sender.sendMessage("totalEggs: " + Timer.getTotalEggs());
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        User user;
        String userName;
        if (args.length == 1) {
            PlayerCache player = PlayerCache.forArg(args[0]);
            if (player == null) {
                throw new CommandWarn("[easteradmin:cheat] player not found: " + args[0]);
            }
            user = plugin.save.userOf(player.uuid);
            userName = player.name;
        } else {
            if (!(sender instanceof Player player)) {
                throw new CommandWarn("[easteradmin:cheat] player expected");
            }
            user = plugin.save.userOf(player.getUniqueId());
            userName = player.getName();
        }
        sender.sendMessage(text("Eggs: " + user.getRegularEggsDiscovered()
                                + "/" + user.getTotalEggsDiscovered(), YELLOW));
        long now = System.currentTimeMillis();
        long egg = Math.max(user.getEggCooldown(), now) - now;
        long mob = Math.max(user.getSpawnCooldown(), now) - now;
        sender.sendMessage(text("Cooldowns:"
                                + " egg=" + Duration.ofMillis(egg).toSeconds() + "s"
                                + " mob=" + Duration.ofMillis(mob).toSeconds() + "s",
                                YELLOW));
        Vec3i currentEgg = user.getCurrentEgg();
        if (currentEgg == null) {
            sender.sendMessage(text("No current egg", RED));
        } else {
            String cmd = "/tp"
                + " " + currentEgg.getX()
                + " " + currentEgg.getY()
                + " " + currentEgg.getZ();
            sender.sendMessage(text("Current egg: " + currentEgg, YELLOW)
                               .clickEvent(ClickEvent.suggestCommand(cmd)));
        }
        return true;
    }

    private boolean eggMerchant(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        merchant(sender, args[0], false);
        return true;
    }

    private boolean tokenMerchant(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        merchant(sender, args[0], true);
        return true;
    }

    private void merchant(CommandSender sender, String name, boolean token) {
        Player player = Bukkit.getPlayerExact(name);
        if (player == null) throw new CommandWarn("Player not found: " + name);
        player.openMerchant(token ? Trades.makeTokenMerchant(player) : Trades.makeEggMerchant(), true);
        sender.sendMessage(text("Opened " + (token ? "Token" : "Egg") + " merchant for " + player.getName(), YELLOW));
    }
}
