package ch.ksrminecraft.murdermystery.utils;

import ch.ksrminecraft.murdermystery.MurderMystery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class MapManager {

    private final MurderMystery plugin;
    private final Random random = new Random();

    public MapManager(MurderMystery plugin) {
        this.plugin = plugin;
    }

    /**
     * Teleportiert alle Spieler in eine zufällige Map aus config.yml -> worlds.maps.
     */
    public void teleportToRandomMap(Set<UUID> playerUUIDs) {
        List<String> maps = plugin.getConfig().getStringList("worlds.maps");

        if (maps == null || maps.isEmpty()) {
            plugin.getLogger().severe("Keine Maps in der Config gefunden (worlds.maps)!");
            return;
        }

        String chosen = maps.get(random.nextInt(maps.size()));
        World map = Bukkit.getWorld(chosen);

        if (map == null) {
            plugin.getLogger().severe("Map '" + chosen + "' konnte nicht geladen werden!");
            return;
        }

        Location spawn = map.getSpawnLocation();
        plugin.debug("Map '" + chosen + "' ausgewählt. Teleportiere Spieler zum Spawn.");

        for (UUID uuid : playerUUIDs) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.teleport(spawn);
                plugin.debug("Spieler " + p.getName() + " wurde nach '" + chosen + "' teleportiert.");
            }
        }
    }

    /**
     * Teleportiert alle Spieler zurück in die Hauptwelt aus config.yml -> worlds.main.
     */
    public void teleportToMainWorld(Set<UUID> players) {
        String mainName = plugin.getConfig().getString("worlds.main");

        if (mainName == null || mainName.isBlank()) {
            plugin.getLogger().severe("Fehler: 'worlds.main' ist in der Config nicht gesetzt!");
            return;
        }

        World main = Bukkit.getWorld(mainName);
        if (main == null) {
            plugin.getLogger().severe("Hauptwelt '" + mainName + "' konnte nicht geladen werden!");
            return;
        }

        Location spawn = main.getSpawnLocation();
        plugin.debug("Teleportiere alle Spieler zurück in die Hauptwelt '" + mainName + "'.");

        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.teleport(spawn);
                plugin.debug("Spieler " + p.getName() + " wurde nach '" + mainName + "' teleportiert.");
            }
        }
    }
}
