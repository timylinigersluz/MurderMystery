package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.model.Arena;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
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

    /**
     * Prüft, ob ein Spieler in einer Arena- oder Lobby-Welt ist.
     */
    private boolean isProtectedWorld(Player p) {
        // Arena-Welten
        Arena arena = plugin.getArenaManager().getArenaForWorld(p.getWorld());
        if (arena != null) return true;

        // Lobby-Welt
        String lobbyWorld = plugin.getConfigManager().getLobbyWorld();
        return p.getWorld().getName().equalsIgnoreCase(lobbyWorld);
    }

    private boolean isAdminBypassed(Player p) {
        return p.hasPermission(ADMIN_PERMISSION) && p.getGameMode() == GameMode.CREATIVE;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        if (!isProtectedWorld(p) || isAdminBypassed(p)) return;

        event.setCancelled(true);
        p.sendMessage(ChatColor.RED + "Du darfst hier keine Blöcke platzieren!");
        plugin.debug("BlockPlace verhindert in geschützter Welt: " + p.getName() + " → " + event.getBlockPlaced().getType());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        if (!isProtectedWorld(p) || isAdminBypassed(p)) return;

        event.setCancelled(true);
        p.sendMessage(ChatColor.RED + "Du darfst hier keine Blöcke abbauen!");
        plugin.debug("BlockBreak verhindert in geschützter Welt: " + p.getName() + " → " + event.getBlock().getType());
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player p = event.getPlayer();
        if (!isProtectedWorld(p) || isAdminBypassed(p)) return;

        event.setCancelled(true);
        p.sendMessage(ChatColor.RED + "Eimer sind hier deaktiviert!");
        plugin.debug("BucketEmpty verhindert in geschützter Welt: " + p.getName());
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player p = event.getPlayer();
        if (!isProtectedWorld(p) || isAdminBypassed(p)) return;

        event.setCancelled(true);
        plugin.debug("BucketFill verhindert in geschützter Welt: " + p.getName());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (!isProtectedWorld(p) || isAdminBypassed(p)) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        Material type = item.getType();
        if (type == Material.FLINT_AND_STEEL || type == Material.FIRE_CHARGE) {
            event.setCancelled(true);
            p.sendMessage(ChatColor.RED + "Feuer ist hier deaktiviert!");
            plugin.debug("Feuerzeug/FireCharge verhindert in geschützter Welt: " + p.getName());
        }
    }
}
