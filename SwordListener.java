package com.soulswords;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SwordListener implements Listener {

    private final SoulSwords plugin;
    private final SwordManager sm;
    private final Map<UUID, UUID> marked = new HashMap<>();
    private final Map<UUID, Long> markExpiry = new HashMap<>();
    private final Map<UUID, Long> cooldownEnd = new HashMap<>();

    public SwordListener(SoulSwords plugin, SwordManager sm) {
        this.plugin = plugin;
        this.sm = sm;
    }

    // ---- First join: give the wooden sword ----
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!p.hasPlayedBefore() && sm.findSword(p) == -1) {
            p.getInventory().addItem(sm.build(0, 0));
            p.sendMessage(Component.text("You received a Wooden Soul Sword. Get kills to level it up!", NamedTextColor.GOLD));
        }
    }

    // ---- Count kills and level up ----
    @EventHandler
    public void onKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null || killer.equals(e.getEntity())) return;
        boolean countMobs = plugin.getConfig().getBoolean("count-mob-kills", false);
        if (e.getEntity() instanceof Player || countMobs) {
            addKill(killer);
        }
    }

    private void addKill(Player p) {
        int slot = sm.findSword(p);
        if (slot == -1) return;
        ItemStack old = p.getInventory().getItem(slot);
        int oldTier = sm.getTier(old);
        int kills = sm.getKills(old) + 1;
        int newTier = Math.max(oldTier, sm.tierForKills(kills));
        p.getInventory().setItem(slot, sm.build(newTier, kills));
        if (newTier > oldTier) {
            p.sendMessage(Component.text("Your sword evolved into the " + SwordManager.NAMES[newTier] + " Soul Sword!", NamedTextColor.LIGHT_PURPLE));
            if (newTier == SwordManager.MAX_TIER) {
                Bukkit.broadcast(Component.text(p.getName() + " reached the IMMORTAL Soul Sword!", NamedTextColor.RED));
            }
        }
    }

    // ---- Soulbound: keep on death, cannot drop ----
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        for (ItemStack item : new ArrayList<>(e.getDrops())) {
            if (sm.getTier(item) >= 0) {
                e.getDrops().remove(item);
                e.getItemsToKeep().add(item);
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (sm.getTier(e.getItemDrop().getItemStack()) >= 0) e.setCancelled(true);
    }

    // ---- Immortal: step 1, right-click a player to mark ----
    @EventHandler
    public void onRightClick(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!(e.getRightClicked() instanceof Player target)) return;
        Player p = e.getPlayer();
        if (sm.getTier(p.getInventory().getItemInMainHand()) != SwordManager.MAX_TIER) return;

        long now = System.currentTimeMillis();
        long end = cooldownEnd.getOrDefault(p.getUniqueId(), 0L);
        if (now < end) {
            long secs = (end - now) / 1000 + 1;
            p.sendActionBar(Component.text("Execute on cooldown: " + secs + "s", NamedTextColor.RED));
            return;
        }
        int markSecs = plugin.getConfig().getInt("mark-seconds", 10);
        marked.put(p.getUniqueId(), target.getUniqueId());
        markExpiry.put(p.getUniqueId(), now + markSecs * 1000L);
        p.sendActionBar(Component.text("Marked " + target.getName() + " - left-click them to execute!", NamedTextColor.RED));
    }

    // ---- Immortal: step 2, left-click the marked player to kill ----
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!(e.getEntity() instanceof Player target)) return;
        if (sm.getTier(p.getInventory().getItemInMainHand()) != SwordManager.MAX_TIER) return;

        UUID markedId = marked.get(p.getUniqueId());
        Long expiry = markExpiry.get(p.getUniqueId());
        long now = System.currentTimeMillis();
        if (markedId == null || expiry == null || now > expiry || !markedId.equals(target.getUniqueId())) return;
        if (now < cooldownEnd.getOrDefault(p.getUniqueId(), 0L)) return;
        if (target.getGameMode() == GameMode.CREATIVE || target.getGameMode() == GameMode.SPECTATOR) return;

        e.setCancelled(true);
        marked.remove(p.getUniqueId());
        markExpiry.remove(p.getUniqueId());
        int cd = plugin.getConfig().getInt("execute-cooldown-seconds", 100);
        cooldownEnd.put(p.getUniqueId(), now + cd * 1000L);

        target.setHealth(0.0);
        p.sendActionBar(Component.text("EXECUTED " + target.getName() + "! Cooldown: " + cd + "s", NamedTextColor.DARK_RED));
        addKill(p);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        marked.remove(e.getPlayer().getUniqueId());
        markExpiry.remove(e.getPlayer().getUniqueId());
    }
}
