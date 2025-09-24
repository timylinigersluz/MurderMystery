package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class EnvironmentProtectListener implements Listener {

    private final MurderMystery plugin;
    private static final String ADMIN_PERMISSION = "murdermystery.admin";

    public EnvironmentProtectListener(MurderMystery plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        if (p.hasPermission(ADMIN_PERMISSION)) return;

        event.setCancelled(true);
        p.sendMessage(ChatColor.RED + "Du darfst hier keine Blöcke platzieren!");
        plugin.debug("BlockPlace verhindert: " + p.getName() + " → " + event.getBlockPlaced().getType());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        if (p.hasPermission(ADMIN_PERMISSION)) return;

        event.setCancelled(true);
        p.sendMessage(ChatColor.RED + "Du darfst hier keine Blöcke abbauen!");
        plugin.debug("BlockBreak verhindert: " + p.getName() + " → " + event.getBlock().getType());
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player p = event.getPlayer();
        if (p.hasPermission(ADMIN_PERMISSION)) return;

        event.setCancelled(true);
        p.sendMessage(ChatColor.RED + "Eimer sind in MurderMystery deaktiviert!");
        plugin.debug("BucketEmpty verhindert: " + p.getName());
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player p = event.getPlayer();
        if (p.hasPermission(ADMIN_PERMISSION)) return;

        event.setCancelled(true);
        plugin.debug("BucketFill verhindert: " + p.getName());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (p.hasPermission(ADMIN_PERMISSION)) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        Material type = item.getType();
        if (type == Material.FLINT_AND_STEEL || type == Material.FIRE_CHARGE) {
            event.setCancelled(true);
            p.sendMessage(ChatColor.RED + "Feuer ist in MurderMystery deaktiviert!");
            plugin.debug("Feuerzeug/FireCharge verhindert: " + p.getName());
        }
    }
}
