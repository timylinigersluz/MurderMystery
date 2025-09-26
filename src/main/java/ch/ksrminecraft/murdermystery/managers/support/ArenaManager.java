package ch.ksrminecraft.murdermystery.managers.support;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.model.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

public class ArenaManager {

    private final MurderMystery plugin;
    private final ConfigManager configManager;
    private final Map<String, Arena> arenas = new HashMap<>();

    public ArenaManager(MurderMystery plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        loadArenasFromConfig();
    }

    /**
     * Arenen aus dem ConfigManager laden
     */
    private void loadArenasFromConfig() {
        arenas.clear();

        Map<String, ConfigManager.ArenaConfig> arenaConfigs = configManager.getArenas();
        if (arenaConfigs.isEmpty()) {
            plugin.getLogger().severe("Keine Arenen in der Config gefunden!");
            return;
        }

        for (ConfigManager.ArenaConfig cfg : arenaConfigs.values()) {
            String worldName = cfg.getWorld();
            int maxPlayers = cfg.getMaxPlayers();

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().severe("Arena '" + cfg.getName() + "': Welt '" + worldName + "' konnte nicht geladen werden!");
                continue;
            }

            // === Spawns laden ===
            List<Location> spawns = new ArrayList<>();
            for (String s : cfg.getSpawns()) {
                try {
                    String[] parts = s.split(",");
                    if (parts.length < 3) {
                        plugin.getLogger().warning("Ungültiger Spawn-Eintrag in Arena '" + cfg.getName() + "': " + s);
                        continue;
                    }

                    double x = Double.parseDouble(parts[0].trim());
                    double y = Double.parseDouble(parts[1].trim());
                    double z = Double.parseDouble(parts[2].trim());

                    spawns.add(new Location(world, x, y, z));
                } catch (Exception ex) {
                    plugin.getLogger().warning("Fehler beim Laden von Spawn '" + s + "' in Arena '" + cfg.getName() + "': " + ex.getMessage());
                }
            }

            // === Region laden (optional) ===
            Integer minX = cfg.getRegion().get("minX");
            Integer maxX = cfg.getRegion().get("maxX");
            Integer minZ = cfg.getRegion().get("minZ");
            Integer maxZ = cfg.getRegion().get("maxZ");

            String arenaKey = cfg.getName().toLowerCase();
            Arena arena = new Arena(arenaKey, maxPlayers, spawns, world, minX, maxX, minZ, maxZ);
            arenas.put(arenaKey, arena);

            plugin.debug("Arena '" + arenaKey + "' geladen → Welt=" + worldName + ", MaxPlayers=" + maxPlayers + ", Spawns=" + spawns.size());
        }
    }

    /** Lädt alle Arenen neu über den ConfigManager */
    public void reload() {
        configManager.reload();
        loadArenasFromConfig();
        plugin.getLogger().info("ArenaManager neu geladen. " + arenas.size() + " Arenen verfügbar.");
    }

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public Arena getRandomArena() {
        if (arenas.isEmpty()) return null;
        List<Arena> list = new ArrayList<>(arenas.values());
        return list.get(new Random().nextInt(list.size()));
    }

    public Arena getArenaForWorld(World world) {
        if (world == null) return null;
        return arenas.values().stream()
                .filter(a -> a.getWorld().equals(world))
                .findFirst()
                .orElse(null);
    }

    public Collection<Arena> getAllArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }
}
