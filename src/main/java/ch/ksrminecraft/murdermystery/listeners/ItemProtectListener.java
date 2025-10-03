package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.effects.ItemManager;
import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import ch.ksrminecraft.murdermystery.model.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.ItemStack;

public class ItemProtectListener implements Listener {

    private static final String ADMIN_PERMISSION = "murdermystery.admin";
    private final ConfigManager configManager;
    private final MurderMystery plugin;

    public ItemProtectListener(ConfigManager configManager) {
        this.configManager = configManager;
        this.plugin = MurderMystery.getInstance();
    }

    // ---------- Inventar Klick ----------
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (player.hasPermission(ADMIN_PERMISSION) && configManager.isAllowAdminMove()) {
                return; // Admin darf Items bewegen, wenn erlaubt
            }
        }

        ItemStack current = event.getCurrentItem();
        if (ItemManager.isDetectiveBow(current) || ItemManager.isMurdererSword(current)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player p) {
                p.sendMessage("§cDieses Spezialitem darf nicht bewegt werden!");
            }
        }
    }

    // ---------- Inventar Drag ----------
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (player.hasPermission(ADMIN_PERMISSION) && configManager.isAllowAdminMove()) {
                return;
            }
        }

        ItemStack dragged = event.getOldCursor();
        if (ItemManager.isDetectiveBow(dragged) || ItemManager.isMurdererSword(dragged)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player p) {
                p.sendMessage("§cDieses Spezialitem darf nicht bewegt werden!");
            }
        }
    }

    // ---------- Hopper / Automoves ----------
    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        ItemStack moved = event.getItem();
        if (ItemManager.isDetectiveBow(moved) || ItemManager.isMurdererSword(moved)) {
            event.setCancelled(true);
        }
    }

    // ---------- Failsafe bei Join ----------
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        clearInvalidItems(event.getPlayer());
    }

    // ---------- Failsafe bei Weltwechsel ----------
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        clearInvalidItems(event.getPlayer());
    }

    // ---------- Hilfsmethode ----------
    private void clearInvalidItems(Player player) {
        // Spezialitems sind nur in Arenen erlaubt
        Arena arena = plugin.getArenaManager().getArenaForWorld(player.getWorld());

        if (arena == null) {
            // Spieler befindet sich NICHT in einer Arena (also z. B. MainLobby oder fremde Welt)
            ItemManager.clearSpecialItems(player);
            plugin.debug("[ItemProtectListener]: Spezialitems aus Inventar von " + player.getName()
                    + " entfernt (Welt=" + player.getWorld().getName() + ")");
        }
    }
}
