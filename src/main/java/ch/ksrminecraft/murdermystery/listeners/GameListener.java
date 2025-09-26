package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.game.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class GameListener implements Listener {

    private final MurderMystery plugin;
    private final GameManager gameManager;

    public GameListener(GameManager gameManager) {
        this.plugin = MurderMystery.getInstance();
        this.gameManager = gameManager;
    }

    /**
     * Spieler stirbt → wird eliminiert, falls er Teil des Spiels ist.
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();

        if (!gameManager.isGameStarted()) {
            plugin.debug("Spieler " + dead.getName() + " ist gestorben, aber es läuft kein Spiel.");
            return;
        }

        if (gameManager.isPlayerInGame(dead)) {
            plugin.debug("Spieler " + dead.getName() + " ist im Spiel gestorben und wird eliminiert.");
            gameManager.eliminate(dead);
        } else {
            plugin.debug("Spieler " + dead.getName() + " ist gestorben, war aber nicht Teil des Spiels.");
        }
    }
}
