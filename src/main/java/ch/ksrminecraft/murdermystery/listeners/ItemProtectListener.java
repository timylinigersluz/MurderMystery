package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.managers.effects.ItemManager;
import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.ItemStack;

public class ItemProtectListener implements Listener {

    private static final String ADMIN_PERMISSION = "murdermystery.admin";
    private final ConfigManager configManager;

    public ItemProtectListener(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (player.hasPermission(ADMIN_PERMISSION) && configManager.isAllowAdminMove()) {
                return; // Admin darf Items bewegen, wenn in config erlaubt
            }
        }

        ItemStack current = event.getCurrentItem();
        if (ItemManager.isDetectiveBow(current) || ItemManager.isMurdererSword(current)) {
            event.setCancelled(true);
        }
    }

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
        }
    }

    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        // Hopper etc. – kein Spieler
        ItemStack moved = event.getItem();
        if (ItemManager.isDetectiveBow(moved) || ItemManager.isMurdererSword(moved)) {
            event.setCancelled(true);
        }
    }
}
