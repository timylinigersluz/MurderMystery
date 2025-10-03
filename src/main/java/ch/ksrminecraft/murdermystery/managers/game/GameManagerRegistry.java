package ch.ksrminecraft.murdermystery.managers.game;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import ch.ksrminecraft.murdermystery.managers.support.MapManager;
import ch.ksrminecraft.murdermystery.model.Arena;
import ch.ksrminecraft.murdermystery.model.ArenaGame;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameManagerRegistry {

    private final MurderMystery plugin;
    private final Map<String, ArenaGame> managers = new HashMap<>();

    public GameManagerRegistry(MurderMystery plugin) {
        this.plugin = plugin;
    }

    /** ArenaGame für einen Arenanamen holen */
    public ArenaGame getGameManager(String arenaName) {
        if (arenaName == null) return null;
        ArenaGame game = managers.get(arenaName.toLowerCase());
        return game;
    }

    /** Neue Arena registrieren → Manager erstellen */
    public void registerArena(Arena arena, PointsManager pointsManager, ConfigManager configManager, MapManager mapManager) {
        if (arena == null) {
            plugin.getLogger().warning("[GameRegistry] registerArena() → Arena=null, wird ignoriert.");
            return;
        }
        String key = arena.getName().toLowerCase();
        managers.put(key, new ArenaGame(plugin, arena, pointsManager, configManager, mapManager));
        plugin.debug("[GameRegistry] ArenaGame registriert für Arena '" + arena.getName() + "'");
    }

    /** Alle Manager zurückgeben */
    public Map<String, ArenaGame> getAllManagers() {
        return managers;
    }

    /** Manager für einen Spieler ermitteln (in welcher Arena er gerade ist) */
    public ArenaGame findArenaOfPlayer(Player player) {
        if (player == null) return null;
        UUID uuid = player.getUniqueId();

        // 1. Spieler ist in einer Arena (active oder spectator)
        for (ArenaGame game : managers.values()) {
            if (game.isPlayerInGame(player)) {
                plugin.debug("[GameRegistry] Spieler " + player.getName() + " gefunden in Arena '" + game.getArena().getName() + "' (Player/Spectator-Liste).");
                return game;
            }
        }

        // 2. Fallback: Spieler steht physisch in der Welt der Arena
        String worldName = player.getWorld().getName();
        for (ArenaGame game : managers.values()) {
            if (game.getArena().getWorld().getName().equalsIgnoreCase(worldName)) {
                plugin.debug("[GameRegistry] Spieler " + player.getName() + " physisch in Arena-Welt '" + worldName + "' gefunden.");
                return game;
            }
        }

        plugin.debug("[GameRegistry] Spieler " + player.getName() + " → keine Arena gefunden.");
        return null;
    }
}