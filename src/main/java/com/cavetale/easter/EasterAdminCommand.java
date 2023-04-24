package com.cavetale.easter;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.easter.struct.Region;
import com.cavetale.easter.struct.User;
import com.cavetale.fam.trophy.SQLTrophy;
import com.cavetale.fam.trophy.Trophies;
import com.cavetale.mytems.item.trophy.TrophyCategory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static com.cavetale.easter.util.EasterText.easterify;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

final class EasterAdminCommand extends AbstractCommand<EasterPlugin> {
    protected EasterAdminCommand(final EasterPlugin plugin) {
        super(plugin, "easteradmin");
    }

    @Override
    protected void onEnable() {
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
        rootNode.addChild("clearegg").arguments("<player>")
            .description("Clear current player egg")
            .completers(CommandArgCompleter.NULL)
            .senderCaller(this::clearEgg);
        rootNode.addChild("skipcooldown").arguments("<player>")
            .description("Skip player egg cooldown")
            .completers(CommandArgCompleter.NULL)
            .senderCaller(this::skipCooldown);
        CommandNode scoreNode = rootNode.addChild("score")
            .description("Score subcommands");
        scoreNode.addChild("reward").denyTabCompletion()
            .description("Reward the top 10")
            .senderCaller(this::scoreReward);
        scoreNode.addChild("maketrophies").denyTabCompletion()
            .description("Make Event Trophies")
            .senderCaller(this::makeTrophies);
    }

    private boolean setArea(Player player, String[] args) {
        Cuboid cuboid = Cuboid.requireSelectionOf(player);
        Region region = new Region(player.getWorld().getName(), cuboid);
        plugin.save.setRegion(region);
        plugin.save();
        plugin.reloadChunks();
        player.sendMessage(text("Region set: " + region, YELLOW));
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
            String coord = currentEgg.getX()
                + " " + currentEgg.getY()
                + " " + currentEgg.getZ();
            String cmd = "/tp" + " " + coord;
            sender.sendMessage(text("Current egg: " + coord, YELLOW)
                               .insertion(coord)
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
        player.openMerchant(token ? plugin.trades.makeTokenMerchant(player) : plugin.trades.makeEggMerchant(), true);
        sender.sendMessage(text("Opened " + (token ? "Token" : "Egg") + " merchant for " + player.getName(), YELLOW));
    }

    private void makeTrophies(CommandSender sender) {
        List<UUID> uuids = new ArrayList<>(plugin.save.getUsers().keySet());
        Collections.sort(uuids, (a, b) -> Integer.compare(plugin.save.userOf(b).getTotalEggsDiscovered(),
                                                          plugin.save.userOf(a).getTotalEggsDiscovered()));
        List<SQLTrophy> trophies = new ArrayList<>();
        int previousEggs = -1;
        int placement = 0;
        for (UUID uuid : uuids) {
            int eggs = plugin.save.userOf(uuid).getTotalEggsDiscovered();
            if (eggs == 0) break;
            if (eggs != previousEggs) {
                placement += 1;
                previousEggs = eggs;
            }
            trophies.add(new SQLTrophy(uuid,
                                       "easter_egg_hunt",
                                       placement,
                                       TrophyCategory.EASTER,
                                       easterify("Easter Egg Hunt " + Timer.getYear()),
                                       "You collected " + eggs + " Easter Egg" + (eggs != 1 ? "s" : "") + "!"));
        }
        Trophies.insertTrophies(trophies);
        sender.sendMessage("Created " + trophies.size() + " trophies!");
    }

    private boolean clearEgg(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        PlayerCache player = PlayerCache.require(args[0]);
        User user = plugin.save.userOf(player.uuid);
        Vec3i currentEgg = user.getCurrentEgg();
        if (currentEgg == null) {
            throw new CommandWarn("Player has no current egg: " + player.name);
        }
        user.setCurrentEgg(null);
        EasterEgg easterEgg = plugin.easterEggMap.get(currentEgg);
        if (easterEgg != null) easterEgg.itemFrame.remove();
        plugin.save();
        sender.sendMessage(text("Egg removed: " + player.name + ", " + currentEgg, YELLOW));
        return true;
    }

    private boolean skipCooldown(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        PlayerCache player = PlayerCache.require(args[0]);
        User user = plugin.save.userOf(player.uuid);
        user.setEggCooldown(0L);
        sender.sendMessage("Egg cooldown cleared: " + player.name);
        return true;
    }

    private void scoreReward(CommandSender sender) {
        int count = 0;
        for (var hi : plugin.getHighscore()) {
            if (hi.getPlacement() > 10) break;
            String name = PlayerCache.nameForUuid(hi.uuid);
            plugin.getLogger().info(hi.getPlacement() + ") " + hi.getScore() + " " + name);
            String cmd = "titles unlockset " + name + " EggSmash";
            plugin.getLogger().info("Dispatching command: " + cmd);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            count += 1;
        }
        sender.sendMessage(text(count + " players rewarded", AQUA));
    }
}
