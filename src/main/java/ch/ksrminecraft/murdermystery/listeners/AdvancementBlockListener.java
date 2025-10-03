package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.game.GameManagerRegistry;
import ch.ksrminecraft.murdermystery.model.ArenaGame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public class AdvancementBlockListener implements Listener {

    private final MurderMystery plugin;
    private final GameManagerRegistry registry;

    public AdvancementBlockListener(MurderMystery plugin, GameManagerRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        // Nur blockieren, wenn der Spieler in einer Arena ist UND dort gerade ein Spiel läuft
        ArenaGame manager = registry.findArenaOfPlayer(event.getPlayer());
        if (manager != null && manager.isGameStarted()) {
            event.message(null); // unterdrückt die Chat-Nachricht (Paper API)
            plugin.debug("Advancement von " + event.getPlayer().getName()
                    + " blockiert (Arena=" + manager.getArena().getName() + "): "
                    + event.getAdvancement().getKey().getKey());
        }
    }
}
