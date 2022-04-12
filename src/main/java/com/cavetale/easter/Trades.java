package com.cavetale.easter;

import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.easter.EasterEggColor;
import com.cavetale.mytems.util.Items;
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
import static net.kyori.adventure.text.Component.join;
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
        ItemStack result = event.getTrade().getResult();
        if (!result.hasItemMeta()) return;
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String name = Tags.getString(pdc, titleKey);
        if (name == null) return;
        Title title = TitlePlugin.getInstance().getTitle(name);
        if (title == null) return;
        Player player = event.getPlayer();
        String cmd = "titles unlockset " + player.getName() + " " + title.getName();
        plugin.getLogger().info("Running command " + cmd);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
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
            recipe.setIngredients(List.of(color.eggMytems.createItemStack(),
                                          color.eggMytems.createItemStack()));
            recipes.add(recipe);
        }
        merchant.setRecipes(recipes);
        return merchant;
    }

    public static Merchant makeTokenMerchant(Player player) {
        Component title = join(noSeparators(),
                               Mytems.EASTER_TOKEN.component,
                               easterify("Easter Token Merchant"));
        Merchant merchant = Bukkit.createMerchant(title);
        List<MerchantRecipe> recipes = new ArrayList<>();
        recipes.add(makeTitleRecipe(player, 20, "EasterBunny"));
        recipes.add(makeTitleRecipe(player, 20, "EggHunter"));
        recipes.add(makeTokenRecipe(10, Mytems.EASTER_HELMET.createItemStack()));
        recipes.add(makeTokenRecipe(10, Mytems.EASTER_CHESTPLATE.createItemStack()));
        recipes.add(makeTokenRecipe(10, Mytems.EASTER_LEGGINGS.createItemStack()));
        recipes.add(makeTokenRecipe(10, Mytems.EASTER_BOOTS.createItemStack()));
        recipes.add(makeTokenRecipe(2, Mytems.KITTY_COIN.createItemStack()));
        merchant.setRecipes(recipes);
        return merchant;
    }

    private static MerchantRecipe makeTokenRecipe(int tokens, ItemStack prize) {
        int maxUses = tokens <= 3 ? 999 : 1;
        MerchantRecipe recipe = new MerchantRecipe(prize, maxUses);
        recipe.setExperienceReward(false);
        recipe.setIgnoreDiscounts(true);
        recipe.setIngredients(List.of(Mytems.EASTER_TOKEN.createItemStack(tokens)));
        return recipe;
    }

    private static MerchantRecipe makeTitleRecipe(Player player, int tokens, String name) {
        Title title = TitlePlugin.getInstance().getTitle(name);
        UUID uuid = player.getUniqueId();
        int maxUses = title != null && !TitlePlugin.getInstance().playerHasTitle(uuid, title)
            ? 1
            : 0;
        ItemStack prize = Mytems.RUBY.createItemStack();
        if (title != null) {
            prize = Items.text(prize, List.of(new Component[] {
                        title.getTitleComponent(),
                        title.getTooltip(),
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
