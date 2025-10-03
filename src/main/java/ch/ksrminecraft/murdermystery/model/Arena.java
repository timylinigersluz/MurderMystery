package ch.ksrminecraft.murdermystery.model;

import ch.ksrminecraft.murdermystery.MurderMystery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.List;
import java.util.Random;

public class Arena {

    private final String name;
    private final int maxPlayers;
    private final List<Location> arenaGameSpawnPoints; // Spielspawns
    private final World world;
    private String gameMode = "classic"; // Default

    // Optionaler Region-Fallback
    private final Integer minX, maxX, minZ, maxZ;

    // Arena-Größe (small/mid/large)
    private final String size;

    // Lobby-Spawn für Arena
    private Location arenaLobbySpawnPoint;

    // Spectator-Spawn
    private Location spectatorSpawnPoint;

    private final Random random = new Random();

    public Arena(String name,
                 int maxPlayers,
                 List<Location> arenaGameSpawnPoints,
                 World world,
                 Integer minX,
                 Integer maxX,
                 Integer minZ,
                 Integer maxZ,
                 String size) {
        this.name = name;
        this.maxPlayers = maxPlayers;
        this.arenaGameSpawnPoints = arenaGameSpawnPoints;
        this.world = world;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.size = size != null ? size.toLowerCase() : "unspecified";
    }

    // --- Getter ---
    public String getName() { return name; }
    public int getMaxPlayers() { return maxPlayers; }
    public List<Location> getArenaGameSpawnPoints() { return arenaGameSpawnPoints; }
    public World getWorld() { return world; }
    public String getSize() { return size; }

    // ---- Arena-Lobby ----
    public Location getArenaLobbySpawnPoint() {
        if (arenaLobbySpawnPoint != null) {
            MurderMystery.getInstance().debug(
                    "[Arena] getArenaLobbySpawnPoint() → definiert @ " +
                            String.format("(%.1f, %.1f, %.1f | Yaw=%.1f, Pitch=%.1f)",
                                    arenaLobbySpawnPoint.getX(), arenaLobbySpawnPoint.getY(), arenaLobbySpawnPoint.getZ(),
                                    arenaLobbySpawnPoint.getYaw(), arenaLobbySpawnPoint.getPitch())
            );
            return arenaLobbySpawnPoint;
        } else {
            Location fallback = world.getSpawnLocation();
            MurderMystery.getInstance().debug(
                    "[Arena] getArenaLobbySpawnPoint() → Fallback Weltspawn @ " +
                            String.format("(%.1f, %.1f, %.1f | Yaw=%.1f, Pitch=%.1f)",
                                    fallback.getX(), fallback.getY(), fallback.getZ(),
                                    fallback.getYaw(), fallback.getPitch())
            );
            return fallback;
        }
    }

public void setArenaLobbySpawnPoint(Location lobbySpawn) {
    if (lobbySpawn == null) {
        MurderMystery.getInstance().debug("[Arena] setArenaLobbySpawnPoint(null) → KEIN Spawn gesetzt!");
        this.arenaLobbySpawnPoint = null;
        return;
    }

    this.arenaLobbySpawnPoint = lobbySpawn;
    MurderMystery.getInstance().debug(
            "[Arena] Lobby-Spawn gesetzt für Arena '" + name + "' → " +
                    String.format("(%.1f, %.1f, %.1f | Yaw=%.1f, Pitch=%.1f)",
                            lobbySpawn.getX(), lobbySpawn.getY(), lobbySpawn.getZ(),
                            lobbySpawn.getYaw(), lobbySpawn.getPitch())
    );
}

    // ---- Spectator ----
    public Location getSpectatorSpawnPoint() {
        return (spectatorSpawnPoint != null) ? spectatorSpawnPoint : getArenaLobbySpawnPoint(); // Fallback Lobby
    }
    public void setSpectatorSpawnPoint(Location spectatorSpawn) {
        this.spectatorSpawnPoint = spectatorSpawn;
    }

    // ---- Game-Spawns ----
    public Location getRandomArenaGameSpawn() {
        // 1. Vordefinierte Spielspawns
        if (arenaGameSpawnPoints != null && !arenaGameSpawnPoints.isEmpty()) {
            return arenaGameSpawnPoints.get(random.nextInt(arenaGameSpawnPoints.size()));
        }

        // 2. Region fallback
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

        // 3. Fallback: Weltspawn oder Default-Serverwelt
        return (world != null)
                ? world.getSpawnLocation()
                : Bukkit.getWorlds().get(0).getSpawnLocation();
    }

    // ---- Region ----
    public boolean hasRegion() {
        return (minX != null && maxX != null && minZ != null && maxZ != null && world != null);
    }

    // ---- Gamemode ----
    public String getGameMode() {
        return gameMode;
    }
    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }
}
