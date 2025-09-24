package ch.ksrminecraft.murdermystery;

import ch.ksrminecraft.murdermystery.commands.MurderMysteryCommand;
import ch.ksrminecraft.murdermystery.commands.MurderMysteryTabCompleter;
import ch.ksrminecraft.murdermystery.listeners.*;
import ch.ksrminecraft.murdermystery.utils.ArenaManager;
import ch.ksrminecraft.murdermystery.utils.GameManager;
import ch.ksrminecraft.murdermystery.utils.PointsManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MurderMystery extends JavaPlugin {

    private static MurderMystery instance;
    private GameManager gameManager;
    private PointsManager pointsManager;
    private ArenaManager arenaManager; // <--- NEU

    // Debug-Flag aus Config
    private boolean debugEnabled;

    @Override
    public void onEnable() {
        instance = this;

        // Config laden und Defaults sicherstellen
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        reloadConfig();

        // Debug-Flag laden
        debugEnabled = getConfig().getBoolean("debug", false);
        getLogger().info("Debug-Modus: " + (debugEnabled ? "AKTIVIERT" : "deaktiviert"));

        // Config-Werte einlesen
        int minPlayers = getConfig().getInt("min-players", 3);
        int countdownTime = getConfig().getInt("countdown-seconds", 10);
        if (minPlayers < 3) {
            getLogger().severe("Config-Fehler: MIN_PLAYERS muss mindestens 3 sein. Default 3 wird angewendet.");
            minPlayers = 3;
        }

        // ArenaManager initialisieren
        arenaManager = new ArenaManager(this);

        // PointsManager initialisieren
        pointsManager = new PointsManager(getLogger(), this);

        // GameManager initialisieren
        gameManager = new GameManager(pointsManager, this);
        gameManager.setMinPlayers(minPlayers);
        gameManager.setCountdownTime(countdownTime);

        // === Listener registrieren ===
        getServer().getPluginManager().registerEvents(new GameListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(gameManager), this);

        // Item- und Umgebungsschutz
        getServer().getPluginManager().registerEvents(new ItemProtectListener(), this);
        getServer().getPluginManager().registerEvents(new EnvironmentProtectListener(this), this);

        // Waffen-Handling
        getServer().getPluginManager().registerEvents(new SwordListener(), this);
        getServer().getPluginManager().registerEvents(new BowListener(this), this);

        debug("Alle Listener erfolgreich registriert.");

        // === Command-Registrierung über plugin.yml ===
        if (getCommand("mm") != null) {
            MurderMysteryCommand mmCommand = new MurderMysteryCommand(gameManager);
            getCommand("mm").setExecutor(mmCommand);
            getCommand("mm").setTabCompleter(new MurderMysteryTabCompleter(mmCommand.getSubCommands()));
            debug("Befehl '/mm' erfolgreich registriert.");
        } else {
            getLogger().severe("Befehl '/mm' konnte nicht registriert werden – fehlt in plugin.yml?");
        }

        // RankPointsAPI Check
        if (getServer().getPluginManager().getPlugin("RankPointsAPI") == null) {
            getLogger().severe("RankPointsAPI Plugin nicht gefunden! Punkteverteilung nicht möglich.");
        }

        debug("MurderMystery Plugin erfolgreich aktiviert.");
    }

    // Plugin Instanz Getter
    public static MurderMystery getInstance() {
        return instance;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public PointsManager getPointsManager() {
        return pointsManager;
    }

    public ArenaManager getArenaManager() { // <--- NEU
        return arenaManager;
    }

    // Murderer-Bogen Kill Status
    private boolean murdererKilledByBow = false;
    public boolean isMurdererKilledByBow() {
        return murdererKilledByBow;
    }
    public void setMurdererKilledByBow(boolean murdererKilledByBow) {
        this.murdererKilledByBow = murdererKilledByBow;
    }

    // Debug
    public void debug(String message) {
        if (debugEnabled) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }
}
