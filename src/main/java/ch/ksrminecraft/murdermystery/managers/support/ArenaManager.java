package ch.ksrminecraft.murdermystery.managers.support;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.model.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.util.*;

public class ArenaManager {

    private final MurderMystery plugin;
    private final ConfigManager configManager;
    private final Map<String, Arena> arenas = new HashMap<>();
    private final Map<String, String> arenaSizes = new HashMap<>();
    private final Random random = new Random();

    public ArenaManager(MurderMystery plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        loadArenasFromConfig();
    }

    private void loadArenasFromConfig() {
        arenas.clear();
        arenaSizes.clear();

        Map<String, ConfigManager.ArenaConfig> arenaConfigs = configManager.getArenas();
        plugin.debug("[ArenaManager] Lade Arenen aus Config. Gefunden: " + arenaConfigs.size());

        if (arenaConfigs.isEmpty()) {
            plugin.getLogger().severe("Keine Arenen in der Config gefunden!");
            return;
        }

        for (ConfigManager.ArenaConfig cfg : arenaConfigs.values()) {
            final String worldName = cfg.getWorld();
            final int maxPlayers = cfg.getMaxPlayers();
            final String size = cfg.getSize();

            // Welt laden / nachladen
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Welt '" + worldName + "' ist nicht geladen. Versuche zu laden...");
                try {
                    world = new WorldCreator(worldName).createWorld();
                    plugin.getLogger().info("Welt '" + worldName + "' erfolgreich geladen!");
                } catch (Exception ex) {
                    plugin.getLogger().severe("Arena '" + cfg.getName() + "': Welt '" + worldName +
                            "' konnte NICHT geladen werden! Fehler: " + ex.getMessage());
                    continue;
                }
            }

            // Arena anlegen (GameSpawns füllen wir gleich)
            String arenaKey = cfg.getName().toLowerCase();
            Arena arena = new Arena(
                    arenaKey,
                    maxPlayers,
                    new ArrayList<>(), // arenaGameSpawnPoints wird gleich befüllt
                    world,
                    cfg.getRegion().get("minX"),
                    cfg.getRegion().get("maxX"),
                    cfg.getRegion().get("minZ"),
                    cfg.getRegion().get("maxZ"),
                    size
            );

            // Arena-Lobby-Spawn setzen
            Location lobby = cfg.getArenaLobbySpawnPoint();
            if (lobby != null) {
                if (lobby.getWorld() == null) lobby.setWorld(world);
                arena.setArenaLobbySpawnPoint(lobby);
                plugin.debug("[ArenaManager] Lobby-Spawn gesetzt für '" + arenaKey + "' → "
                        + String.format("(%.1f, %.1f, %.1f | Yaw=%.1f, Pitch=%.1f)",
                        lobby.getX(), lobby.getY(), lobby.getZ(), lobby.getYaw(), lobby.getPitch()));
            } else {
                plugin.debug("[ArenaManager] Kein Lobby-Spawn für '" + arenaKey + "' in der Config. Fallback: Weltspawn.");
            }

            // Game-Spawns & optional Spectator-Spawn laden
            for (String s : cfg.getArenaGameSpawnPoints()) {
                try {
                    String[] parts = s.split(",");
                    if (parts.length < 3) {
                        plugin.debug("[ArenaManager] Ungültiger Spawn-String: " + s);
                        continue;
                    }

                    double x = Double.parseDouble(parts[0].trim());
                    double y = Double.parseDouble(parts[1].trim());
                    double z = Double.parseDouble(parts[2].trim());
                    float yaw = parts.length > 3 ? Float.parseFloat(parts[3].trim()) : 0f;
                    float pitch = parts.length > 4 ? Float.parseFloat(parts[4].trim()) : 0f;

                    Location loc = new Location(world, x, y, z, yaw, pitch);

                    if (parts.length > 5 && parts[5].equalsIgnoreCase("SPECTATOR")) {
                        arena.setSpectatorSpawnPoint(loc);
                        plugin.debug("[ArenaManager] Spectator-Spawn erkannt: " + loc);
                    } else {
                        arena.getArenaGameSpawnPoints().add(loc);
                        plugin.debug("[ArenaManager] GameSpawn hinzugefügt: " + loc);
                    }
                } catch (Exception ex) {
                    plugin.getLogger().warning("Fehler beim Laden von Spawn '" + s +
                            "' in Arena '" + cfg.getName() + "': " + ex.getMessage());
                }
            }

            arenas.put(arenaKey, arena);
            arenaSizes.put(arenaKey, size.toLowerCase());

            plugin.debug("[ArenaManager] Arena '" + arenaKey + "' geladen → Größe=" + size +
                    ", Welt=" + worldName +
                    ", GameSpawns=" + arena.getArenaGameSpawnPoints().size() +
                    (arena.getSpectatorSpawnPoint() != null ? " + Spectator-Spawn" : "") +
                    (lobby != null ? " + Lobby-Spawn" : ""));
        }

        plugin.debug("[ArenaManager] Fertig geladen. Arenen insgesamt: " + arenas.size());
    }

    public void reload() {
        plugin.debug("[ArenaManager] Reload angefordert.");
        configManager.reload();
        loadArenasFromConfig();
    }

    public Arena getArena(String name) {
        if (name == null) return null;
        Arena arena = arenas.get(name.toLowerCase());
        if (arena == null) {
            plugin.debug("[ArenaManager] getArena(" + name + ") → nicht gefunden!");
        }
        return arena;
    }

    public Arena getRandomArena() {
        if (arenas.isEmpty()) {
            plugin.debug("[ArenaManager] getRandomArena → keine Arenen vorhanden.");
            return null;
        }
        List<Arena> list = new ArrayList<>(arenas.values());
        return list.get(random.nextInt(list.size()));
    }

    public Arena getRandomArenaBySize(String size) {
        plugin.debug("[ArenaManager] getRandomArenaBySize(" + size + ")");
        List<Arena> filtered = new ArrayList<>();
        for (String key : arenas.keySet()) {
            if (arenaSizes.getOrDefault(key, "unspecified").equalsIgnoreCase(size)) {
                filtered.add(arenas.get(key));
            }
        }
        if (filtered.isEmpty()) {
            plugin.debug("[ArenaManager] Keine passende Größe gefunden → fallback getRandomArena()");
            return getRandomArena();
        }
        return filtered.get(random.nextInt(filtered.size()));
    }

    public Collection<Arena> getAllArenas() {
        plugin.debug("[ArenaManager] getAllArenas() → " + arenas.size() + " Arenen zurückgegeben.");
        return Collections.unmodifiableCollection(arenas.values());
    }

    /** Arena anhand der Welt bestimmen */
    public Arena getArenaForWorld(World world) {
        if (world == null) {
            plugin.debug("[ArenaManager] getArenaForWorld(null) → null");
            return null;
        }

        Arena arena = arenas.values().stream()
                .filter(a -> a.getWorld().equals(world))
                .findFirst()
                .orElse(null);

        plugin.debug("[ArenaManager] getArenaForWorld(" + world.getName() + ") → "
                + (arena != null ? arena.getName() : "null"));
        return arena;
    }

}
