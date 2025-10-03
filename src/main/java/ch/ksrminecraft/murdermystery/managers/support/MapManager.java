package ch.ksrminecraft.murdermystery.managers.support;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.model.Arena;
import org.bukkit.*;
import org.bukkit.entity.Player;

public class MapManager {

    private final MurderMystery plugin;
    private final ArenaManager arenaManager;
    private final ConfigManager configManager;

    public MapManager(MurderMystery plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.configManager = plugin.getConfigManager();
    }

    /** Spieler in die MainWorldLobby teleportieren */
    public void teleportToMainLobby(Player p) {
        if (p == null || !p.isOnline()) return;

        Location lobby = configManager.getMainWorldLobbySpawnPoint(); // ✅ korrigiert
        if (lobby == null) {
            World world = Bukkit.getWorld(configManager.getMainWorld());
            lobby = (world != null) ? world.getSpawnLocation() : Bukkit.getWorlds().get(0).getSpawnLocation();
        }

        teleportPlayer(p, lobby, "MainWorldLobby");
        p.setGameMode(GameMode.SURVIVAL);
    }

    /** Spieler in die ArenaLobby teleportieren */
    public void teleportToArenaLobby(Player player, Arena arena) {
        Location lobby = arena.getArenaLobbySpawnPoint();
        plugin.debug("[MapManager] Teleportiere " + player.getName() +
                " → ArenaLobby '" + arena.getName() + "' @ " +
                (lobby != null ? String.format("(%.1f, %.1f, %.1f | Yaw=%.1f, Pitch=%.1f)",
                        lobby.getX(), lobby.getY(), lobby.getZ(),
                        lobby.getYaw(), lobby.getPitch()) : "null"));

        if (lobby != null) {
            player.teleport(lobby);
        } else {
            player.sendMessage(ChatColor.RED + "Lobby-Spawn nicht definiert!");
        }
    }

    /** Spieler auf einen zufälligen ArenaGameSpawn teleportieren */
    public void teleportToArenaGameSpawn(Player p, Arena arena) {
        if (p == null || !p.isOnline() || arena == null) return;

        Location spawn = arena.getRandomArenaGameSpawn();
        teleportPlayer(p, spawn, "ArenaGameSpawn '" + arena.getName() + "'");
        p.setGameMode(GameMode.SURVIVAL);
    }

    /** Spieler auf den SpectatorSpawn teleportieren */
    public void teleportToSpectatorSpawn(Player p, Arena arena) {
        if (p == null || !p.isOnline() || arena == null) return;

        Location spec = arena.getSpectatorSpawnPoint();
        teleportPlayer(p, spec, "SpectatorSpawn '" + arena.getName() + "'");
        p.setGameMode(GameMode.SPECTATOR);
    }

    public void teleportPlayer(Player p, Location location, String context) {
        if (location == null) {
            plugin.getLogger().warning("[MapManager] Teleport fehlgeschlagen → Location null (" + context + ")");
            return;
        }
        p.teleport(location);
        plugin.debug("[MapManager] Spieler " + p.getName() + " → " + context +
                " @ " + String.format("(%.1f, %.1f, %.1f)", location.getX(), location.getY(), location.getZ()));
    }
}
