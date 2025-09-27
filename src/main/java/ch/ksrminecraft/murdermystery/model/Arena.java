package ch.ksrminecraft.murdermystery.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.List;
import java.util.Random;

public class Arena {

    private final String name;
    private final int maxPlayers;
    private final List<Location> spawnPoints;
    private final World world;

    // Optionaler Region-Fallback
    private final Integer minX, maxX, minZ, maxZ;

    // NEU: Arena-Größe (small/mid/large)
    private final String size;

    private final Random random = new Random();

    public Arena(String name,
                 int maxPlayers,
                 List<Location> spawnPoints,
                 World world,
                 Integer minX,
                 Integer maxX,
                 Integer minZ,
                 Integer maxZ,
                 String size) {
        this.name = name;
        this.maxPlayers = maxPlayers;
        this.spawnPoints = spawnPoints;
        this.world = world;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.size = size != null ? size.toLowerCase() : "unspecified";
    }

    public String getName() {
        return name;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public List<Location> getSpawnPoints() {
        return spawnPoints;
    }

    public World getWorld() {
        return world;
    }

    public String getSize() {
        return size;
    }

    /**
     * Gibt einen zufälligen Spawn zurück:
     * 1. Config-Spawns
     * 2. Zufälliger Punkt innerhalb Region (falls gesetzt)
     * 3. Welt-Spawn
     */
    public Location getRandomSpawn() {
        // 1. Vordefinierte Spawnpunkte
        if (spawnPoints != null && !spawnPoints.isEmpty()) {
            return spawnPoints.get(random.nextInt(spawnPoints.size()));
        }

        // 2. Region fallback
        if (minX != null && maxX != null && minZ != null && maxZ != null && world != null) {
            for (int i = 0; i < 20; i++) { // 20 Versuche, brauchbaren Block zu finden
                int x = random.nextInt(maxX - minX + 1) + minX;
                int z = random.nextInt(maxZ - minZ + 1) + minZ;
                int y = world.getHighestBlockYAt(x, z);

                Location loc = new Location(world, x + 0.5, y + 1, z + 0.5);

                Material under = world.getBlockAt(x, y, z).getType();
                if (under.isSolid() && under != Material.LAVA && under != Material.WATER) {
                    return loc;
                }
            }
        }

        // 3. Fallback: Weltspawn oder Default-Serverwelt
        return (world != null)
                ? world.getSpawnLocation()
                : Bukkit.getWorlds().get(0).getSpawnLocation();
    }

    public boolean hasRegion() {
        return (minX != null && maxX != null && minZ != null && maxZ != null && world != null);
    }
}
