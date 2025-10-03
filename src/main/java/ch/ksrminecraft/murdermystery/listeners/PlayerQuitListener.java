package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.game.GameManagerRegistry;
import ch.ksrminecraft.murdermystery.model.ArenaGame;
import ch.ksrminecraft.murdermystery.model.QuitTracker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final MurderMystery plugin;
    private final GameManagerRegistry registry;

    public PlayerQuitListener(GameManagerRegistry registry) {
        this.plugin = MurderMystery.getInstance();
        this.registry = registry;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player quitter = e.getPlayer();
        plugin.debug("Spieler " + quitter.getName() + " hat den Server verlassen.");

        // Immer im QuitTracker markieren (wird bei Join wieder gecleart)
        QuitTracker.mark(quitter);

        // Arena finden
        ArenaGame manager = registry.findArenaOfPlayer(quitter);
        if (manager != null && manager.isGameStarted()) {
            plugin.debug("Quit während Spiel in Arena " + manager.getArena().getName() + " → handleLeave()");
            manager.handleLeave(quitter);
        } else {
            plugin.debug("Quit außerhalb laufender Arenen → nur im QuitTracker vermerkt.");
        }
    }
}
