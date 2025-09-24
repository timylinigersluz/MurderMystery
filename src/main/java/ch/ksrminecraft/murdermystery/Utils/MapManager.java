package ch.ksrminecraft.murdermystery.Utils;

import ch.ksrminecraft.murdermystery.MurderMystery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class MapManager {

    // Sammlung von Spieler mit der UUID in eine zufällige "map" Welt teleportieren (Config)
    public static void teleportToRandomMap(Collection<UUID> playerUUIDs) {
        FileConfiguration cfg = MurderMystery.getInstance().getConfig();
        List<String> maps = cfg.getStringList("worlds.maps");
        if (maps.isEmpty()) {
            Bukkit.getLogger().severe("Keine Maps im Config gefunden!");
            return;
        }

        String chosen = maps.get(new Random().nextInt(maps.size()));
        World map = Bukkit.getWorld(chosen);
        if (map == null) {
            Bukkit.getLogger().severe("Map '" + chosen + "' konnte nicht geladen werden!");
            return;
        }

        Location loc = map.getSpawnLocation();

        for (UUID uuid : playerUUIDs) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.teleport(loc);
            }
        }
    }

    // Spieler zurück in Hauptwelt (Config) teleportieren
    public static void teleportToMainWorld(Set<UUID> players) {
        FileConfiguration cfg = MurderMystery.getInstance().getConfig();
        String mainName = cfg.getString("worlds.main");
        if (mainName == null) return;
        World main = Bukkit.getWorld(mainName);
        if (main == null) return;
        Location loc = main.getSpawnLocation();

        players.forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.teleport(loc);
            }
        });
    }
}
