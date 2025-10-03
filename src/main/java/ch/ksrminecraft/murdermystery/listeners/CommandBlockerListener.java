package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.effects.ItemManager;
import ch.ksrminecraft.murdermystery.managers.game.GameManagerRegistry;
import ch.ksrminecraft.murdermystery.model.ArenaGame;
import ch.ksrminecraft.murdermystery.utils.MessageLimiter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandBlockerListener implements Listener {

    private final MurderMystery plugin;
    private final GameManagerRegistry registry;

    public CommandBlockerListener(MurderMystery plugin, GameManagerRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();

        // Arena ermitteln
        ArenaGame manager = registry.findArenaOfPlayer(p);
        if (manager == null || !manager.isGameStarted()) return;

        String msg = event.getMessage().toLowerCase();

        // /kill blockieren
        if (msg.startsWith("/kill") || msg.startsWith("/minecraft:kill")) {
            event.setCancelled(true);
            p.sendMessage(ChatColor.RED + "Dieser Befehl ist während MurderMystery deaktiviert!");
            plugin.debug("Befehl /kill von " + p.getName()
                    + " blockiert (Arena=" + manager.getArena().getName() + ").");
            return;
        }

        // /clear blockieren, wenn Spezialitems im Inventar
        if (msg.startsWith("/clear") || msg.startsWith("/minecraft:clear")) {
            if (p.getInventory().contains(ItemManager.createDetectiveBow().getType()) ||
                    p.getInventory().contains(ItemManager.createMurdererSword().getType())) {

                event.setCancelled(true);
                MessageLimiter.sendPlayerMessage(
                        p,
                        "clear-block",
                        "§cDu darfst dein Inventar nicht leeren, solange du Spezialitems besitzt!"
                );
                plugin.debug("Befehl /clear von " + p.getName()
                        + " blockiert (Arena=" + manager.getArena().getName()
                        + ", Spezialitem im Inventar).");
            }
        }
    }
}
