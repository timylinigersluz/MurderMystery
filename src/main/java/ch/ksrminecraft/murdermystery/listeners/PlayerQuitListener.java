package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.utils.GameManager;
import ch.ksrminecraft.murdermystery.utils.QuitTracker;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final MurderMystery plugin;
    private final GameManager gameManager;
    private final FileConfiguration config;

    public PlayerQuitListener(GameManager gameManager) {
        this.plugin = MurderMystery.getInstance();
        this.gameManager = gameManager;
        this.config = plugin.getConfig();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player quitter = e.getPlayer();
        plugin.debug("Spieler " + quitter.getName() + " hat den Server verlassen.");

        // Wenn kein Spiel läuft → nichts tun
        if (!gameManager.isGameStarted()) {
            plugin.debug("Spiel läuft nicht. Quit von " + quitter.getName() + " wird ignoriert.");
            return;
        }

        // War Spieler Teil des Spiels?
        if (gameManager.isPlayerInGame(quitter)) {
            plugin.debug("Spieler " + quitter.getName() + " war im Spiel → wird eliminiert.");
            gameManager.eliminate(quitter);

            // Strafe für Ragequit
            int penalty = config.getInt("punkte-strafe", 5);
            plugin.getPointsManager().applyPenalty(quitter.getUniqueId(), penalty, "Ragequit");

            // QuitTracker → merken, falls Spieler nicht in Hauptwelt war
            String mainWorldName = config.getString("worlds.main");
            World mainWorld = Bukkit.getWorld(mainWorldName);

            if (mainWorld == null) {
                plugin.getLogger().severe("Fehler: Hauptwelt '" + mainWorldName + "' konnte nicht geladen werden!");
                return;
            }

            World currentWorld = quitter.getWorld();
            if (!currentWorld.getName().equalsIgnoreCase(mainWorld.getName())) {
                QuitTracker.mark(quitter);
                plugin.debug("Spieler " + quitter.getName() + " wurde im QuitTracker markiert (war in Welt '" + currentWorld.getName() + "').");
            } else {
                plugin.debug("Spieler " + quitter.getName() + " war bereits in der Hauptwelt, kein QuitTracker-Eintrag notwendig.");
            }
        } else {
            plugin.debug("Spieler " + quitter.getName() + " war nicht im Spiel. Keine Eliminierung nötig.");
        }
    }
}
