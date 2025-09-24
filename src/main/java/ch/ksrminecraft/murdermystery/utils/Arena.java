package ch.ksrminecraft.murdermystery.utils;

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

    // Optionaler Region-Fallback
    private final World world;
    private final Integer minX, maxX, minZ, maxZ;

    private final Random random = new Random();

    public Arena(String name, int maxPlayers, List<Location> spawnPoints,
                 World world, Integer minX, Integer maxX, Integer minZ, Integer maxZ) {
        this.name = name;
        this.maxPlayers = maxPlayers;
        this.spawnPoints = spawnPoints;
        this.world = world;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
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

    /**
     * Gibt einen zufälligen Spawn zurück.
     * 1. Config-Spawns
     * 2. Region (falls vorhanden)
     * 3. Welt-Spawn
     */
    public Location getRandomSpawn() {
        if (!spawnPoints.isEmpty()) {
            return spawnPoints.get(random.nextInt(spawnPoints.size()));
        }

        if (minX != null && maxX != null && minZ != null && maxZ != null && world != null) {
            for (int i = 0; i < 20; i++) {
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

        return (world != null)
                ? world.getSpawnLocation()
                : Bukkit.getWorlds().get(0).getSpawnLocation();
    }
}
