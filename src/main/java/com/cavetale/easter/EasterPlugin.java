package com.cavetale.easter;

import com.cavetale.easter.struct.Region;
import com.cavetale.easter.struct.Save;
import com.cavetale.easter.struct.User;
import com.cavetale.easter.struct.Vec3i;
import com.cavetale.easter.util.Fireworks;
import com.cavetale.easter.util.Json;
import com.cavetale.mytems.Mytems;
import com.cavetale.sidebar.PlayerSidebarEvent;
import com.cavetale.sidebar.Priority;
import com.winthier.generic_events.PlayerCanBuildEvent;
import com.winthier.playercache.PlayerCache;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Rotation;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class EasterPlugin extends JavaPlugin implements Listener {
    protected static EasterPlugin instance;
    protected Save save;
    protected Map<Vec3i, EasterEgg> easterEggMap = new HashMap<>();
    protected List<Entity> evilMobs = new ArrayList<>();
    protected Random random = new Random();

    @Override
    public void onEnable() {
        instance = this;
        Timer.enable();
        save = Json.load(new File(getDataFolder(), "save.json"), Save.class, Save::new);
        if (save.getRegion() != null) {
            loadChunks(save.getRegion());
        }
        Bukkit.getScheduler().runTaskTimer(this, this::tickPlayers, 20L, 20L);
        Bukkit.getPluginManager().registerEvents(this, this);
        new EasterAdminCommand(this).enable();
    }

    @Override
    public void onDisable() {
        save();
        for (EasterEgg easterEgg : easterEggMap.values()) {
            easterEgg.itemFrame.remove();
        }
        easterEggMap.clear();
        for (World world : Bukkit.getWorlds()) {
            world.removePluginChunkTickets(this);
        }
        for (Entity evilMob : evilMobs) {
            evilMob.remove();
        }
        evilMobs.clear();
    }

    void save() {
        getDataFolder().mkdirs();
        Json.save(new File(getDataFolder(), "save.json"), save, true);
    }

    void tickPlayers() {
        if (save.getRegion() == null) return;
        evilMobs.removeIf(e -> !e.isValid());
        for (Player player : save.getRegion().toWorld().getPlayers()) {
            if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
                continue;
            }
            if (!player.hasPermission("easter.hunt")) continue;
            if (!save.getRegion().contains(player.getLocation())) continue;
            User user = save.userOf(player.getUniqueId());
            tickPlayer(player, user);
            long now = System.currentTimeMillis();
            if (now > user.getSpawnCooldown() && spawnEvilMob(player)) {
                user.setSpawnCooldown(now + 1000L * 30L);
            }
        }
    }

    ItemStack randomEasterEgg() {
        List<Mytems> mytemsList = Arrays
            .asList(Mytems.BLUE_EASTER_EGG,
                    Mytems.GREEN_EASTER_EGG,
                    Mytems.ORANGE_EASTER_EGG,
                    Mytems.PINK_EASTER_EGG,
                    Mytems.PURPLE_EASTER_EGG,
                    Mytems.YELLOW_EASTER_EGG);
        Mytems mytems = mytemsList.get(random.nextInt(mytemsList.size()));
        return mytems.createItemStack();
    }

    void tickPlayer(Player player, User user) {
        Vec3i currentEgg = user.getCurrentEgg();
        if (Timer.getEasterDay() == 0) return;
        if (currentEgg != null) {
            EasterEgg easterEgg = easterEggMap.get(currentEgg);
            if (easterEgg != null && easterEgg.itemFrame.isValid()) {
                return;
            }
            // Spawn the egg
            getLogger().info("Spawning egg for " + player.getName() + " at " + currentEgg);
            Block block = currentEgg.toBlock(save.getRegion().toWorld());
            Location location = block.getLocation().add(0.5, 0.0, 0.5).setDirection(new Vector(0.0, 1.0, 0.0));
            ItemFrame itemFrame;
            try {
                itemFrame = location.getWorld().spawn(location, ItemFrame.class, e -> {
                        e.setFacingDirection(BlockFace.UP);
                        e.setPersistent(false);
                        e.setItem(randomEasterEgg());
                        e.setItemDropChance(0.0f);
                        e.setVisible(false);
                        Rotation[] rotations = Rotation.values();
                        Rotation rotation = rotations[random.nextInt(rotations.length)];
                        e.setRotation(rotation);
                    });
            } catch (IllegalArgumentException iae) {
                // Happens above fence posts?
                user.setCurrentEgg(null);
                return;
            }
            easterEgg = new EasterEgg(player.getUniqueId(), itemFrame);
            easterEggMap.put(currentEgg, easterEgg);
            return;
        }
        // Current egg is null!
        long now = System.currentTimeMillis();
        if (user.getRegularEggsDiscovered() < Timer.getTotalEggs() || user.getEggCooldown() < now) {
            // Find a new egg spot. If this fails, try again next "tick"
            final int dx = random.nextInt(save.getRegion().getCuboid().getSizeX());
            final int dz = random.nextInt(save.getRegion().getCuboid().getSizeZ());
            final int x = save.getRegion().getCuboid().getMin().getX() + dx;
            final int z = save.getRegion().getCuboid().getMin().getZ() + dz;
            World world = save.getRegion().toWorld();
            int lowest = save.getRegion().getCuboid().getMin().getY();
            int highest = Math.min(world.getHighestBlockYAt(x, z), save.getRegion().getCuboid().getMax().getY());
            List<Vec3i> vectors = new ArrayList<>();
            for (int y = lowest; y <= highest; y += 1) {
                Block block = world.getBlockAt(x, y, z);
                if (!block.isSolid()) continue;
                Block above = block.getRelative(0, 1, 0);
                if (!above.isEmpty()) continue;
                if (above.getLightFromSky() == 0 && above.getLightFromBlocks() < 3) continue;
                Vec3i vector = Vec3i.of(above);
                if (save.userHasCurrentEgg(vector)) continue;
                vectors.add(vector);
            }
            if (vectors.isEmpty()) return;
            Vec3i vector = vectors.get(random.nextInt(vectors.size()));
            user.setCurrentEgg(vector);
            user.setEggCooldown(0L);
            save();
        }
    }

    boolean spawnEvilMob(Player player) {
        Location location = player.getLocation();
        for (Entity evilMob : evilMobs) {
            if (evilMob.getLocation().distanceSquared(location) < 256.0) {
                return false;
            }
        }
        double angle = random.nextDouble() * Math.PI * 2.0;
        double dx = Math.cos(angle) * 16.0;
        double dz = Math.sin(angle) * 16.0;
        location = location.add(dx, 0, dz);
        Block block = location.getBlock();
        while (block.isPassable() && block.getY() > 0) block = block.getRelative(0, -1, 0);
        while (!block.isEmpty() && block.getY() < 255) block = block.getRelative(0, 1, 0);
        if (!block.isPassable()) {
            return false;
        }
        if (block.getLightFromSky() == 0 && block.getLightFromBlocks() < 3) {
            return false;
        }
        if (!block.getRelative(0, -1, 0).isSolid()) {
            return false;
        }
        if (!save.getRegion().contains(block)) {
            return false;
        }
        location = block.getLocation().add(0.5, 0.0, 0.5);
        Mob entity;
        if (random.nextBoolean()) {
            entity = location.getWorld().spawn(location, Bee.class, e -> {
                    e.setPersistent(false);
                    e.setRemoveWhenFarAway(false);
                    e.setAdult();
                    e.setAnger(999999);
                    e.setCannotEnterHiveTicks(999999);
                    e.setHealth(1.0);
                });
        } else {
            entity = location.getWorld().spawn(location, Rabbit.class, e -> {
                    e.setPersistent(false);
                    e.setRemoveWhenFarAway(false);
                    e.setAdult();
                    e.setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
                    e.setHealth(1.0);
                });
        }
        entity.setTarget(player);
        evilMobs.add(entity);
        getLogger().info("Spawned " + entity.getType() + " for " + player.getName() + " at " + Vec3i.of(block));
        return true;
    }

    public int reloadChunks() {
        for (World world : Bukkit.getWorlds()) {
            world.removePluginChunkTickets(this);
        }
        if (save.getRegion() != null) {
            return loadChunks(save.getRegion());
        }
        return 0;
    }

    public int loadChunks(Region region) {
        World world = region.toWorld();
        if (world == null) {
            getLogger().warning("World not found: " + region.getWorld());
            return 0;
        }
        int ax = region.getCuboid().getMin().getX() >> 4;
        int bx = region.getCuboid().getMax().getX() >> 4;
        int az = region.getCuboid().getMin().getZ() >> 4;
        int bz = region.getCuboid().getMax().getZ() >> 4;
        int count = 0;
        for (int z = az; z <= bz; z += 1) {
            for (int x = ax; x <= bx; x += 1) {
                boolean success = world.addPluginChunkTicket(x, z, this);
                if (success) count += 1;
            }
        }
        getLogger().info(region.getWorld() + ": " + count + " chunks loaded");
        return count;
    }

    @EventHandler
    void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (save.getRegion() == null) return;
        if (!(event.getRightClicked() instanceof ItemFrame)) return;
        ItemFrame itemFrame = (ItemFrame) event.getRightClicked();
        if (!Objects.equals(itemFrame.getWorld(), save.getRegion().toWorld())) return;
        Vec3i vector = Vec3i.of(itemFrame.getLocation());
        EasterEgg easterEgg = easterEggMap.get(vector);
        if (easterEgg == null) return;
        event.setCancelled(true);
    }

    @EventHandler
    void onHangingBreak(HangingBreakEvent event) {
        if (save.getRegion() == null) return;
        if (!(event.getEntity() instanceof ItemFrame)) return;
        ItemFrame itemFrame = (ItemFrame) event.getEntity();
        if (!Objects.equals(itemFrame.getWorld(), save.getRegion().toWorld())) return;
        Vec3i vector = Vec3i.of(itemFrame.getLocation());
        EasterEgg easterEgg = easterEggMap.get(vector);
        if (easterEgg == null) return;
        event.setCancelled(true);
    }

    static String th(int in) {
        String out = Integer.toString(in);
        switch (out.charAt(out.length() - 1)) {
        case '1':
            return out.concat(out.endsWith("11") ? "th" : "st");
        case '2':
            return out.concat(out.endsWith("12") ? "th" : "nd");
        case '3':
            return out.concat(out.endsWith("13") ? "th" : "rd");
        default:
            return out.concat("th");
        }
    }

    @EventHandler
    void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (save.getRegion() == null) return;
        if (!Objects.equals(event.getEntity().getWorld(), save.getRegion().toWorld())) return;
        if (event.getEntity() instanceof ItemFrame) {
            ItemFrame itemFrame = (ItemFrame) event.getEntity();
            Vec3i vector = Vec3i.of(itemFrame.getLocation());
            EasterEgg easterEgg = easterEggMap.get(vector);
            if (easterEgg == null) return;
            event.setCancelled(true);
            if (!(event.getDamager() instanceof Player)) return;
            Player player = (Player) event.getDamager();
            if (!(player.hasPermission("easter.hunt"))) return;
            if (!Objects.equals(player.getUniqueId(), easterEgg.owner)) {
                String name = PlayerCache.nameForUuid(easterEgg.owner);
                player.sendMessage(Component.text("This egg belongs to " + name + "!").color(NamedTextColor.RED));
                return;
            }
            // Drop item frame
            easterEggMap.remove(vector);
            Location location = vector.toBlock(itemFrame.getWorld()).getLocation().add(0.5, 0.0, 0.5);
            itemFrame.getWorld().dropItem(location, itemFrame.getItem().clone(), e -> {
                    e.setOwner(player.getUniqueId());
                });
            itemFrame.remove();
            // Update user
            User user = save.userOf(player.getUniqueId());
            user.setCurrentEgg(null);
            int totalEggsDiscovered = user.getTotalEggsDiscovered() + 1;
            user.setTotalEggsDiscovered(totalEggsDiscovered);
            int regularEggsDiscovered = user.getRegularEggsDiscovered();
            int totalEggs = Timer.getTotalEggs();
            if (regularEggsDiscovered < totalEggs) {
                regularEggsDiscovered += 1;
                user.setRegularEggsDiscovered(regularEggsDiscovered);
            }
            if (regularEggsDiscovered < totalEggs) {
                player.sendMessage(Component.text("You discovered your " + th(totalEggsDiscovered) + " egg! A new egg will spawn soon.")
                                   .color(NamedTextColor.GREEN));
            } else {
                user.setEggCooldown(System.currentTimeMillis() + 10L * 60L * 1000L);
                player.sendMessage(Component.text("You discovered your " + th(totalEggsDiscovered) + " egg! A new egg will spawn in 10 minutes.")
                                   .color(NamedTextColor.GREEN));
            }
            save();
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.2f, 2.0f);
            Fireworks.spawnFirework(location);
            Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(255, 105, 180), 1.5f);
            location.getWorld().spawnParticle(Particle.REDSTONE, location, 16, .25, .25, .25, 0, dust);
            if (random.nextBoolean()) {
                int count = 1 + random.nextInt(3);
                for (int i = 0; i < count; i += 1) {
                    location.getWorld().spawn(location, Sheep.class, e -> {
                            e.setPersistent(false);
                            e.setRemoveWhenFarAway(true);
                            e.setColor(DyeColor.PINK);
                            e.setBaby();
                        });
                }
            } else {
                int count = 1 + random.nextInt(5);
                for (int i = 0; i < count; i += 1) {
                    location.getWorld().spawn(location, Rabbit.class, e -> {
                            e.setPersistent(false);
                            e.setRemoveWhenFarAway(true);
                            List<Rabbit.Type> types = new ArrayList<>(Arrays.asList(Rabbit.Type.values()));
                            types.remove(Rabbit.Type.THE_KILLER_BUNNY);
                            Rabbit.Type type = types.get(random.nextInt(types.size()));
                            e.setRabbitType(type);
                            e.setBaby();
                        });
                }
            }
            return;
        }
        if (event.getDamager().getType() == EntityType.FIREWORK) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            event.setDamage(0.0);
            final Entity damager = event.getDamager();
            switch (damager.getType()) {
            case RABBIT:
            case BEE:
                PotionEffect potionEffect = new PotionEffect(PotionEffectType.LEVITATION, 100, 0, true, true, true);
                player.addPotionEffect(potionEffect);
                Bukkit.getScheduler().runTaskLater(this, () -> {
                        if (damager.isValid()) {
                            Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(255, 105, 180), 1.5f);
                            damager.getWorld().spawnParticle(Particle.REDSTONE, damager.getLocation(), 16, .25, .25, .25, 0, dust);
                            damager.remove();
                        }
                    }, 20L);
                break;
            default:
                break;
            }
            return;
        }
    }

    @EventHandler
    void onPlayerSidebar(PlayerSidebarEvent event) {
        if (save.getRegion() == null) return;
        Player player = event.getPlayer();
        if (!(player.hasPermission("easter.hunt"))) return;
        if (Timer.getEasterDay() == 0) return;
        User user = save.userOf(player.getUniqueId());
        Vec3i currentEgg = user.getCurrentEgg();
        List<String> lines = new ArrayList<>();
        if (currentEgg != null) {
            lines.add(ChatColor.GREEN + "Easter Egg Ready!");
            if (save.getRegion().contains(player.getLocation())) {
                int distance = Vec3i.of(player.getLocation()).distanceSquared(currentEgg);
                if (distance < 12 * 12) {
                    lines.add(ChatColor.GREEN + "Hint " + ChatColor.GOLD + ChatColor.BOLD + "HOT");
                } else if (distance < 24 * 24) {
                    lines.add(ChatColor.GREEN + "Hint " + ChatColor.GOLD + ChatColor.ITALIC + "Warmer");
                } else if (distance < 48 * 48) {
                    lines.add(ChatColor.GREEN + "Hint " + ChatColor.YELLOW + ChatColor.ITALIC + "Warm");
                } else {
                    lines.add(ChatColor.GREEN + "Hint " + ChatColor.AQUA + ChatColor.ITALIC + "Cold");
                }
            } else {
                lines.add(ChatColor.GREEN + "Visit the Easter World!");
            }
        } else if (user.getEggCooldown() == 0L) {
            lines.add(ChatColor.GREEN + "Easter Egg Ready!");
            lines.add(ChatColor.GREEN + "Visit the Easter World!");
        } else {
            long duration = Math.max(0L, user.getEggCooldown() - System.currentTimeMillis());
            long seconds = duration / 1000L;
            long minutes = seconds / 60L;
            seconds %= 60L;
            lines.add(ChatColor.GREEN + "Easter Egg in "
                      + ChatColor.WHITE + minutes + ChatColor.GRAY + "m "
                      + ChatColor.WHITE + seconds + ChatColor.GRAY + "s");
        }
        if (!lines.isEmpty()) {
            event.addLines(this, Priority.HIGH, lines);
        }
    }

    @EventHandler
    void onPlayerCanBuild(PlayerCanBuildEvent event) {
        if (save.getRegion() == null) return;
        if (save.getRegion().contains(event.getBlock())) {
            event.setCancelled(true);
        }
    }
}
