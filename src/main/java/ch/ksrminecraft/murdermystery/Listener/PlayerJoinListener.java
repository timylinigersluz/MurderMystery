package ch.ksrminecraft.murdermystery.Listener;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.Utils.QuitTracker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final FileConfiguration config = MurderMystery.getInstance().getConfig();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        // Wenn Spieler rejoined ist, nachdem er den Server während dem Spiel verlassen hat
        if (QuitTracker.hasQuit(p)) {
            QuitTracker.clear(p);

            // Hauptwelt aus Config laden
            String mainWorldName = config.getString("worlds.main");
            World mainWorld = Bukkit.getWorld(mainWorldName);

            if (mainWorld != null) {
                Location spawn = mainWorld.getSpawnLocation();
                p.teleport(spawn);
            } else {
                Bukkit.getLogger().severe("Hauptwelt konnte nicht in Config gefunden werden!");
                p.sendMessage(ChatColor.RED + "Hauptwelt konnte nicht in Config gefunden werden, tp abgebrochen.");
            }
        }

    }
}