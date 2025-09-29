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
    private final Map<String, String> arenaSizes = new HashMap<>(); // ArenaName → Size
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
        if (arenaConfigs.isEmpty()) {
            plugin.getLogger().severe("Keine Arenen in der Config gefunden!");
            return;
        }

        for (ConfigManager.ArenaConfig cfg : arenaConfigs.values()) {
            String worldName = cfg.getWorld();
            int maxPlayers = cfg.getMaxPlayers();
            String size = cfg.getSize();

            // --- Auto-Load der Welt ---
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Welt '" + worldName + "' ist nicht geladen. Versuche zu laden...");
                try {
                    world = new WorldCreator(worldName).createWorld();
                    plugin.getLogger().info("Welt '" + worldName + "' erfolgreich geladen!");
                } catch (Exception ex) {
                    plugin.getLogger().severe("Arena '" + cfg.getName() + "': Welt '" + worldName + "' konnte NICHT geladen werden! Fehler: " + ex.getMessage());
                    continue;
                }
            }

            List<Location> spawns = new ArrayList<>();
            for (String s : cfg.getSpawns()) {
                try {
                    String[] parts = s.split(",");
                    if (parts.length < 3) continue;
                    double x = Double.parseDouble(parts[0].trim());
                    double y = Double.parseDouble(parts[1].trim());
                    double z = Double.parseDouble(parts[2].trim());
                    spawns.add(new Location(world, x, y, z));
                } catch (Exception ex) {
                    plugin.getLogger().warning("Fehler beim Laden von Spawn '" + s + "' in Arena '" + cfg.getName() + "': " + ex.getMessage());
                }
            }

            Integer minX = cfg.getRegion().get("minX");
            Integer maxX = cfg.getRegion().get("maxX");
            Integer minZ = cfg.getRegion().get("minZ");
            Integer maxZ = cfg.getRegion().get("maxZ");

            String arenaKey = cfg.getName().toLowerCase();
            Arena arena = new Arena(
                    arenaKey,
                    maxPlayers,
                    spawns,
                    world,
                    minX,
                    maxX,
                    minZ,
                    maxZ,
                    size // Größe wird an Arena übergeben
            );
            arenas.put(arenaKey, arena);
            arenaSizes.put(arenaKey, size.toLowerCase());

            plugin.debug("Arena '" + arenaKey + "' geladen → Größe=" + size + ", Welt=" + worldName);
        }
    }

    public void reload() {
        configManager.reload();
        loadArenasFromConfig();
    }

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public Arena getRandomArena() {
        if (arenas.isEmpty()) return null;
        List<Arena> list = new ArrayList<>(arenas.values());
        return list.get(random.nextInt(list.size()));
    }

    public Arena getRandomArenaBySize(String size) {
        List<Arena> filtered = new ArrayList<>();
        for (String key : arenas.keySet()) {
            if (arenaSizes.getOrDefault(key, "unspecified").equalsIgnoreCase(size)) {
                filtered.add(arenas.get(key));
            }
        }
        if (filtered.isEmpty()) return getRandomArena();
        return filtered.get(random.nextInt(filtered.size()));
    }

    /** Arena anhand der Welt bestimmen */
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
