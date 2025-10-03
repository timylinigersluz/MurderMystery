package ch.ksrminecraft.murdermystery.managers.support;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.model.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Verwaltet das Laden/Speichern der Plugin-Konfiguration.
 * Reihenfolge: Felder → Getter → Setter → Öffentliche API → Hilfsmethoden → Innere Klassen
 */
public class ConfigManager {

    // ==========================
    // Felder
    // ==========================
    private final MurderMystery plugin;
    private FileConfiguration config;

    // --- MainWorld ---
    private String mainWorld;
    private Location mainWorldLobbySpawnPoint;

    // --- Arenen ---
    private Map<String, ArenaConfig> arenas = new HashMap<>();

    // --- Spielparameter ---
    private int minPlayers;
    private int countdownSeconds;
    private int maxGameSeconds;

    // --- Player Gamemode ---
    private String playerGameMode;

    // --- Punkte ---
    private int pointsKillMurderer;
    private int pointsKillInnocent;
    private int pointsKillAsMurderer;
    private int pointsSurvive;
    private int pointsWin;
    private int pointsCoWin;
    private int pointsLose;
    private int pointsConsolation;
    private int pointsQuit;
    private int pointsTimeUp;

    // --- Protection ---
    private boolean allowAdminMove;

    // ==========================
    // Konstruktor
    // ==========================
    public ConfigManager(MurderMystery plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        reload();
    }

    // ==========================
    // Getter
    // ==========================
    public String getMainWorld() { return mainWorld; }
    public Location getMainWorldLobbySpawnPoint() { return mainWorldLobbySpawnPoint; }
    public Map<String, ArenaConfig> getArenas() { return arenas; }

    public int getMinPlayers() { return minPlayers; }
    public int getCountdownSeconds() { return countdownSeconds; }
    public int getMaxGameSeconds() { return maxGameSeconds; }

    public int getPointsKillMurderer() { return pointsKillMurderer; }
    public int getPointsKillInnocent() { return pointsKillInnocent; }
    public int getPointsKillAsMurderer() { return pointsKillAsMurderer; }
    public int getPointsSurvive() { return pointsSurvive; }
    public int getPointsWin() { return pointsWin; }
    public int getPointsCoWin() { return pointsCoWin; }
    public int getPointsLose() { return pointsLose; }
    public int getPointsConsolation() { return pointsConsolation; }
    public int getPointsQuit() { return pointsQuit; }
    public int getPointsTimeUp() { return pointsTimeUp; }

    public boolean isAllowAdminMove() { return allowAdminMove; }

    public org.bukkit.GameMode getPlayerGameMode() {
        return switch (playerGameMode) {
            case "adventure" -> org.bukkit.GameMode.ADVENTURE;
            case "creative" -> org.bukkit.GameMode.CREATIVE;
            default -> org.bukkit.GameMode.SURVIVAL;
        };
    }

    // ==========================
    // Setter
    // ==========================
    /** Haupt-Lobby setzen (persistiert und reloadet). */
    public void setMainLobby(Location loc) {
        String locStr = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()
                + "," + loc.getYaw() + "," + loc.getPitch();
        config.set("main-lobby-spawn", locStr);
        config.set("worlds.main", loc.getWorld().getName());
        save();
        reload();
        plugin.debug("[ConfigManager] MainWorldLobby gesetzt: " + locStr);
    }

    /** Arena-Lobby setzen (persistiert und reloadet). */
    public void setArenaLobby(String arenaName, Location loc) {
        String locStr = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()
                + "," + loc.getYaw() + "," + loc.getPitch();
        config.set("arenas." + arenaName + ".lobby", locStr);
        config.set("arenas." + arenaName + ".world", loc.getWorld().getName());
        save();
        reload();
        plugin.debug("[ConfigManager] ArenaLobby gesetzt für " + arenaName + ": " + locStr);
    }

    /** Arena-GameSpawns setzen (persistiert und reloadet). */
    public void setArenaGameSpawns(String arenaName, List<String> spawns) {
        String path = "arenas." + arenaName + ".spawns";
        config.set(path, spawns);
        save();
        reload();
        plugin.debug("[ConfigManager] setArenaGameSpawns(" + arenaName + ") → " + spawns.size() + " Einträge gespeichert");
    }

    /** Arena-Modus setzen (persistiert und reloadet). */
    public void setArenaGameMode(String arenaName, String mode) {
        config.set("arenas." + arenaName + ".gamemode", mode.toLowerCase());
        save();
        reload();
        plugin.debug("[ConfigManager] setArenaGameMode(" + arenaName + ") → " + mode);
    }

    // ==========================
    // Öffentliche API (Laden/Speichern, Arena-Zugriffe)
    // ==========================
    /** Konfiguration neu laden. */
    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        plugin.debug("[ConfigManager] Reload gestartet...");

        // === MainWorld laden ===
        this.mainWorld = config.getString("worlds.main", "world");
        plugin.debug("[ConfigManager] MainWorld geladen: " + mainWorld);

        // === MainWorld-Lobby laden ===
        String mainLobbyStr = config.getString("main-lobby-spawn", null);
        plugin.debug("[ConfigManager] MainLobbyRaw=" + mainLobbyStr);
        if (mainLobbyStr != null) {
            this.mainWorldLobbySpawnPoint = parseLocation(mainWorld, mainLobbyStr);
        }

        // === Player Gamemode ===
        this.playerGameMode = config.getString("player-gamemode", "survival").toLowerCase();

        // === Arenen laden ===
        arenas.clear();
        ConfigurationSection arenaSection = config.getConfigurationSection("arenas");
        if (arenaSection != null) {
            for (String key : arenaSection.getKeys(false)) {
                ConfigurationSection sec = arenaSection.getConfigurationSection(key);
                if (sec != null) {
                    String world = sec.getString("world", "world");
                    int maxPlayers = sec.getInt("maxPlayers", 12);
                    String size = sec.getString("size", "unspecified").toLowerCase();
                    List<String> spawns = sec.getStringList("spawns");

                    Map<String, Integer> region = new HashMap<>();
                    if (sec.isConfigurationSection("region")) {
                        ConfigurationSection regSec = sec.getConfigurationSection("region");
                        for (String rKey : regSec.getKeys(false)) {
                            region.put(rKey, regSec.getInt(rKey));
                        }
                    }

                    // Rohdaten Lobby-String speichern (nicht parsen!)
                    String rawLobby = null;
                    Object rawLobbyObj = sec.get("lobby");
                    if (rawLobbyObj instanceof String str) {
                        rawLobby = str;
                    } else if (rawLobbyObj instanceof List<?> list && !list.isEmpty()) {
                        if (list.get(0) instanceof String s) rawLobby = s;
                    }

                    arenas.put(key, new ArenaConfig(
                            key, world, maxPlayers, size, spawns, region, rawLobby
                    ));

                    plugin.debug("[ConfigManager] Arena geladen: " + key +
                            " (world=" + world +
                            ", maxPlayers=" + maxPlayers +
                            ", size=" + size +
                            ", spawns=" + (spawns != null ? spawns.size() : 0) +
                            ", rawLobby=" + rawLobby + ")");
                }
            }
        }

        plugin.debug("[ConfigManager] Reload abgeschlossen.");


    // === Spielparameter ===
        this.minPlayers = config.getInt("min-players", 3);
        this.countdownSeconds = config.getInt("countdown-seconds", 15);
        this.maxGameSeconds = config.getInt("max-game-seconds", 600);
        plugin.debug("[ConfigManager] Spielparameter: minPlayers=" + minPlayers +
                ", countdown=" + countdownSeconds + "s, maxGame=" + maxGameSeconds + "s");

        // === Punkte ===
        this.pointsKillMurderer = config.getInt("points.kill-murderer", 5);
        this.pointsKillInnocent = config.getInt("points.kill-innocent", -5);
        this.pointsKillAsMurderer = config.getInt("points.kill-as-murderer", 2);
        this.pointsSurvive = config.getInt("points.survive", 3);
        this.pointsWin = config.getInt("points.win", 5);
        this.pointsCoWin = config.getInt("points.co-win", 2);
        this.pointsLose = config.getInt("points.lose", 2);
        this.pointsConsolation = config.getInt("points.consolation", 2);
        this.pointsQuit = config.getInt("points.quit", -3);
        this.pointsTimeUp = config.getInt("points.time-up", 3);

        plugin.debug("[ConfigManager] Punkte geladen: " +
                "kill-murderer=" + pointsKillMurderer +
                ", kill-innocent=" + pointsKillInnocent +
                ", kill-as-murderer=" + pointsKillAsMurderer +
                ", survive=" + pointsSurvive +
                ", win=" + pointsWin +
                ", co-win=" + pointsCoWin +
                ", lose=" + pointsLose +
                ", consolation=" + pointsConsolation +
                ", quit=" + pointsQuit +
                ", time-up=" + pointsTimeUp);

        // === Protection & Debug ===
        this.allowAdminMove = config.getBoolean("protection.allow-admin-move", true);
        plugin.debug("[ConfigManager] Protection: allow-admin-move=" + allowAdminMove);

        // === RankPointsAPI Settings ===
        String rankUrl = config.getString("Rank-Points-API-url", "none");
        String rankUser = config.getString("Rank-Points-API-user", "none");
        boolean rankDebug = config.getBoolean("rankpoints.debug", false);
        boolean excludeStaff = config.getBoolean("rankpoints.exclude-staff", false);

        plugin.debug("[ConfigManager] RankPointsAPI Config: url=" + rankUrl +
                ", user=" + rankUser +
                ", debug=" + rankDebug +
                ", exclude-staff=" + excludeStaff);

        plugin.debug("[ConfigManager] Reload abgeschlossen.");
    }



    /** Konfiguration speichern. */
    public void save() {
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
            plugin.debug("[ConfigManager] Config gespeichert.");
        } catch (IOException e) {
            plugin.getLogger().severe("Konnte config.yml nicht speichern: " + e.getMessage());
        }
    }

    /** Arena-GameSpawns abfragen (nur lesen). */
    public List<String> getArenaGameSpawns(String arenaName) {
        String path = "arenas." + arenaName + ".spawns";
        List<String> list = config.getStringList(path);
        plugin.debug("[ConfigManager] getArenaGameSpawns(" + arenaName + ") → " + (list != null ? list.size() : 0) + " Einträge");
        return (list != null) ? list : new ArrayList<>();
    }

    /** Arena-spezifische Konfiguration in das Arena-Objekt laden. */
    public void loadArenaConfig(Arena arena) {
        String basePath = "arenas." + arena.getName();

        // === Gamemode ===
        String mode = config.getString(basePath + ".gamemode", "classic");
        arena.setGameMode(mode);
        plugin.debug("[ConfigManager] loadArenaConfig(" + arena.getName() + ") → Gamemode=" + mode);

        // === Lobby-Spawn ===
        String worldName = config.getString(basePath + ".world", arena.getWorld().getName());
        Location lobbyLoc = null;

        Object rawLobby = config.get(basePath + ".lobby");
        if (rawLobby instanceof String str) {
            plugin.debug("[ConfigManager] loadArenaConfig(" + arena.getName() + ") Lobby als String: " + str);
            lobbyLoc = parseLocation(worldName, str);
        } else if (rawLobby instanceof List<?>) {
            List<?> list = (List<?>) rawLobby;
            plugin.debug("[ConfigManager] loadArenaConfig(" + arena.getName() + ") Lobby als Liste: " + list);
            if (!list.isEmpty() && list.get(0) instanceof String s) {
                lobbyLoc = parseLocation(worldName, s);
            }
        } else {
            plugin.debug("[ConfigManager] loadArenaConfig(" + arena.getName() + ") Lobby nicht gesetzt oder unbekannter Typ: "
                    + (rawLobby != null ? rawLobby.getClass().getName() : "null"));
        }

        if (lobbyLoc != null) {
            arena.setArenaLobbySpawnPoint(lobbyLoc);
            plugin.debug("[ConfigManager] LobbyLoc erfolgreich gesetzt → "
                    + String.format("(%.1f, %.1f, %.1f | Yaw=%.1f, Pitch=%.1f)",
                    lobbyLoc.getX(), lobbyLoc.getY(), lobbyLoc.getZ(),
                    lobbyLoc.getYaw(), lobbyLoc.getPitch()));
        } else {
            plugin.debug("[ConfigManager] Kein gültiger Lobby-Spawn definiert für Arena=" + arena.getName());
        }

        // === Spectator-Spawn ===
        List<String> spawns = config.getStringList(basePath + ".spawns");
        plugin.debug("[ConfigManager] loadArenaConfig(" + arena.getName() + ") → SpawnsRaw=" + spawns);

        if (spawns != null && !spawns.isEmpty()) {
            for (String s : spawns) {
                if (s.toUpperCase().endsWith(",SPECTATOR")) {
                    String raw = s.substring(0, s.lastIndexOf(",SPECTATOR"));
                    Location specLoc = parseLocation(worldName, raw);
                    if (specLoc != null) {
                        arena.setSpectatorSpawnPoint(specLoc);
                        plugin.debug("[ConfigManager] SpectatorLoc gesetzt → "
                                + String.format("(%.1f, %.1f, %.1f | Yaw=%.1f, Pitch=%.1f)",
                                specLoc.getX(), specLoc.getY(), specLoc.getZ(),
                                specLoc.getYaw(), specLoc.getPitch()));
                    } else {
                        plugin.debug("[ConfigManager] SpectatorLoc PARSE-ERROR für String=" + raw);
                    }
                }
            }
        }
    }


    // ==========================
    // Hilfsmethoden
    // ==========================
    public void debug(String msg) {
        if (plugin.isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] " + msg);
        }
    }

    /** Location aus "x,y,z[,yaw[,pitch]]" in einer gegebenen Welt parsen. */
    public Location parseLocation(String worldName, String locStr) {
        String[] parts = locStr.split(",");
        if (parts.length < 3) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        try {
            double x = Double.parseDouble(parts[0].trim());
            double y = Double.parseDouble(parts[1].trim());
            double z = Double.parseDouble(parts[2].trim());
            float yaw = parts.length > 3 ? Float.parseFloat(parts[3].trim()) : 0f;
            float pitch = parts.length > 4 ? Float.parseFloat(parts[4].trim()) : 0f;
            Location loc = new Location(world, x, y, z, yaw, pitch);
            debug("[ConfigManager] parseLocation → " + loc);
            return loc;
        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Parsen einer Location: " + locStr + " (" + e.getMessage() + ")");
            return null;
        }
    }

    // ==========================
    // Innere Klassen
    // ==========================
    /** Immutable + optionaler Lobby-Spawn, der geändert werden kann. */
    public static class ArenaConfig {
        private final String name;
        private final String world;
        private final int maxPlayers;
        private final String size;
        private final List<String> arenaGameSpawnPoints;
        private final Map<String, Integer> region;
        private final String rawLobby; // 🔹 neu

        public ArenaConfig(String name,
                           String world,
                           int maxPlayers,
                           String size,
                           List<String> arenaGameSpawnPoints,
                           Map<String, Integer> region,
                           String rawLobby) {
            this.name = name;
            this.world = world;
            this.maxPlayers = maxPlayers;
            this.size = size;
            this.arenaGameSpawnPoints = arenaGameSpawnPoints;
            this.region = region;
            this.rawLobby = rawLobby;
        }

        // Getter
        public String getName() { return name; }
        public String getWorld() { return world; }
        public int getMaxPlayers() { return maxPlayers; }
        public String getSize() { return size; }
        public List<String> getArenaGameSpawnPoints() { return arenaGameSpawnPoints; }
        public Map<String, Integer> getRegion() { return region; }
        public String getRawLobby() { return rawLobby; }
    }
}
