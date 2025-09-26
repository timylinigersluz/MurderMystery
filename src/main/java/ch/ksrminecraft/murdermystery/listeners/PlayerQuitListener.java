package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.game.GameManager;
import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import ch.ksrminecraft.murdermystery.model.QuitTracker;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final MurderMystery plugin;
    private final GameManager gameManager;
    private final ConfigManager configManager;

    public PlayerQuitListener(GameManager gameManager, ConfigManager configManager) {
        this.plugin = MurderMystery.getInstance();
        this.gameManager = gameManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player quitter = e.getPlayer();
        plugin.debug("Spieler " + quitter.getName() + " hat den Server verlassen.");

        if (!gameManager.isGameStarted()) {
            plugin.debug("Spiel läuft nicht. Quit von " + quitter.getName() + " wird ignoriert.");
            return;
        }

        if (gameManager.isPlayerInGame(quitter)) {
            plugin.debug("Spieler " + quitter.getName() + " war im Spiel → wird eliminiert.");
            gameManager.eliminate(quitter);

            // Strafe für Ragequit
            plugin.getPointsManager().applyPenalty(
                    quitter.getUniqueId(),
                    configManager.getPointsQuit(),
                    "Ragequit"
            );

            // QuitTracker setzen, wenn nicht in MainWorld
            String mainWorldName = configManager.getMainWorld();
            World mainWorld = Bukkit.getWorld(mainWorldName);
            if (mainWorld == null) {
                plugin.getLogger().severe("Fehler: Hauptwelt '" + mainWorldName + "' nicht gefunden!");
                return;
            }

            if (!quitter.getWorld().getName().equalsIgnoreCase(mainWorld.getName())) {
                QuitTracker.mark(quitter);
                plugin.debug("Spieler " + quitter.getName() + " wurde im QuitTracker markiert.");
            }
        }
    }
}
