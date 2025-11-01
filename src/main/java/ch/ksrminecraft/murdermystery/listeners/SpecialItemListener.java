package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.effects.ItemManager;
import ch.ksrminecraft.murdermystery.managers.game.GameManagerRegistry;
import ch.ksrminecraft.murdermystery.model.Arena;
import ch.ksrminecraft.murdermystery.model.ArenaGame;
import ch.ksrminecraft.murdermystery.model.Role;
import ch.ksrminecraft.murdermystery.utils.MessageLimiter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class SpecialItemListener implements Listener {

    private final MurderMystery plugin;
    private final GameManagerRegistry registry;

    public SpecialItemListener(MurderMystery plugin, GameManagerRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    // ---------- Drop verhindern ----------
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack drop = event.getItemDrop().getItemStack();
        Player player = event.getPlayer();

        if (ItemManager.isDetectiveBow(drop)) {
            event.setCancelled(true);
            MessageLimiter.sendPlayerMessage(player, "bow-drop",
                    "§cDu darfst den Detective-Bogen nicht fallen lassen!");
            plugin.debug("Drop von Detective-Bogen durch " + player.getName() + " verhindert.");
            return;
        }

        if (ItemManager.isMurdererSword(drop)) {
            event.setCancelled(true);
            MessageLimiter.sendPlayerMessage(player, "sword-drop",
                    "§cDu darfst das Murderer-Schwert nicht fallen lassen!");
            plugin.debug("Drop von Murderer-Schwert durch " + player.getName() + " verhindert.");
            return;
        }

        // Arena für Spieler bestimmen (lokaler RoleManager)
        ArenaGame manager = registry.findArenaOfPlayer(player);
        if (manager == null) return;

        Role role = manager.getRoles().get(player.getUniqueId());
        if (drop.getType() == Material.ARROW && role == Role.DETECTIVE) {
            event.setCancelled(true);
            MessageLimiter.sendPlayerMessage(player, "arrow-drop",
                    "§cAls Detective darfst du deine Pfeile nicht droppen!");
            plugin.debug("Drop von Pfeilen durch Detective " + player.getName() + " verhindert.");
        }
    }

    // ---------- Pickup regeln ----------
    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack stack = event.getItem().getItemStack();
        UUID uuid = player.getUniqueId();

        // Arena ermitteln
        Arena arena = plugin.getArenaManager().getArenaForWorld(player.getWorld());
        if (arena == null) {
            // Failsafe: außerhalb von Arenen löschen
            if (ItemManager.isDetectiveBow(stack) || ItemManager.isMurdererSword(stack) || stack.getType() == Material.ARROW) {
                event.setCancelled(true);
                event.getItem().remove();
                plugin.debug("Failsafe: Spezialitem ausserhalb Arena entfernt (Pickup durch " + player.getName() + ")");
            }
            return;
        }

        ArenaGame manager = registry.getGameManager(arena.getName());
        if (manager == null) return;

        Role currentRole = manager.getRoles().get(uuid);
        if (currentRole == null) {
            event.setCancelled(true);
            plugin.debug("Pickup-Check: Spieler " + player.getName() + " hat keine Rolle → blockiert.");
            return;
        }

        // 🔪 Murderer-Schwert
        if (ItemManager.isMurdererSword(stack)) {
            if (currentRole != Role.MURDERER) {
                event.setCancelled(true);
                MessageLimiter.sendPlayerMessage(player, "sword-pickup",
                        "§cNur der Mörder darf das Schwert aufnehmen!");
                plugin.debug("Pickup von Murderer-Schwert durch " + player.getName() + " blockiert (Rolle=" + currentRole + ")");
            }
            return;
        }

        // 🏹 Detective-Bogen
        if (ItemManager.isDetectiveBow(stack)) {
            if (currentRole == Role.BYSTANDER) {
                // Rolle lokal in Arena aktualisieren
                manager.getRoleManager().setRole(uuid, Role.DETECTIVE);
                manager.getRoles().put(uuid, Role.DETECTIVE);

                MessageLimiter.sendPlayerMessage(player, "bow-pickup",
                        ChatColor.BLUE + "Du bist jetzt Detective!");
                plugin.debug("Bystander " + player.getName() + " wurde zum Detective (Bogen aufgenommen).");

                if (!player.getInventory().contains(Material.ARROW)) {
                    player.getInventory().addItem(new ItemStack(Material.ARROW, 1));
                    plugin.debug("Neuer Detective " + player.getName() + " hat automatisch einen Pfeil erhalten.");
                }
            } else {
                event.setCancelled(true);
                MessageLimiter.sendPlayerMessage(player, "bow-deny",
                        ChatColor.RED + "Als " + currentRole + " darfst du den Bogen nicht aufnehmen!");
                plugin.debug("Pickup von Detective-Bogen durch " + player.getName() + " blockiert (Rolle=" + currentRole + ")");
            }
            return;
        }

        // 🏹 Pfeile
        if (stack.getType() == Material.ARROW && currentRole != Role.DETECTIVE) {
            event.setCancelled(true);
            MessageLimiter.sendPlayerMessage(player, "arrow-pickup",
                    "§cNur Detectives dürfen Pfeile aufheben!");
            plugin.debug("Pickup von Pfeilen durch " + player.getName() + " blockiert (Rolle=" + currentRole + ")");
        }
    }

    // ---------- Despawn verhindern ----------
    @EventHandler
    public void onDespawn(ItemDespawnEvent event) {
        Item item = event.getEntity();

        Arena arena = plugin.getArenaManager().getArenaForWorld(item.getWorld());
        if (arena == null) {
            if (ItemManager.isDetectiveBow(item.getItemStack()) || ItemManager.isMurdererSword(item.getItemStack())) {
                item.remove();
                plugin.debug("Failsafe: Spezialitem ausserhalb Arena despawned und entfernt.");
            }
            return;
        }

        // In Arenen schützen
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
            MessageLimiter.sendPlayerMessage(player, "item-move",
                    "§cDieses Spezialitem darf nicht bewegt werden!");
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack dragged = event.getOldCursor();
        if (ItemManager.isDetectiveBow(dragged) || ItemManager.isMurdererSword(dragged)) {
            event.setCancelled(true);
            MessageLimiter.sendPlayerMessage(player, "item-drag",
                    "§cDieses Spezialitem darf nicht bewegt werden!");
        }
    }

    // ---------- Weltwechsel-Failsafe ----------
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        Arena arena = plugin.getArenaManager().getArenaForWorld(player.getWorld());
        if (arena == null) {
            ItemManager.clearSpecialItems(player);
            plugin.debug("Failsafe: Spezialitems bei " + player.getName() +
                    " nach Weltwechsel entfernt (Welt=" + player.getWorld().getName() + ")");
        }
    }
}
