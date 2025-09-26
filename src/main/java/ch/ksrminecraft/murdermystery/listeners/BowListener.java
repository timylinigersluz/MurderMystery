package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.effects.ItemManager;
import ch.ksrminecraft.murdermystery.model.Role;
import ch.ksrminecraft.murdermystery.managers.game.RoleManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class BowListener implements Listener {

    private final MurderMystery plugin;

    public BowListener(MurderMystery plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!plugin.getGameManager().isGameStarted()) return;
        if (!(event.getEntity() instanceof Player shooter)) return;
        if (!ItemManager.isDetectiveBow(event.getBow())) return;

        if (!ItemManager.canShoot(shooter)) {
            shooter.sendMessage(ChatColor.GOLD + "Du musst 3 Sekunden warten, bevor du wieder schießen kannst!");
            event.setCancelled(true);
        } else {
            shooter.playSound(shooter.getLocation(), Sound.BLOCK_BIG_DRIPLEAF_TILT_DOWN, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!plugin.getGameManager().isGameStarted()) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        if (!ItemManager.isDetectiveBow(shooter.getInventory().getItemInMainHand())) return;

        event.setDamage(0);
        Role victimRole = RoleManager.getRole(victim.getUniqueId());

        if (victimRole == Role.MURDERER) {
            victim.setHealth(0);
            Bukkit.broadcastMessage(ChatColor.BLUE + "Detective " + shooter.getName() + " hat den Mörder " + victim.getName() + " getötet!");
        } else {
            victim.setHealth(0);
            shooter.setHealth(0);
            RoleManager.setRole(shooter.getUniqueId(), Role.BYSTANDER);
            shooter.getWorld().dropItemNaturally(shooter.getLocation(), ItemManager.createDetectiveBow());
            Bukkit.broadcastMessage(ChatColor.RED + "Detective " + shooter.getName() + " hat " + victim.getName() + " (kein Mörder) getötet!");
        }
    }

    @EventHandler
    public void onBowPickup(EntityPickupItemEvent event) {
        if (!plugin.getGameManager().isGameStarted()) return;
        if (!(event.getEntity() instanceof Player p)) return;
        if (!ItemManager.isDetectiveBow(event.getItem().getItemStack())) return;

        Role role = RoleManager.getRole(p.getUniqueId());
        if (role == Role.MURDERER) {
            event.setCancelled(true);
            return;
        }

        if (role == Role.BYSTANDER) {
            RoleManager.setRole(p.getUniqueId(), Role.DETECTIVE);
            Bukkit.broadcastMessage(ChatColor.AQUA + p.getName() + " ist der neue Detective!");
        }
    }

    @EventHandler
    public void onDespawn(ItemDespawnEvent event) {
        if (!plugin.getGameManager().isGameStarted()) return;
        Item item = event.getEntity();
        if (ItemManager.isDetectiveBow(item.getItemStack())) {
            event.setCancelled(true);
            item.setUnlimitedLifetime(true);
            item.setTicksLived(1);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!plugin.getGameManager().isGameStarted()) return;
        if (ItemManager.isDetectiveBow(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.getGameManager().isGameStarted()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (ItemManager.isDetectiveBow(event.getCurrentItem())) {
            event.setCancelled(true);
            player.sendMessage("§cDer Detective-Bogen darf nicht bewegt werden!");
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!plugin.getGameManager().isGameStarted()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getOldCursor() != null && ItemManager.isDetectiveBow(event.getOldCursor())) {
            event.setCancelled(true);
            player.sendMessage("§cDer Detective-Bogen darf nicht bewegt werden!");
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getGameManager().isGameStarted()) return;
        Player p = event.getPlayer();
        String msg = event.getMessage().toLowerCase();

        if (msg.startsWith("/clear") || msg.startsWith("/minecraft:clear")) {
            if (p.getInventory().contains(ItemManager.createDetectiveBow().getType())) {
                event.setCancelled(true);
                p.sendMessage("§cDu darfst dein Inventar nicht leeren, solange du den Detective-Bogen besitzt!");
            }
        }
    }
}
