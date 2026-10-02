package com.soulswords;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SoulSwords extends JavaPlugin implements CommandExecutor {

    private SwordManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new SwordManager(this);
        getServer().getPluginManager().registerEvents(new SwordListener(this, manager), this);
        getCommand("soulsword").setExecutor(this);

        // Passive powers: re-applied every second while the sword is in the main hand.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                ItemStack hand = p.getInventory().getItemInMainHand();
                int tier = manager.getTier(hand);
                if (tier <= 0) continue;
                PotionEffectType[] types = powerTypes();
                for (int i = 0; i < tier && i < types.length; i++) {
                    p.addPotionEffect(new PotionEffect(types[i], 60, 0, false, false, true));
                }
            }
        }, 20L, 20L);
    }

    public static PotionEffectType[] powerTypes() {
        return new PotionEffectType[] {
            PotionEffectType.SPEED,
            PotionEffectType.HASTE,
            PotionEffectType.RESISTANCE,
            PotionEffectType.REGENERATION,
            PotionEffectType.STRENGTH,
            PotionEffectType.FIRE_RESISTANCE
        };
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 2 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(Component.text("Usage: /soulsword give <player> [tier 0-6]", NamedTextColor.RED));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }
        int tier = 0;
        if (args.length >= 3) {
            try {
                tier = Math.max(0, Math.min(SwordManager.MAX_TIER, Integer.parseInt(args[2])));
            } catch (NumberFormatException ex) {
                sender.sendMessage(Component.text("Tier must be a number 0-6.", NamedTextColor.RED));
                return true;
            }
        }
        int kills = manager.killsFor(tier);
        target.getInventory().addItem(manager.build(tier, kills));
        sender.sendMessage(Component.text("Gave " + SwordManager.NAMES[tier] + " Soul Sword to " + target.getName(), NamedTextColor.GREEN));
        return true;
    }
}
