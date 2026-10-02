package com.soulswords;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class SwordManager {

    public static final int MAX_TIER = 6;
    public static final String[] NAMES = {"Wooden", "Stone", "Iron", "Golden", "Diamond", "Netherite", "Immortal"};
    public static final String[] POWERS = {"Speed", "Haste", "Resistance", "Regeneration", "Strength", "Fire Resistance"};
    private static final Material[] MATS = {
        Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD,
        Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.NETHERITE_SWORD
    };
    private static final NamedTextColor[] COLORS = {
        NamedTextColor.GOLD, NamedTextColor.GRAY, NamedTextColor.WHITE, NamedTextColor.YELLOW,
        NamedTextColor.AQUA, NamedTextColor.DARK_GRAY, NamedTextColor.LIGHT_PURPLE
    };
    private static final int[] DEFAULT_KILLS = {0, 10, 25, 50, 100, 200, 400};

    private final SoulSwords plugin;
    private final NamespacedKey tierKey;
    private final NamespacedKey killsKey;
    private int[] required = DEFAULT_KILLS;

    public SwordManager(SoulSwords plugin) {
        this.plugin = plugin;
        this.tierKey = new NamespacedKey(plugin, "tier");
        this.killsKey = new NamespacedKey(plugin, "kills");
        List<Integer> cfg = plugin.getConfig().getIntegerList("kills-required");
        if (cfg.size() == MAX_TIER + 1) {
            required = new int[cfg.size()];
            for (int i = 0; i < cfg.size(); i++) required[i] = cfg.get(i);
        }
    }

    public int killsFor(int tier) {
        return required[tier];
    }

    public int tierForKills(int kills) {
        int tier = 0;
        for (int i = 0; i < required.length; i++) {
            if (kills >= required[i]) tier = i;
        }
        return tier;
    }

    /** Returns -1 if the item is not a soul sword. */
    public int getTier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return -1;
        Integer t = item.getItemMeta().getPersistentDataContainer().get(tierKey, PersistentDataType.INTEGER);
        return t == null ? -1 : t;
    }

    public int getKills(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        Integer k = item.getItemMeta().getPersistentDataContainer().get(killsKey, PersistentDataType.INTEGER);
        return k == null ? 0 : k;
    }

    public int findSword(Player p) {
        ItemStack[] contents = p.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            if (getTier(contents[i]) >= 0) return i;
        }
        return -1;
    }

    public ItemStack build(int tier, int kills) {
        ItemStack item = new ItemStack(MATS[tier]);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(NAMES[tier] + " Soul Sword", COLORS[tier])
            .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(line("Tier " + tier + " / " + MAX_TIER, NamedTextColor.GRAY));
        if (tier < MAX_TIER) {
            lore.add(line("Kills: " + kills + " / " + required[tier + 1], NamedTextColor.GRAY));
        } else {
            lore.add(line("Kills: " + kills + " (MAX)", NamedTextColor.LIGHT_PURPLE));
        }
        lore.add(line("", NamedTextColor.GRAY));
        if (tier == 0) {
            lore.add(line("No powers yet. Get kills to level up!", NamedTextColor.DARK_GRAY));
        }
        for (int i = 0; i < tier; i++) {
            lore.add(line("+ " + POWERS[i] + " (while held)", NamedTextColor.GREEN));
        }
        if (tier == MAX_TIER) {
            lore.add(line("", NamedTextColor.GRAY));
            lore.add(line("EXECUTE: Right-click a player, then left-click", NamedTextColor.RED));
            lore.add(line("them to kill instantly. Cooldown: "
                + plugin.getConfig().getInt("execute-cooldown-seconds", 100) + "s", NamedTextColor.RED));
        }
        meta.lore(lore);

        meta.setUnbreakable(true);
        if (tier == MAX_TIER) meta.setEnchantmentGlintOverride(true);

        NamespacedKey model = NamespacedKey.fromString("soulswords:" + NAMES[tier].toLowerCase());
        if (model != null) meta.setItemModel(model);

        meta.getPersistentDataContainer().set(tierKey, PersistentDataType.INTEGER, tier);
        meta.getPersistentDataContainer().set(killsKey, PersistentDataType.INTEGER, kills);
        item.setItemMeta(meta);
        return item;
    }

    private Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
