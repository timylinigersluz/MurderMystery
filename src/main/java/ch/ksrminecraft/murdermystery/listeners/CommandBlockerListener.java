package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.effects.ItemManager;
import ch.ksrminecraft.murdermystery.utils.MessageLimiter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandBlockerListener implements Listener {

    private final MurderMystery plugin;

    public CommandBlockerListener(MurderMystery plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getGameManager().isGameStarted()) return;

        Player p = event.getPlayer();
        String msg = event.getMessage().toLowerCase();

        // /kill blockieren
        if (msg.startsWith("/kill") || msg.startsWith("/minecraft:kill")) {
            event.setCancelled(true);
            p.sendMessage(ChatColor.RED + "Dieser Befehl ist während MurderMystery deaktiviert!");
            plugin.debug("Befehl /kill von " + p.getName() + " blockiert.");
            return;
        }

        // /clear blockieren, wenn Spezialitems im Inventar
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
}
