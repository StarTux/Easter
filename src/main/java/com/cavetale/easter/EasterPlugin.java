package com.cavetale.easter;

import com.cavetale.core.event.block.PlayerBlockAbilityQuery;
import com.cavetale.core.event.entity.PlayerEntityAbilityQuery;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.core.util.Json;
import com.cavetale.easter.struct.Region;
import com.cavetale.easter.struct.Save;
import com.cavetale.easter.struct.User;
import com.cavetale.easter.util.Fireworks;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.easter.EasterEggColor;
import com.cavetale.mytems.util.Entities;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import net.kyori.adventure.text.Component;
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
import org.bukkit.block.data.Waterlogged;
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
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import static com.cavetale.easter.util.EasterText.easterify;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

public final class EasterPlugin extends JavaPlugin implements Listener {
    protected static EasterPlugin instance;
    protected Save save;
    protected Map<Vec3i, EasterEgg> easterEggMap = new HashMap<>();
    protected List<Entity> evilMobs = new ArrayList<>();
    protected Random random = new Random();
    protected Trades trades = new Trades(this);

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
        new EasterCommand(this).enable();
        trades.enable();
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

    protected void save() {
        getDataFolder().mkdirs();
        Json.save(new File(getDataFolder(), "save.json"), save, true);
    }

    private void tickPlayers() {
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
            // long now = System.currentTimeMillis();
            // if (now > user.getSpawnCooldown() && spawnEvilMob(player)) {
            //     user.setSpawnCooldown(now + 1000L * 30L);
            // }
        }
    }

    private EasterEggColor randomEasterEggColor() {
        EasterEggColor[] colors = EasterEggColor.values();
        return colors[random.nextInt(colors.length)];
    }

    private static boolean isLiquid(Block block) {
        switch (block.getType()) {
        case KELP:
        case SEAGRASS:
            return true;
        default:
            return block.isLiquid()
                || (block.getBlockData() instanceof Waterlogged waterlogged
                    && waterlogged.isWaterlogged());
        }
    }

    private void tickPlayer(Player player, User user) {
        if (Timer.getEasterDay() == 0) return;
        Vec3i currentEgg = user.getCurrentEgg();
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
            EasterEggColor color = randomEasterEggColor();
            try {
                itemFrame = location.getWorld().spawn(location, ItemFrame.class, e -> {
                        e.setFacingDirection(BlockFace.UP);
                        e.setPersistent(false);
                        Entities.setTransient(e);
                        e.setItem(color.basketMytems.createItemStack());
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
            easterEgg = new EasterEgg(player.getUniqueId(), itemFrame, color);
            easterEggMap.put(currentEgg, easterEgg);
            return;
        }
        // Current egg is null!
        long now = System.currentTimeMillis();
        if (user.getEggCooldown() < now) {
            // Find a new egg spot. If this fails, try again next "tick"
            final int dx = random.nextInt(save.getRegion().getCuboid().getSizeX());
            final int dz = random.nextInt(save.getRegion().getCuboid().getSizeZ());
            final int x = save.getRegion().getCuboid().getMin().getX() + dx;
            final int z = save.getRegion().getCuboid().getMin().getZ() + dz;
            World world = save.getRegion().toWorld();
            int lowest = save.getRegion().getCuboid().getMin().getY();
            int highest = Math.min(world.getHighestBlockYAt(x, z), save.getRegion().getCuboid().getMax().getY());
            List<Vec3i> vectors = new ArrayList<>();
            // Find solid blocks to put egg above.
            for (int y = lowest; y <= highest; y += 1) {
                Block block = world.getBlockAt(x, y, z);
                Collection<BoundingBox> blockBB = block.getCollisionShape().getBoundingBoxes();
                if (blockBB.size() != 1) continue;
                BoundingBox bb = blockBB.iterator().next();
                if (bb.getWidthX() != 1.0 || bb.getHeight() != 1.0 || bb.getWidthZ() != 1.0) {
                    continue;
                }
                Block above = block.getRelative(0, 1, 0);
                if (isLiquid(above)) continue;
                if (!above.getCollisionShape().getBoundingBoxes().isEmpty()) continue;
                if (above.getLightFromSky() < 8) continue;
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

    private boolean spawnEvilMob(Player player) {
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
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
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
    private void onHangingBreak(HangingBreakEvent event) {
        if (save.getRegion() == null) return;
        if (!(event.getEntity() instanceof ItemFrame)) return;
        ItemFrame itemFrame = (ItemFrame) event.getEntity();
        if (!Objects.equals(itemFrame.getWorld(), save.getRegion().toWorld())) return;
        Vec3i vector = Vec3i.of(itemFrame.getLocation());
        EasterEgg easterEgg = easterEggMap.get(vector);
        if (easterEgg == null) return;
        event.setCancelled(true);
    }

    private static String th(int in) {
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
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!Objects.equals(event.getEntity().getWorld(), save.getRegion().toWorld())) return;
        if (event.getDamager() instanceof Player player) {
            if (save.getRegion() == null) return;
            Vec3i vector = Vec3i.of(event.getEntity().getLocation());
            onHitBlock(player, vector);
            event.setCancelled(true);
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

    @EventHandler(ignoreCancelled = false)
    private void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (!event.hasBlock()) return;
        if (save.getRegion() == null) return;
        Block block = event.getClickedBlock();
        if (!Objects.equals(block.getWorld(), save.getRegion().toWorld())) return;
        onHitBlock(event.getPlayer(), Vec3i.of(block));
    }

    private void onHitBlock(Player player, Vec3i vector) {
        EasterEgg easterEgg = easterEggMap.get(vector);
        if (easterEgg == null) return;
        if (!(player.hasPermission("easter.hunt"))) return;
        if (!Objects.equals(player.getUniqueId(), easterEgg.owner)) {
            String name = PlayerCache.nameForUuid(easterEgg.owner);
            player.sendMessage(text("This egg belongs to " + name + "!").color(RED));
            return;
        }
        // Drop item frame
        easterEggMap.remove(vector);
        Location location = vector.toBlock(player.getWorld()).getLocation().add(0.5, 0.0, 0.5);
        location.getWorld().dropItem(location, easterEgg.color.eggMytems.createItemStack(), e -> {
                e.setOwner(player.getUniqueId());
            });
        easterEgg.itemFrame.remove();
        // Update user
        User user = save.userOf(player.getUniqueId());
        user.setCurrentEgg(null);
        final int totalEggsDiscovered = user.getTotalEggsDiscovered() + 1;
        user.setTotalEggsDiscovered(totalEggsDiscovered);
        int regularEggsDiscovered = user.getRegularEggsDiscovered();
        final int totalEggs = Timer.getTotalEggs();
        if (regularEggsDiscovered < totalEggs) {
            regularEggsDiscovered += 1;
            user.setRegularEggsDiscovered(regularEggsDiscovered);
        }
        if (regularEggsDiscovered < totalEggs) {
            user.setEggCooldown(System.currentTimeMillis() + Duration.ofSeconds(10).toMillis());
            user.setDailyExtraEggs(0);
            player.sendMessage(text("You discovered your " + th(totalEggsDiscovered) + " egg! A new egg will spawn soon.")
                               .color(GREEN));
        } else {
            int extra = user.getDailyExtraEggs();
            int minutes = 5 + extra;
            user.setDailyExtraEggs(extra + 1);
            user.setEggCooldown(System.currentTimeMillis() + Duration.ofMinutes(minutes).toMillis());
            player.sendMessage(text("You discovered your " + th(totalEggsDiscovered) + " egg! A new egg will spawn in " + minutes + " minutes.")
                               .color(GREEN));
        }
        player.sendActionBar(empty());
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
    }

    @EventHandler
    private void onPlayerHud(PlayerHudEvent event) {
        if (save.getRegion() == null) return;
        Player player = event.getPlayer();
        if (!(player.hasPermission("easter.hunt"))) return;
        if (Timer.getEasterDay() == 0) return;
        User user = save.userOf(player.getUniqueId());
        Vec3i currentEgg = user.getCurrentEgg();
        List<Component> lines = new ArrayList<>();
        if (currentEgg != null) {
            lines.add(textOfChildren(Mytems.EASTER_EGG, easterify("Easter Egg Ready!")));
            Location playerLocation = player.getLocation();
            if (save.getRegion().contains(playerLocation)) {
                Vec3i playerVector = Vec3i.of(playerLocation);
                int distance = playerVector.distanceSquared(currentEgg);
                if (distance < 16 * 16) {
                    Component msg = text("HOT", GOLD, BOLD);
                    lines.add(textOfChildren(Mytems.EASTER_EGG, text("Hint ", GREEN), msg));
                    player.sendActionBar(msg);
                    Location loc = currentEgg.toBlock(player.getWorld()).getLocation().add(0.5, 0.5, 0.5);
                    player.spawnParticle(Particle.SPELL_MOB, loc, 1, 0.25, 0.25, 0.25, 1.0);
                } else if (distance < 32 * 32) {
                    Component msg = text("Warmer", GOLD, ITALIC);
                    lines.add(textOfChildren(Mytems.EASTER_EGG, text("Hint ", GREEN), msg));
                    player.sendActionBar(msg);
                } else if (distance < 64 * 64) {
                    Component msg = text("Warm", YELLOW, ITALIC);
                    lines.add(textOfChildren(Mytems.EASTER_EGG, text("Hint ", GREEN), msg));
                    player.sendActionBar(msg);
                } else {
                    Vec3i direct = currentEgg.subtract(playerVector);
                    List<String> messages = new ArrayList<>();
                    final int min = 32;
                    if (direct.z > min) {
                        messages.add("South");
                    } else if (direct.z < -min) {
                        messages.add("North");
                    }
                    if (direct.x > min) {
                        messages.add("East");
                    } else if (direct.x < -min) {
                        messages.add("West");
                    }
                    if (messages.isEmpty()) {
                        messages.add("Cold");
                    }
                    lines.add(textOfChildren(Mytems.EASTER_EGG, text("Hint ", GREEN), text(String.join(" ", messages), AQUA, ITALIC)));
                    Vector playerDirection = playerLocation.getDirection();
                    double playerAngle = Math.atan2(playerDirection.getZ(), playerDirection.getX());
                    double targetAngle = Math.atan2((double) direct.z, (double) direct.x);
                    if (Double.isFinite(playerAngle) && Double.isFinite(targetAngle)) {
                        double angle = targetAngle - playerAngle;
                        if (angle > Math.PI) angle -= 2.0 * Math.PI;
                        if (angle < -Math.PI) angle += 2.0 * Math.PI;
                        if (angle < Math.PI * -0.5) {
                            player.sendActionBar(Mytems.ARROW_LEFT.component);
                        } else if (angle > Math.PI * 0.5) {
                            player.sendActionBar(Mytems.ARROW_RIGHT.component);
                        } else {
                            player.sendActionBar(Mytems.ARROW_UP.component);
                        }
                    }
                }
            } else {
                lines.add(textOfChildren(Mytems.EASTER_EGG, text("Visit the Easter World!", GREEN)));
            }
        } else if (user.getEggCooldown() < System.currentTimeMillis()) {
            lines.add(textOfChildren(Mytems.EASTER_EGG, easterify("Easter Egg Ready!")));
            lines.add(textOfChildren(Mytems.EASTER_EGG, text("Visit the Easter World!", GREEN)));
        } else {
            long duration = Math.max(0L, user.getEggCooldown() - System.currentTimeMillis());
            long seconds = duration / 1000L;
            long minutes = seconds / 60L;
            seconds %= 60L;
            lines.add(textOfChildren(Mytems.EASTER_EGG, easterify("Easter Egg")));
            lines.add(textOfChildren(Mytems.EASTER_EGG, text("in ", GREEN),
                                     text(minutes, WHITE), text("m ", GRAY),
                                     text(seconds, WHITE), text("s", GRAY)));
        }
        if (!lines.isEmpty()) {
            event.sidebar(PlayerHudPriority.HIGH, lines);
        }
    }

    @EventHandler
    private void onPlayerBlockAbility(PlayerBlockAbilityQuery query) {
        if (save.getRegion() == null) return;
        if (save.getRegion().contains(query.getBlock())) {
            switch (query.getAction()) {
            case FLY: return;
            default:
                query.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void onPlayerEntityAbiltiy(PlayerEntityAbilityQuery query) {
        if (save.getRegion() == null) return;
        if (save.getRegion().contains(query.getEntity().getLocation())) {
            query.setCancelled(true);
        }
    }
}
