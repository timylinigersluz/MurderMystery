package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.game.GameManager;
import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final MurderMystery plugin;
    private final GameManager gameManager;

    public PlayerQuitListener(GameManager gameManager, ConfigManager configManager) {
        this.plugin = MurderMystery.getInstance();
        this.gameManager = gameManager;
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
            plugin.debug("Quit während Spiel → handleLeave()");
            gameManager.handleLeave(quitter);
        }
    }
}
