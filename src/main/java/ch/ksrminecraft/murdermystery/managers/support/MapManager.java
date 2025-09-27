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

    // ================== Öffentliche Methoden ==================

    /** Spieler in eine zufällige Arena teleportieren */
    public void teleportToRandomArena(Set<UUID> playerUUIDs) {
        Arena arena = arenaManager.getRandomArena();
        if (arena == null) {
            plugin.getLogger().severe("Keine Arena verfügbar!");
            return;
        }

        for (UUID uuid : playerUUIDs) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                Location spawn = arena.getRandomSpawn();
                teleportPlayer(p, spawn, "Arena '" + arena.getName() + "'");
            }
        }
    }

    /** Spieler in die Hauptwelt teleportieren */
    public void teleportToMainWorld(Set<UUID> players) {
        teleportAll(players, "worlds.main", "Hauptwelt");
    }

    /** Spieler in die Lobby teleportieren */
    public void teleportToLobby(Set<UUID> players) {
        teleportAll(players, "worlds.lobby", "Lobby");
    }

    // === Bequeme Methoden für EINEN Spieler ===

    public void teleportToMainWorld(Player p) {
        if (p != null && p.isOnline()) {
            teleportToMainWorld(Set.of(p.getUniqueId()));
        }
    }

    public void teleportToLobby(Player p) {
        if (p != null && p.isOnline()) {
            teleportToLobby(Set.of(p.getUniqueId()));
        }
    }

    // ================== Hilfsmethoden ==================

    /** Teleportiert alle Spieler zu einer bestimmten Welt aus der Config */
    private void teleportAll(Set<UUID> players, String configPath, String debugName) {
        String worldName = plugin.getConfig().getString(configPath);

        if (worldName == null || worldName.isBlank()) {
            plugin.getLogger().severe("Fehler: '" + configPath + "' ist in der Config nicht gesetzt!");
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().severe(debugName + " '" + worldName + "' konnte nicht geladen werden!");
            return;
        }

        Location spawn = world.getSpawnLocation();
        plugin.debug("Teleportiere Spieler → " + debugName + " (" + worldName + ").");

        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                teleportPlayer(p, spawn, debugName);
            }
        }
    }

    /** Einzelnen Spieler sicher teleportieren (wiederverwendbar) */
    private void teleportPlayer(Player p, Location location, String context) {
        p.teleport(location);
        plugin.debug("Spieler " + p.getName() + " wurde nach " + context +
                " teleportiert → " +
                String.format("(%.1f, %.1f, %.1f)", location.getX(), location.getY(), location.getZ()));
    }
}
