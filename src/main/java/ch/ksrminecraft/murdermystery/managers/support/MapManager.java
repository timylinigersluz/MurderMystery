package ch.ksrminecraft.murdermystery.managers.support;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.model.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class MapManager {

    private final MurderMystery plugin;
    private final ArenaManager arenaManager;

    public MapManager(MurderMystery plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    /**
     * Teleportiert alle Spieler in eine zufällige Arena (aus config.yml -> arenas).
     */
    public void teleportToRandomArena(Set<UUID> playerUUIDs) {
        Arena arena = arenaManager.getRandomArena();
        if (arena == null) {
            plugin.getLogger().severe("Keine Arena verfügbar!");
            return;
        }

        for (UUID uuid : playerUUIDs) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                Location spawn = arena.getRandomSpawn(); // 👈 direkt Arena-Logik nutzen
                p.teleport(spawn);
                plugin.debug("Spieler " + p.getName() + " wurde in Arena '" + arena.getName() + "' teleportiert → " +
                        String.format("(%.1f, %.1f, %.1f)", spawn.getX(), spawn.getY(), spawn.getZ()));
            }
        }
    }

    /**
     * Teleportiert alle Spieler zurück in die Hauptwelt (config.yml -> worlds.main).
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

    /**
     * Teleportiert alle Spieler zurück in die Lobby (config.yml -> worlds.lobby).
     */
    public void teleportToLobby(Set<UUID> players) {
        String lobbyName = plugin.getConfig().getString("worlds.lobby");

        if (lobbyName == null || lobbyName.isBlank()) {
            plugin.getLogger().severe("Fehler: 'worlds.lobby' ist in der Config nicht gesetzt!");
            return;
        }

        World lobby = Bukkit.getWorld(lobbyName);
        if (lobby == null) {
            plugin.getLogger().severe("Lobby-Welt '" + lobbyName + "' konnte nicht geladen werden!");
            return;
        }

        Location spawn = lobby.getSpawnLocation();
        plugin.debug("Teleportiere alle Spieler zurück in die Lobby '" + lobbyName + "'.");

        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.teleport(spawn);
                plugin.debug("Spieler " + p.getName() + " wurde nach '" + lobbyName + "' teleportiert.");
            }
        }
    }
}
