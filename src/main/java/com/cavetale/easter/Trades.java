package com.cavetale.easter;

import com.cavetale.easter.struct.User;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.easter.EasterEggColor;
import com.cavetale.worldmarker.util.Tags;
import com.winthier.title.Title;
import com.winthier.title.TitlePlugin;
import io.papermc.paper.event.player.PlayerPurchaseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import static com.cavetale.easter.util.EasterText.easterify;
import static com.cavetale.mytems.util.Items.tooltip;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class Trades implements Listener {
    private final EasterPlugin plugin;
    private static NamespacedKey titleKey;

    protected void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        titleKey = new NamespacedKey(plugin, "title");
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerPurchase(PlayerPurchaseEvent event) {
        Player player = event.getPlayer();
        ItemStack result = event.getTrade().getResult();
        if (triggerTitle(player, result)) return;
        triggerMytemsPurchase(player, result);
    }

    private boolean triggerTitle(Player player, ItemStack result) {
        if (!result.hasItemMeta()) return false;
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String name = Tags.getString(pdc, titleKey);
        if (name == null) return false;
        Title title = TitlePlugin.getInstance().getTitle(name);
        if (title == null) return false;
        String cmd = "titles unlockset " + player.getName() + " " + title.getName();
        plugin.getLogger().info("Running command " + cmd);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        return true;
    }

    private void triggerMytemsPurchase(Player player, ItemStack result) {
        Mytems mytems = Mytems.forItem(result);
        if (mytems == null) return;
        User user = plugin.save.userOf(player.getUniqueId());
        if (user.getPurchasedItems().add(mytems)) {
            plugin.save();
        }
    }

    public static Merchant makeEggMerchant() {
        EasterEggColor[] colors = EasterEggColor.values();
        Component title = join(noSeparators(),
                               colors[ThreadLocalRandom.current().nextInt(colors.length)].eggMytems.component,
                               easterify("Easter Egg Merchant"));
        Merchant merchant = Bukkit.createMerchant(title);
        List<MerchantRecipe> recipes = new ArrayList<>();
        for (EasterEggColor color : colors) {
            MerchantRecipe recipe = new MerchantRecipe(Mytems.EASTER_TOKEN.createItemStack(), 999);
            recipe.setExperienceReward(false);
            recipe.setIgnoreDiscounts(true);
            recipe.setIngredients(List.of(color.eggMytems.createItemStack(3)));
            recipes.add(recipe);
        }
        merchant.setRecipes(recipes);
        return merchant;
    }

    public Merchant makeTokenMerchant(Player player) {
        Component title = join(noSeparators(),
                               Mytems.EASTER_TOKEN.component,
                               easterify("Easter Token Merchant"));
        Merchant merchant = Bukkit.createMerchant(title);
        List<MerchantRecipe> recipes = new ArrayList<>();
        recipes.add(makeTitleRecipe(player, 20, "EasterBunny"));
        recipes.add(makeTitleRecipe(player, 20, "EggHunter"));
        recipes.add(makeTokenRecipe(player, 10, Mytems.EASTER_HELMET, true));
        recipes.add(makeTokenRecipe(player, 10, Mytems.EASTER_CHESTPLATE, true));
        recipes.add(makeTokenRecipe(player, 10, Mytems.EASTER_LEGGINGS, true));
        recipes.add(makeTokenRecipe(player, 10, Mytems.EASTER_BOOTS, true));
        recipes.add(makeTokenRecipe(player, 20, Mytems.MAGIC_CAPE, true));
        recipes.add(makeTokenRecipe(player, 2, Mytems.KITTY_COIN, false));
        merchant.setRecipes(recipes);
        return merchant;
    }

    private MerchantRecipe makeTokenRecipe(Player player, int tokens, Mytems mytems, boolean unique) {
        UUID uuid = player.getUniqueId();
        int maxUses = unique
            ? (plugin.save.userOf(uuid).getPurchasedItems().contains(mytems) ? 0 : 1)
            : 999;
        ItemStack prize = mytems.createItemStack();
        MerchantRecipe recipe = new MerchantRecipe(prize, maxUses);
        recipe.setExperienceReward(false);
        recipe.setIgnoreDiscounts(true);
        recipe.setIngredients(List.of(Mytems.EASTER_TOKEN.createItemStack(tokens)));
        return recipe;
    }

    private MerchantRecipe makeTitleRecipe(Player player, int tokens, String name) {
        Title title = TitlePlugin.getInstance().getTitle(name);
        UUID uuid = player.getUniqueId();
        int maxUses = title != null && !TitlePlugin.getInstance().playerHasTitle(uuid, title)
            ? 1
            : 0;
        ItemStack prize = Mytems.RUBY.createItemStack();
        if (title != null) {
            prize = tooltip(prize, List.of(new Component[] {
                        title.getTitleComponent(),
                        text("" + title.getDescription(), DARK_GRAY),
                        easterify("Unlock this title!"),
                    }));
            prize.editMeta(meta -> {
                    PersistentDataContainer pdc = meta.getPersistentDataContainer();
                    Tags.set(pdc, titleKey, name);
                });
        }
        MerchantRecipe recipe = new MerchantRecipe(prize, maxUses);
        recipe.setExperienceReward(false);
        recipe.setIgnoreDiscounts(true);
        recipe.setIngredients(List.of(Mytems.EASTER_TOKEN.createItemStack(tokens)));
        return recipe;
    }
}
