package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathMessageListener implements Listener {

    private final MurderMystery plugin;

    public DeathMessageListener(MurderMystery plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getDeathMessage() != null) {
            // Standard-Nachricht blockieren
            plugin.debug("DeathMessage geblockt: \"" + event.getDeathMessage() + "\" für Spieler "
                    + event.getEntity().getName());
        }
        event.setDeathMessage(null);
    }
}
