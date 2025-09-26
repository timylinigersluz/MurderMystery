package ch.ksrminecraft.murdermystery;

import ch.ksrminecraft.murdermystery.commands.MurderMysteryCommand;
import ch.ksrminecraft.murdermystery.listeners.*;
import ch.ksrminecraft.murdermystery.managers.support.ArenaManager;
import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import ch.ksrminecraft.murdermystery.managers.game.GameManager;
import ch.ksrminecraft.murdermystery.managers.game.PointsManager;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;

public class MurderMystery extends JavaPlugin {

    private static MurderMystery instance;
    private GameManager gameManager;
    private PointsManager pointsManager;
    private ArenaManager arenaManager;
    private ConfigManager configManager;

    // Debug-Flag
    private boolean debugEnabled;

    @Override
    public void onEnable() {
        instance = this;

        // ConfigManager initialisieren
        this.configManager = new ConfigManager(this);

        // Debug-Flag laden
        this.debugEnabled = configManager.isDebug();
        getLogger().info("Debug-Modus: " + (debugEnabled ? "AKTIVIERT" : "deaktiviert"));

        // ArenaManager initialisieren
        this.arenaManager = new ArenaManager(this, configManager);

        // PointsManager initialisieren
        this.pointsManager = new PointsManager(getLogger(), this);

        // GameManager initialisieren
        this.gameManager = new GameManager(pointsManager, arenaManager, this, configManager);
        gameManager.setMinPlayers(configManager.getMinPlayers());
        gameManager.setCountdownTime(configManager.getCountdownSeconds());
        gameManager.setGameMode(configManager.getGameMode());

        // === Listener registrieren ===
        getServer().getPluginManager().registerEvents(new GameListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(gameManager, configManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(gameManager, configManager), this);
        getServer().getPluginManager().registerEvents(new SignListener(this), this);

        // Item- und Umgebungsschutz
        getServer().getPluginManager().registerEvents(new ItemProtectListener(configManager), this);
        getServer().getPluginManager().registerEvents(new EnvironmentProtectListener(this), this);

        // Waffen-Handling
        getServer().getPluginManager().registerEvents(new SwordListener(this), this);
        getServer().getPluginManager().registerEvents(new BowListener(this), this);

        debug("Alle Listener erfolgreich registriert.");

        // === Command-Registrierung ===
        CommandMap commandMap = getServer().getCommandMap();
        commandMap.register("murdermystery", new MurderMysteryCommand(gameManager, configManager));

        debug("Befehl '/mm' erfolgreich registriert.");

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

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
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
