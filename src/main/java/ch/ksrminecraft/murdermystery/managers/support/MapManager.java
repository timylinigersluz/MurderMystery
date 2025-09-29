package ch.ksrminecraft.murdermystery.managers.support;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.model.Arena;
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

    /** Spieler in die Lobby teleportieren (Random-Spawn mit Fallback) */
    public void teleportToLobby(Set<UUID> players) {
        String worldName = plugin.getConfig().getString("worlds.lobby");
        if (worldName == null || worldName.isBlank()) {
            plugin.getLogger().severe("Fehler: 'worlds.lobby' ist in der Config nicht gesetzt!");
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().severe("Lobby-Welt '" + worldName + "' konnte nicht geladen werden!");
            return;
        }

        Location targetSpawn = getRandomLobbySpawn(world);

        plugin.debug("Teleportiere Spieler → Lobby (" + worldName + "), Spawn " +
                String.format("(%.1f, %.1f, %.1f)", targetSpawn.getX(), targetSpawn.getY(), targetSpawn.getZ()));

        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                teleportPlayer(p, targetSpawn, "Lobby");
            }
        }
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

    /** Liefert einen zufälligen Lobby-Spawn oder Fallback auf Welt-Spawn */
    public Location getRandomLobbySpawn(World world) {
        Location targetSpawn = null;

        List<String> spawnStrings = plugin.getConfig().getStringList("lobby-spawns");
        if (!spawnStrings.isEmpty()) {
            String randomEntry = spawnStrings.get(new Random().nextInt(spawnStrings.size()));
            try {
                String[] parts = randomEntry.split(",");
                if (parts.length >= 3) {
                    double x = Double.parseDouble(parts[0].trim());
                    double y = Double.parseDouble(parts[1].trim());
                    double z = Double.parseDouble(parts[2].trim());
                    targetSpawn = new Location(world, x + 0.5, y, z + 0.5);
                } else {
                    plugin.getLogger().warning("Ungültiger Spawn-Eintrag (zu wenig Koordinaten): " + randomEntry);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Fehler beim Parsen eines lobby-spawns: " + randomEntry + " → " + e.getMessage());
            }
        }

        if (targetSpawn == null) {
            targetSpawn = world.getSpawnLocation();
            plugin.getLogger().warning("Kein gültiger Lobby-Spawn gefunden → Fallback auf Welt-Spawn!");
        }

        return targetSpawn;
    }

    /** Überladung: Holt sich die Lobby-Welt automatisch aus der Config */
    public Location getRandomLobbySpawn() {
        String worldName = plugin.getConfig().getString("worlds.lobby");
        if (worldName == null || worldName.isBlank()) {
            plugin.getLogger().severe("Fehler: 'worlds.lobby' ist in der Config nicht gesetzt!");
            return Bukkit.getWorlds().get(0).getSpawnLocation(); // Fallback: erste Welt
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().severe("Lobby-Welt '" + worldName + "' konnte nicht geladen werden!");
            return Bukkit.getWorlds().get(0).getSpawnLocation(); // Fallback
        }

        return getRandomLobbySpawn(world);
    }

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
