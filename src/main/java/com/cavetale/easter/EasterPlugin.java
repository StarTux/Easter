package com.cavetale.easter;

import com.cavetale.mirage.PlayerUseEntityEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class EasterPlugin extends JavaPlugin implements Listener {
    static final class Round {
        int time;
        int doneTime;
        int roundNumber;
        int hidden = 0;
        int found = 0;
    }

    static final class Scores {
        Map<UUID, Integer> scores = new HashMap<>();
    }

    Round round;
    Scores scores;
    List<EasterEgg> easterEggs = new ArrayList<>();
    List<PlayerHead> playerHeads = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.round = loadJsonFile("round.json", Round.class, Round::new);
        this.scores = loadJsonFile("scores.json", Scores.class, Scores::new);
        // playerHeads
        YamlConfiguration eeyaml = YamlConfiguration.loadConfiguration(new InputStreamReader(getResource("easter_eggs.yml")));
        for (Map<?, ?> map : eeyaml.getMapList("heads")) {
            PlayerHead head = new PlayerHead((String) map.get("Id"), (String) map.get("Texture"));
            this.playerHeads.add(head);
        }
        if (this.playerHeads.isEmpty()) throw new IllegalStateException("No player heads loaded!");
        if (this.round.roundNumber == 0) {
            startNewRound();
        }
        setupRound();
        getServer().getScheduler().runTaskTimer(this, this::onTick, 1L, 1L);
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("easter").setExecutor(new EasterCommand(this));
        getCommand("hi").setExecutor(new HiCommand(this));
    }

    @Override
    public void onDisable() {
        saveRound();
        saveScores();
        disableEasterEggs();
    }

    void onTick() {
        boolean allDisabled = true;
        try {
            for (EasterEgg easterEgg : this.easterEggs) {
                if (!easterEgg.isDisabled()) {
                    easterEgg.tick(this);
                    allDisabled = false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (allDisabled) {
            this.round.doneTime += 1;
        }
        this.round.time += 1;
        if (this.round.doneTime > 100 || this.round.time > (20 * 60 * 60)) {
            startNewRound();
            saveRound();
            setupRound();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        return false;
    }

    void saveScores() {
        saveJsonFile("scores.json", this.scores, true);
    }

    void saveRound() {
        saveJsonFile("round.json", this.round, true);
    }

    void startNewRound() {
        disableEasterEggs();
        this.round.roundNumber += 1;
        this.round.time = 0;
        this.round.doneTime = 0;
        this.round.hidden = this.round.found + 10;
        this.round.found = 0;
        getLogger().info("Started round " + this.round.roundNumber + " with " + this.round.hidden + " eggs.");
        announce(ChatColor.LIGHT_PURPLE + "The Easter Bunny just visited spawn and hid " + this.round.hidden + " eggs!");
    }

    void setupRound() {
        final String worldName = getConfig().getString("world");
        final World world = getServer().getWorld(worldName);
        if (world == null) throw new IllegalStateException("World not found: " + worldName);
        final double cx = getConfig().getDouble("x");
        final double cz = getConfig().getDouble("z");
        final double radius = getConfig().getDouble("radius");
        Random random = ThreadLocalRandom.current();
        int total = this.round.hidden - this.round.found;
        for (int i = 0; i < total; i += 1) {
            EasterEgg easterEgg = new EasterEgg();
            easterEgg.setIndex(i);
            double x = cx + random.nextDouble() * radius - random.nextDouble() * radius;
            double z = cz + random.nextDouble() * radius - random.nextDouble() * radius;
            Block block = world.getHighestBlockAt((int)Math.floor(x), (int)Math.floor(z));
            while (!block.isEmpty()) block = block.getRelative(0, 1, 0);
            easterEgg.setLocation(block.getLocation().add(0.5, 0.0, 0.5));
            easterEgg.setPlayerHead(this.playerHeads.get(i % this.playerHeads.size()));
            this.easterEggs.add(easterEgg);
        }
    }

    void disableEasterEggs() {
        for (EasterEgg easterEgg : this.easterEggs) {
            easterEgg.clearMirage();
        }
        this.easterEggs.clear();
    }

    // JSON IO

    <T> T loadJsonFile(String fn, Class<T> clazz, Supplier<T> dfl) {
        File file = new File(getDataFolder(), fn);
        if (!file.exists()) return dfl.get();
        try (FileReader fr = new FileReader(file)) {
            return new Gson().fromJson(fr, clazz);
        } catch (FileNotFoundException fnfr) {
            return dfl.get();
        } catch (IOException ioe) {
            throw new IllegalStateException("Loading " + file, ioe);
        }
    }

    <T> T loadJsonResource(String fn, Class<T> clazz) {
        try (InputStream input = getResource(fn);
             InputStreamReader reader = new InputStreamReader(input)) {
            return new Gson().fromJson(reader, clazz);
        } catch (IOException ioe) {
            throw new IllegalStateException("Loading " + fn, ioe);
        }
    }

    void saveJsonFile(String fn, Object obj, boolean pretty) {
        File dir = getDataFolder();
        dir.mkdirs();
        File file = new File(dir, fn);
        try (FileWriter fw = new FileWriter(file)) {
            Gson gson = pretty ? new GsonBuilder().setPrettyPrinting().create() : new Gson();
            gson.toJson(obj, fw);
        } catch (IOException ioe) {
            throw new IllegalStateException("Saving " + file, ioe);
        }
    }

    @EventHandler
    public void onPlayerUseEntity(PlayerUseEntityEvent event) {
        int id = event.getEntityId();
        for (EasterEgg easterEgg : this.easterEggs) {
            if (easterEgg.isId(id)) {
                if (easterEgg.isDisabled()) return;
                findEgg(event.getPlayer(), easterEgg);
                return;
            }
        }
    }

    void announce(World world, String txt) {
        for (Player player : world.getPlayers()) {
            player.sendActionBar(txt);
        }
    }

    void announce(String txt) {
        for (Player player : getServer().getOnlinePlayers()) {
            player.sendMessage(txt);
            player.sendActionBar(txt);
        }
    }

    void giveItem(Player player, ItemStack item) {
        player.getWorld().dropItem(player.getEyeLocation(), item).setPickupDelay(0);
    }

    void findEgg(Player player, EasterEgg easterEgg) {
        easterEgg.setDisabled(true);
        easterEgg.clearMirage();
        Random random = ThreadLocalRandom.current();
        // Reward
        switch (random.nextInt(10)) {
        case 0: giveItem(player, new ItemStack(Material.DIAMOND)); break;
        case 1: giveItem(player, new ItemStack(Material.EMERALD)); break;
        case 2: giveItem(player, new ItemStack(Material.IRON_INGOT)); break;
        case 3: giveItem(player, new ItemStack(Material.GOLD_INGOT)); break;
        case 4: giveItem(player, new ItemStack(Material.INK_SAC)); break;
        case 5: giveItem(player, new ItemStack(Material.COAL)); break;
        case 6: giveItem(player, new ItemStack(Material.BLAZE_ROD)); break;
        case 7: giveItem(player, new ItemStack(Material.EGG)); break;
        case 8: giveItem(player, new ItemStack(Material.MILK_BUCKET)); break;
        default:
            int idx = random.nextInt(this.playerHeads.size());
            giveItem(player, this.playerHeads.get(idx).makeItem(idx));
            break;
        }
        // Score
        UUID uuid = player.getUniqueId();
        Integer score = this.scores.scores.get(uuid);
        score = score == null ? 1 : score + 1;
        this.scores.scores.put(uuid, score);
        saveScores();
        this.round.found += 1;
        saveRound();
        // Effect
        Location loc = easterEgg.toLocation();
        int rabbitc = random.nextInt(3) + 1;
        Location loc2 = loc.clone();
        loc2.add(random.nextDouble() * 0.5 - random.nextDouble() * 0.5,
                 random.nextDouble() * 0.5,
                 random.nextDouble() * 0.5 - random.nextDouble() * 0.5);
        Rabbit rabbit = loc.getWorld().spawn(loc2, Rabbit.class, r -> {
                r.setPersistent(false);
                r.setRemoveWhenFarAway(true);
                r.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 9999, 0, true, false));
                r.setInvulnerable(true);
                switch (random.nextInt(6)) {
                case 0: r.setRabbitType(Rabbit.Type.BLACK); break;
                case 1: r.setRabbitType(Rabbit.Type.BLACK_AND_WHITE); break;
                case 2: r.setRabbitType(Rabbit.Type.BROWN); break;
                case 3: r.setRabbitType(Rabbit.Type.GOLD); break;
                case 4: r.setRabbitType(Rabbit.Type.SALT_AND_PEPPER); break;
                case 5: r.setRabbitType(Rabbit.Type.WHITE); break;
                default: break;
                }
                if (random.nextBoolean()) {
                    r.setBaby();
                }
            });
        double v = 0.25;
        Vector velo = new Vector(random.nextDouble() * v - random.nextDouble() * v,
                                 v,
                                 random.nextDouble() * v - random.nextDouble() * v);
        rabbit.setVelocity(velo);
        loc = loc.add(0.0, 1.0, 0.0);
        Bat bat = loc.getWorld().spawn(loc, Bat.class, b -> {
                b.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, true, false));
                b.setSilent(true);
                b.setPersistent(false);
                b.setRemoveWhenFarAway(true);
            });
        Sheep sheep = loc.getWorld().spawn(loc, Sheep.class, s -> {
                s.setSilent(true);
                s.setPersistent(false);
                s.setRemoveWhenFarAway(true);
            });
        bat.addPassenger(sheep);
        getServer().getScheduler().runTaskLater(this, () -> {
                rabbit.remove();
                bat.remove();
                sheep.remove();
            }, 400L);
        getServer().dispatchCommand(getServer().getConsoleSender(), "data merge entity " + sheep.getUniqueId() + " {CustomName:\"\\\"jeb_\\\"\"}");
        // Firework firework = loc.getWorld().spawn(loc, Firework.class, fw -> {
        //         FireworkMeta meta = fw.getFireworkMeta();
        //         meta.addEffect(FireworkEffect.builder()
        //                        .with(FireworkEffect.Type.BALL)
        //                        .withColor(Color.fromRGB(random.nextInt(256),
        //                                                 random.nextInt(256),
        //                                                 random.nextInt(256)))
        //                        .build());
        //         fw.setFireworkMeta(meta);
        //     });
        // getServer().getScheduler().runTaskLater(this, () -> {
        //         firework.detonate();
        //     }, 60L);
        player.playSound(player.getEyeLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.5f, 2.0f);
        int remain = this.round.hidden - this.round.found;
        if (remain > 0) {
            announce(player.getWorld(),
                     "" + ChatColor.LIGHT_PURPLE + player.getName() + " found an easter egg! There are " + remain + " more to find.");
        } else {
            announce(player.getWorld(),
                     "" + ChatColor.LIGHT_PURPLE + player.getName() + " found the final easter egg!");
        }
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Your score is now " + score + ".");
    }
}
