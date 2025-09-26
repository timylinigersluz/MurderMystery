package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.effects.ItemManager;
import ch.ksrminecraft.murdermystery.model.Role;
import ch.ksrminecraft.murdermystery.managers.game.RoleManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class SwordListener implements Listener {

    private final MurderMystery plugin;

    public SwordListener(MurderMystery plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSwordHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        if (ItemManager.isMurdererSword(attacker.getInventory().getItemInMainHand())) {
            if (RoleManager.getRole(attacker.getUniqueId()) != Role.MURDERER) {
                event.setCancelled(true);
                attacker.sendMessage("§cNur der Mörder darf das Schwert benutzen!");
                return;
            }

            event.setDamage(0);
            victim.setHealth(0);

            broadcastKill("🔪 Murderer", attacker.getName(), victim.getName(), ChatColor.DARK_RED);
            plugin.debug("Murderer " + attacker.getName() + " hat " + victim.getName() + " mit dem Schwert getötet.");
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (!ItemManager.isMurdererSword(event.getItem().getItemStack())) return;

        Role role = RoleManager.getRole(p.getUniqueId());
        if (role != Role.MURDERER) {
            event.setCancelled(true);
            p.sendMessage("§cNur der Mörder darf das Schwert aufnehmen!");
            plugin.debug("Pickup von Murderer-Schwert durch " + p.getName() + " blockiert (kein Mörder).");
        }
    }

    @EventHandler
    public void onDespawn(ItemDespawnEvent event) {
        Item item = event.getEntity();
        if (ItemManager.isMurdererSword(item.getItemStack())) {
            event.setCancelled(true);
            item.setUnlimitedLifetime(true);
            item.setTicksLived(1);
            plugin.debug("Murderer-Schwert vor Despawn geschützt.");
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (ItemManager.isMurdererSword(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cDu darfst das Murderer-Schwert nicht fallen lassen!");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (ItemManager.isMurdererSword(event.getCurrentItem())) {
            event.setCancelled(true);
            player.sendMessage("§cDas Murderer-Schwert darf nicht bewegt werden!");
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getOldCursor() != null && ItemManager.isMurdererSword(event.getOldCursor())) {
            event.setCancelled(true);
            player.sendMessage("§cDas Murderer-Schwert darf nicht bewegt werden!");
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        String msg = event.getMessage().toLowerCase();

        if (msg.startsWith("/clear") || msg.startsWith("/minecraft:clear")) {
            if (p.getInventory().contains(ItemManager.createMurdererSword().getType())) {
                event.setCancelled(true);
                p.sendMessage("§cDu darfst dein Inventar nicht leeren, solange du das Murderer-Schwert besitzt!");
                plugin.debug("Befehl /clear von " + p.getName() + " blockiert (Murderer-Schwert im Inventar).");
            }
        }
    }

    private void broadcastKill(String role, String killer, String victim, ChatColor color) {
        String title = color + role + " " + killer;
        String subtitle = ChatColor.GRAY + "hat " + ChatColor.RED + victim + ChatColor.GRAY + " getötet!";
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(title, subtitle, 10, 60, 10);
            p.sendMessage(title + ChatColor.GRAY + " hat " + ChatColor.RED + victim + ChatColor.GRAY + " getötet!");
        }
    }
}
