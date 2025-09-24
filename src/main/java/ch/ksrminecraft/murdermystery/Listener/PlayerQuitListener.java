package ch.ksrminecraft.murdermystery.Listener;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.Utils.GameManager;
import ch.ksrminecraft.murdermystery.Utils.QuitTracker;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {
    private final FileConfiguration config;
    private final GameManager gameManager;

    public PlayerQuitListener(GameManager gameManager) {
        this.gameManager = gameManager;
        this.config = MurderMystery.getInstance().getConfig();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        // Spiel muss aktiv sein
        if (!gameManager.isGameStarted()) return;
        Player quitter = e.getPlayer();
        // Spieler wird durch Verlassen eliminiert
        if (gameManager.isPlayerInGame(quitter)) {
            gameManager.eliminate(quitter);
            gameManager.getPlayers().remove(quitter);

            World w = quitter.getWorld();
            String mainWorldName = config.getString("worlds.main");
            World mainWorld = Bukkit.getWorld(mainWorldName);
            // Wenn Spieler nicht in der Hauptwelt war
            if (mainWorld != null && !w.getName().equalsIgnoreCase(mainWorld.getName())) {
                // Spieler markieren, um ihn nach rejoin wieder in die Hauptwelt zu teleportieren
                QuitTracker.mark(quitter);
            }
        }
    }
}
