package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.effects.ItemManager;
import ch.ksrminecraft.murdermystery.managers.game.RoleManager;
import ch.ksrminecraft.murdermystery.model.Role;
import ch.ksrminecraft.murdermystery.utils.MessageLimiter;
import org.bukkit.Bukkit;
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
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

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

        if (drop.getType() == Material.ARROW) {
            Role role = RoleManager.getRole(player.getUniqueId());
            if (role == Role.DETECTIVE) {
                event.setCancelled(true);
                MessageLimiter.sendPlayerMessage(player, "arrow-drop",
                        "§cAls Detective darfst du deine Pfeile nicht droppen!");
                plugin.debug("Drop von Pfeilen durch Detective " + player.getName() + " verhindert.");
            }
        }
    }

    // ---------- Pickup regeln ----------
    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack stack = event.getItem().getItemStack();
        UUID uuid = player.getUniqueId();

        // Safety: außerhalb von Arenen → Item sofort löschen
        if (plugin.getArenaManager().getArenaForWorld(player.getWorld()) == null) {
            if (ItemManager.isDetectiveBow(stack) || ItemManager.isMurdererSword(stack) || stack.getType() == Material.ARROW) {
                event.setCancelled(true);
                event.getItem().remove();
                plugin.debug("Failsafe: Spezialitem ausserhalb Arena entfernt (Pickup durch " + player.getName() + ")");
            }
            return;
        }

        Role currentRole = RoleManager.getRole(uuid);
        if (currentRole == null) {
            plugin.debug("Pickup-Check: Spieler " + player.getName() + " hat keine Rolle → blockiert.");
            event.setCancelled(true);
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
                RoleManager.setRole(uuid, Role.DETECTIVE);
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
        if (stack.getType() == Material.ARROW) {
            if (currentRole != Role.DETECTIVE) {
                event.setCancelled(true);
                MessageLimiter.sendPlayerMessage(player, "arrow-pickup",
                        "§cNur Detectives dürfen Pfeile aufheben!");
                plugin.debug("Pickup von Pfeilen durch " + player.getName() + " blockiert (Rolle=" + currentRole + ")");
            } else {
                // Detective hebt Pfeile auf → direkt auf 1 reduzieren (keine Meldung)
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    int arrowCount = player.getInventory().all(Material.ARROW)
                            .values()
                            .stream()
                            .mapToInt(item -> item != null ? item.getAmount() : 0)
                            .sum();

                    if (arrowCount > 1) {
                        player.getInventory().remove(Material.ARROW);
                        player.getInventory().addItem(new ItemStack(Material.ARROW, 1));
                        plugin.debug("Pickup-Korrektur: " + player.getName() + " hatte " + arrowCount + " Pfeile → reduziert auf 1.");
                    }
                }, 1L); // 1 Tick Delay, damit das Pickup zuerst durchläuft
            }
        }
    }

    // ---------- Despawn verhindern ----------
    @EventHandler
    public void onDespawn(ItemDespawnEvent event) {
        Item item = event.getEntity();

        if (plugin.getArenaManager().getArenaForWorld(item.getWorld()) == null) {
            if (ItemManager.isDetectiveBow(item.getItemStack()) || ItemManager.isMurdererSword(item.getItemStack())) {
                item.remove();
                plugin.debug("Failsafe: Spezialitem ausserhalb Arena despawned und entfernt.");
            }
            return;
        }

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

    // ---------- /clear blockieren ----------
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        String msg = event.getMessage().toLowerCase();

        if (msg.startsWith("/clear") || msg.startsWith("/minecraft:clear")) {
            if (p.getInventory().contains(ItemManager.createDetectiveBow().getType()) ||
                    p.getInventory().contains(ItemManager.createMurdererSword().getType())) {
                event.setCancelled(true);
                MessageLimiter.sendPlayerMessage(p, "clear-block",
                        "§cDu darfst dein Inventar nicht leeren, solange du Spezialitems besitzt!");
                plugin.debug("Befehl /clear von " + p.getName() + " blockiert (Spezialitem im Inventar).");
            }
        }
    }

    // ---------- Weltwechsel-Failsafe ----------
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        if (plugin.getArenaManager().getArenaForWorld(player.getWorld()) == null) {
            ItemManager.clearSpecialItems(player);
            plugin.debug("Failsafe: Spezialitems bei " + player.getName() +
                    " nach Weltwechsel entfernt (Welt=" + player.getWorld().getName() + ")");
        }
    }
}
