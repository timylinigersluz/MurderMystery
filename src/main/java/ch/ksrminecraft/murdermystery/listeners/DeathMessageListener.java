package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.game.GameManagerRegistry;
import ch.ksrminecraft.murdermystery.model.ArenaGame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathMessageListener implements Listener {

    private final MurderMystery plugin;
    private final GameManagerRegistry registry;

    public DeathMessageListener(MurderMystery plugin, GameManagerRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();

        // Arena ermitteln
        ArenaGame manager = registry.findArenaOfPlayer(victim);
        if (manager == null || !manager.isGameStarted()) {
            return; // außerhalb von Arenen → Vanilla DeathMessage zulassen
        }

        // Nachricht blockieren
        if (event.getDeathMessage() != null) {
            plugin.debug("DeathMessage geblockt in Arena " + manager.getArena().getName()
                    + ": \"" + event.getDeathMessage() + "\" für Spieler "
                    + victim.getName());
        }
        event.setDeathMessage(null);
    }
}
