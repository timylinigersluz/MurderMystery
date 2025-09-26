package ch.ksrminecraft.murdermystery.managers.support;

import ch.ksrminecraft.murdermystery.MurderMystery;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConfigManager {

    private final MurderMystery plugin;
    private FileConfiguration config;

    // --- Welten ---
    private String mainWorld;
    private String lobbyWorld;

    // --- Arenen ---
    private Map<String, ArenaConfig> arenas = new HashMap<>();

    // --- Spielparameter ---
    private int minPlayers;
    private int countdownSeconds;
    private int maxGameSeconds;

    // --- Spielmodus ---
    private String gameMode;

    // --- Punkte ---
    private int pointsKillMurderer;
    private int pointsKillInnocent;
    private int pointsKillAsMurderer;
    private int pointsSurvive;
    private int pointsWin;
    private int pointsCoWin;
    private int pointsLose;
    private int pointsQuit;

    // --- RankPointsAPI ---
    private String rankApiUrl;
    private String rankApiUser;
    private String rankApiPassword;
    private boolean rankDebug;
    private boolean excludeStaff;

    // --- Protection ---
    private boolean allowAdminMove;

    // --- Debug ---
    private boolean debug;

    public ConfigManager(MurderMystery plugin) {
        this.plugin = plugin;

        // Erstellt config.yml, falls sie noch nicht existiert
        plugin.saveDefaultConfig();
        reload();
    }

    /**
     * Lädt config.yml neu und cached die wichtigsten Werte.
     */
    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // Welten
        this.mainWorld = config.getString("worlds.main", "world");
        this.lobbyWorld = config.getString("worlds.lobby", "lobby");

        // Arenen
        arenas.clear();
        ConfigurationSection arenaSection = config.getConfigurationSection("arenas");
        if (arenaSection != null) {
            for (String key : arenaSection.getKeys(false)) {
                ConfigurationSection sec = arenaSection.getConfigurationSection(key);
                if (sec != null) {
                    String world = sec.getString("world", "world");
                    int maxPlayers = sec.getInt("maxPlayers", 12);

                    List<String> spawns = sec.getStringList("spawns");
                    Map<String, Integer> region = new HashMap<>();
                    if (sec.isConfigurationSection("region")) {
                        ConfigurationSection regSec = sec.getConfigurationSection("region");
                        for (String rKey : regSec.getKeys(false)) {
                            region.put(rKey, regSec.getInt(rKey));
                        }
                    }

                    arenas.put(key, new ArenaConfig(key, world, maxPlayers, spawns, region));
                }
            }
        }

        // Spielparameter
        this.minPlayers = config.getInt("min-players", 3);
        this.countdownSeconds = config.getInt("countdown-seconds", 15);
        this.maxGameSeconds = config.getInt("max-game-seconds", 600);

        // Gamemode
        this.gameMode = config.getString("gamemode", "classic").toLowerCase();

        // Punkte
        this.pointsKillMurderer = config.getInt("points.kill-murderer", 5);
        this.pointsKillInnocent = config.getInt("points.kill-innocent", -5);
        this.pointsKillAsMurderer = config.getInt("points.kill-as-murderer", 2);
        this.pointsSurvive = config.getInt("points.survive", 3);
        this.pointsWin = config.getInt("points.win", 5);
        this.pointsCoWin = config.getInt("points.co-win", 2);
        this.pointsLose = config.getInt("points.lose", 2);
        this.pointsQuit = config.getInt("points.quit", -3);

        // RankPointsAPI
        this.rankApiUrl = config.getString("Rank-Points-API-url");
        this.rankApiUser = config.getString("Rank-Points-API-user");
        this.rankApiPassword = config.getString("Rank-Points-API-password");
        this.rankDebug = config.getBoolean("rankpoints.debug", false);
        this.excludeStaff = config.getBoolean("rankpoints.exclude-staff", false);

        // Protection
        this.allowAdminMove = config.getBoolean("protection.allow-admin-move", true);

        // Debug
        this.debug = config.getBoolean("debug", false);
    }

    /**
     * Speichert das aktuelle Config-Objekt in die Datei.
     */
    public void save() {
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("Konnte config.yml nicht speichern: " + e.getMessage());
        }
    }

    // --- Getter ---
    public List<String> getArenaSpawns(String arenaName) {
        return config.getStringList("arenas." + arenaName + ".spawns");
    }

    public String getMainWorld() { return mainWorld; }
    public String getLobbyWorld() { return lobbyWorld; }
    public Map<String, ArenaConfig> getArenas() { return arenas; }

    public int getMinPlayers() { return minPlayers; }
    public int getCountdownSeconds() { return countdownSeconds; }
    public int getMaxGameSeconds() { return maxGameSeconds; }

    public String getGameMode() { return gameMode; }

    public int getPointsKillMurderer() { return pointsKillMurderer; }
    public int getPointsKillInnocent() { return pointsKillInnocent; }
    public int getPointsKillAsMurderer() { return pointsKillAsMurderer; }
    public int getPointsSurvive() { return pointsSurvive; }
    public int getPointsWin() { return pointsWin; }
    public int getPointsCoWin() { return pointsCoWin; }
    public int getPointsLose() { return pointsLose; }
    public int getPointsQuit() { return pointsQuit; }

    public String getRankApiUrl() { return rankApiUrl; }
    public String getRankApiUser() { return rankApiUser; }
    public String getRankApiPassword() { return rankApiPassword; }
    public boolean isRankDebug() { return rankDebug; }
    public boolean isExcludeStaff() { return excludeStaff; }

    public boolean isDebug() { return debug; }
    public boolean isAllowAdminMove() { return allowAdminMove; }

    // --- Setter + sofort speichern ---
    public void setArenaSpawns(String arenaName, List<String> spawns) {
        config.set("arenas." + arenaName + ".spawns", spawns);
        save();
        reload();
    }

    public void setGamemode(String mode) {
        config.set("gamemode", mode);
        save();
        this.gameMode = mode.toLowerCase();
    }

    public void setMinPlayers(int minPlayers) {
        config.set("min-players", minPlayers);
        save();
        this.minPlayers = minPlayers;
    }

    public void setCountdownSeconds(int countdownSeconds) {
        config.set("countdown-seconds", countdownSeconds);
        save();
        this.countdownSeconds = countdownSeconds;
    }

    public void setMaxGameSeconds(int maxGameSeconds) {
        config.set("max-game-seconds", maxGameSeconds);
        save();
        this.maxGameSeconds = maxGameSeconds;
    }

    public void setAllowAdminMove(boolean allow) {
        config.set("protection.allow-admin-move", allow);
        save();
        this.allowAdminMove = allow;
    }

    /**
     * Debug-Ausgabe nur, wenn Debug-Modus aktiv.
     */
    public void debug(String msg) {
        if (debug) {
            plugin.getLogger().info("[DEBUG] " + msg);
        }
    }

    // --- ArenaConfig Hilfsklasse ---
    public static class ArenaConfig {
        private final String name;
        private final String world;
        private final int maxPlayers;
        private final List<String> spawns;
        private final Map<String, Integer> region;

        public ArenaConfig(String name, String world, int maxPlayers, List<String> spawns, Map<String, Integer> region) {
            this.name = name;
            this.world = world;
            this.maxPlayers = maxPlayers;
            this.spawns = spawns;
            this.region = region;
        }

        public String getName() { return name; }
        public String getWorld() { return world; }
        public int getMaxPlayers() { return maxPlayers; }
        public List<String> getSpawns() { return spawns; }
        public Map<String, Integer> getRegion() { return region; }
    }
}
