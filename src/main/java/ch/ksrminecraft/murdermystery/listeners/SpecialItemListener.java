package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.effects.ItemManager;
import ch.ksrminecraft.murdermystery.model.Role;
import ch.ksrminecraft.murdermystery.managers.game.RoleManager;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class SpecialItemListener implements Listener {

    private final MurderMystery plugin;

    public SpecialItemListener(MurderMystery plugin) {
        this.plugin = plugin;
    }

    // ---------- Drop verhindern ----------
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack drop = event.getItemDrop().getItemStack();
        Player player = event.getPlayer();

        // Detective-Bogen
        if (ItemManager.isDetectiveBow(drop)) {
            if (plugin.getArenaManager().getArenaForWorld(player.getWorld()) == null) {
                event.getItemDrop().remove();
                plugin.debug("Detective-Bogen außerhalb von Arenen entfernt (Welt=" + player.getWorld().getName() + ")");
                return;
            }
            event.setCancelled(true);
            return;
        }

        // Murderer-Schwert
        if (ItemManager.isMurdererSword(drop)) {
            if (plugin.getArenaManager().getArenaForWorld(player.getWorld()) == null) {
                event.getItemDrop().remove();
                plugin.debug("Murderer-Schwert außerhalb von Arenen entfernt (Welt=" + player.getWorld().getName() + ")");
                return;
            }
            event.setCancelled(true);
            player.sendMessage("§cDu darfst das Murderer-Schwert nicht fallen lassen!");
            return;
        }

        // Pfeile (Detective)
        if (drop.getType() == Material.ARROW) {
            Role role = RoleManager.getRole(player.getUniqueId());
            if (role == Role.DETECTIVE) {
                event.setCancelled(true);
                player.sendMessage("§cAls Detective darfst du deine Pfeile nicht droppen!");
            }
        }
    }

    // ---------- Pickup regeln ----------
    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack stack = event.getItem().getItemStack();

        // Murderer-Schwert nur für Mörder
        if (ItemManager.isMurdererSword(stack)) {
            Role role = RoleManager.getRole(player.getUniqueId());
            if (role != Role.MURDERER) {
                event.setCancelled(true);
                player.sendMessage("§cNur der Mörder darf das Schwert aufnehmen!");
                plugin.debug("Pickup von Murderer-Schwert durch " + player.getName() + " blockiert.");
            }
        }
    }

    // ---------- Despawn verhindern ----------
    @EventHandler
    public void onDespawn(ItemDespawnEvent event) {
        Item item = event.getEntity();
        if (!plugin.getGameManager().isGameStarted()) return;

        if (ItemManager.isDetectiveBow(item.getItemStack()) || ItemManager.isMurdererSword(item.getItemStack())) {
            event.setCancelled(true);
            item.setUnlimitedLifetime(true);
            item.setTicksLived(1);
            plugin.debug("Spezialitem vor Despawn geschützt: " + item.getItemStack().getType());
        }
    }

    // ---------- Inventarbewegung blockieren ----------
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack current = event.getCurrentItem();
        if (ItemManager.isDetectiveBow(current) || ItemManager.isMurdererSword(current)) {
            event.setCancelled(true);
            player.sendMessage("§cDieses Spezialitem darf nicht bewegt werden!");
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack dragged = event.getOldCursor();
        if (ItemManager.isDetectiveBow(dragged) || ItemManager.isMurdererSword(dragged)) {
            event.setCancelled(true);
            player.sendMessage("§cDieses Spezialitem darf nicht bewegt werden!");
        }
    }

    // ---------- /clear blockieren ----------
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        String msg = event.getMessage().toLowerCase();

        if (msg.startsWith("/clear") || msg.startsWith("/minecraft:clear")) {
            if (p.getInventory().contains(ItemManager.createDetectiveBow().getType()) ||
                    p.getInventory().contains(ItemManager.createMurdererSword().getType())) {
                event.setCancelled(true);
                p.sendMessage("§cDu darfst dein Inventar nicht leeren, solange du Spezialitems besitzt!");
                plugin.debug("Befehl /clear von " + p.getName() + " blockiert (Spezialitem im Inventar).");
            }
        }
    }
}
