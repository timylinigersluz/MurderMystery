package ch.ksrminecraft.murdermystery.utils;

import ch.ksrminecraft.murdermystery.MurderMystery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class ArenaManager {

    private final MurderMystery plugin;
    private final Map<String, Arena> arenas = new HashMap<>();

    public ArenaManager(MurderMystery plugin) {
        this.plugin = plugin;
        loadArenasFromConfig();
    }

    /**
     * Arenen aus der Config laden
     */
    private void loadArenasFromConfig() {
        ConfigurationSection arenasSection = plugin.getConfig().getConfigurationSection("arenas");
        if (arenasSection == null) {
            plugin.getLogger().severe("Keine Arenen in der Config gefunden!");
            return;
        }

        for (String key : arenasSection.getKeys(false)) {
            ConfigurationSection section = arenasSection.getConfigurationSection(key);
            if (section == null) continue;

            String worldName = section.getString("world");
            int maxPlayers = section.getInt("maxPlayers", 16);

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().severe("Arena '" + key + "': Welt '" + worldName + "' konnte nicht geladen werden!");
                continue;
            }

            // === Spawns laden ===
            List<String> spawnStrings = section.getStringList("spawns");
            List<Location> spawns = new ArrayList<>();

            for (String s : spawnStrings) {
                try {
                    String[] parts = s.split(",");
                    if (parts.length < 3) {
                        plugin.getLogger().warning("Ungültiger Spawn-Eintrag in Arena '" + key + "': " + s);
                        continue;
                    }

                    double x = Double.parseDouble(parts[0].trim());
                    double y = Double.parseDouble(parts[1].trim());
                    double z = Double.parseDouble(parts[2].trim());

                    spawns.add(new Location(world, x, y, z));
                } catch (Exception ex) {
                    plugin.getLogger().warning("Fehler beim Laden von Spawn '" + s + "' in Arena '" + key + "': " + ex.getMessage());
                }
            }

            // === Region laden (optional) ===
            ConfigurationSection region = section.getConfigurationSection("region");
            Integer minX = null, maxX = null, minZ = null, maxZ = null;
            if (region != null) {
                minX = region.getInt("minX");
                maxX = region.getInt("maxX");
                minZ = region.getInt("minZ");
                maxZ = region.getInt("maxZ");
                plugin.debug("Arena '" + key + "' Region gesetzt: X(" + minX + "→" + maxX + "), Z(" + minZ + "→" + maxZ + ")");
            }

            String arenaKey = key.toLowerCase();
            Arena arena = new Arena(arenaKey, maxPlayers, spawns, world, minX, maxX, minZ, maxZ);
            arenas.put(arenaKey, arena);

            plugin.debug("Arena '" + arenaKey + "' geladen → Welt=" + worldName + ", MaxPlayers=" + maxPlayers + ", Spawns=" + spawns.size());
        }
    }

    /** Lädt alle Arenen neu aus der Config */
    public void reload() {
        arenas.clear();
        plugin.reloadConfig();
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
