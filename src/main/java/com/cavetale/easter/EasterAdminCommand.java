package com.cavetale.easter;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.easter.struct.Cuboid;
import com.cavetale.easter.struct.Region;
import com.cavetale.easter.struct.User;
import com.cavetale.easter.struct.Vec3i;
import com.cavetale.easter.util.WorldEdit;
import com.cavetale.fam.trophy.SQLTrophy;
import com.cavetale.fam.trophy.Trophies;
import com.cavetale.mytems.Mytems;
import com.winthier.playercache.PlayerCache;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
        rootNode.addChild("maketrophies").denyTabCompletion()
            .description("Make Event Trophies")
            .senderCaller(this::makeTrophies);
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
            final Mytems mytems;
            switch (placement) {
            case 1: mytems = Mytems.GOLD_MEDAL; break;
            case 2: mytems = Mytems.SILVER_MEDAL; break;
            case 3: mytems = Mytems.BRONZE_MEDAL; break;
            default: mytems = Mytems.EASTER_TOKEN;
            }
            trophies.add(new SQLTrophy(uuid,
                                      "easter/egg_hunt_2022",
                                      placement,
                                      mytems,
                                      easterify("Easter Egg Hunt 2022"),
                                       "You collected " + eggs + " Easter Egg" + (eggs != 1 ? "s" : "") + "!"));
        }
        Trophies.insertTrophies(trophies);
        sender.sendMessage("Created " + trophies.size() + " trophies!");
    }
}
