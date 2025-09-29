package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public class AdvancementBlockListener implements Listener {

    private final MurderMystery plugin;

    public AdvancementBlockListener(MurderMystery plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        // Nur blockieren, wenn ein Spiel läuft
        if (plugin.getGameManager().isGameStarted()) {
            event.message(null); // unterdrückt die Chat-Nachricht
            plugin.debug("Advancement von " + event.getPlayer().getName() + " blockiert: " +
                    event.getAdvancement().getKey().getKey());
        }
    }
}
