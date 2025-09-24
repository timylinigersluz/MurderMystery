package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.utils.ItemManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.ItemStack;

public class ItemProtectListener implements Listener {

    private static final String ADMIN_PERMISSION = "murdermystery.admin";

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && player.hasPermission(ADMIN_PERMISSION)) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        if (ItemManager.isDetectiveBow(current) || ItemManager.isMurdererSword(current)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && player.hasPermission(ADMIN_PERMISSION)) {
            return;
        }

        ItemStack dragged = event.getOldCursor();
        if (ItemManager.isDetectiveBow(dragged) || ItemManager.isMurdererSword(dragged)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        // Hopper etc. – hier gibt es keinen direkten Spieler
        ItemStack moved = event.getItem();
        if (ItemManager.isDetectiveBow(moved) || ItemManager.isMurdererSword(moved)) {
            event.setCancelled(true);
        }
    }
}
