package ch.ksrminecraft.murdermystery;

import ch.ksrminecraft.murdermystery.commands.MurderMysteryCommand;
import ch.ksrminecraft.murdermystery.listeners.*;
import ch.ksrminecraft.murdermystery.managers.support.ArenaManager;
import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import ch.ksrminecraft.murdermystery.managers.game.*;
import ch.ksrminecraft.murdermystery.managers.support.MapManager;
import ch.ksrminecraft.murdermystery.model.Arena;
import ch.ksrminecraft.murdermystery.model.QuitTracker;
import ch.ksrminecraft.murdermystery.utils.MessageLimiter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MurderMystery extends JavaPlugin {

    private static MurderMystery instance;
    private PointsManager pointsManager;
    private ArenaManager arenaManager;
    private ConfigManager configManager;
    private MapManager mapManager;
    private GameManagerRegistry gameManagerRegistry;

    private boolean debugEnabled;
    private boolean murdererKilledByBow = false;

    @Override
    public void onEnable() {
        instance = this;

        // === Config laden ===
        saveDefaultConfig(); // sicherstellen, dass Datei existiert
        // Debug direkt vor dem ConfigManager laden
        this.debugEnabled = getConfig().getBoolean("debug", true);
        getLogger().info("Debug-Modus: " + (debugEnabled ? "AKTIVIERT" : "deaktiviert"));

        // ConfigManager initialisieren (nutzt bereits gesetztes debugFlag)
        this.configManager = new ConfigManager(this);

        // === RankPointsAPI prüfen ===
        if (getServer().getPluginManager().getPlugin("RankPointsAPI") == null) {
            getLogger().severe("RankPointsAPI Plugin nicht gefunden! MurderMystery wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // === Manager initialisieren ===
        this.arenaManager = new ArenaManager(this, configManager);
        this.pointsManager = new PointsManager(getLogger(), this);
        this.mapManager = new MapManager(this, arenaManager);

        // Registry für Multi-Arena
        this.gameManagerRegistry = new GameManagerRegistry(this);
        for (Arena arena : arenaManager.getAllArenas()) {
            gameManagerRegistry.registerArena(arena, pointsManager, configManager, mapManager);
        }

        // MessageLimiter
        MessageLimiter.init(this);

        // === Listener registrieren ===
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(gameManagerRegistry, mapManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(gameManagerRegistry), this);
        getServer().getPluginManager().registerEvents(new SignListener(this, gameManagerRegistry), this);
        getServer().getPluginManager().registerEvents(new DeathMessageListener(this, gameManagerRegistry), this);
        getServer().getPluginManager().registerEvents(new ItemProtectListener(configManager), this);
        getServer().getPluginManager().registerEvents(new EnvironmentProtectListener(this), this);
        getServer().getPluginManager().registerEvents(new SwordListener(this, gameManagerRegistry), this);
        getServer().getPluginManager().registerEvents(new BowListener(this, gameManagerRegistry), this);
        getServer().getPluginManager().registerEvents(new SpecialItemListener(this, gameManagerRegistry), this);
        getServer().getPluginManager().registerEvents(new AdvancementBlockListener(this, gameManagerRegistry), this);
        getServer().getPluginManager().registerEvents(new CommandBlockerListener(this, gameManagerRegistry), this);

        debug("Alle Listener erfolgreich registriert.");

        // === Command-Registrierung ===
        MurderMysteryCommand mmCommand = new MurderMysteryCommand(this, gameManagerRegistry, configManager);
        getServer().getCommandMap().register("mm", new Command("mm") {
            @Override
            public boolean execute(@NotNull CommandSender sender,
                                   @NotNull String commandLabel,
                                   @NotNull String[] args) {
                return mmCommand.onCommand(sender, this, commandLabel, args);
            }

            @Override
            public @NotNull List<String> tabComplete(@NotNull CommandSender sender,
                                                     @NotNull String alias,
                                                     @NotNull String[] args) {
                return mmCommand.onTabComplete(sender, this, alias, args);
            }
        });

        debug("MurderMystery Plugin erfolgreich aktiviert.");
    }


    @Override
    public void onDisable() {
        try {
            QuitTracker.clearAll();
        } catch (Throwable t) {
            getLogger().warning("QuitTracker konnte nicht geleert werden: " + t.getClass().getSimpleName() + " - " + t.getMessage());
        }
        getLogger().info("MurderMystery deaktiviert.");
    }

    // === Getter ===
    public static MurderMystery getInstance() { return instance; }
    public PointsManager getPointsManager() { return pointsManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public GameManagerRegistry getGameManagerRegistry() { return gameManagerRegistry; }
    public MapManager getMapManager() { return mapManager; }  // ✅ neu

    // === Status für Murderer-Bogen ===
    public boolean isMurdererKilledByBow() { return murdererKilledByBow; }
    public void setMurdererKilledByBow(boolean val) { this.murdererKilledByBow = val; }

    // === Debug ===
    public void debug(String message) {
        if (debugEnabled) {
            getLogger().info("[DEBUG] " + message);
        }
    }
    public boolean isDebugEnabled() { return debugEnabled; }
}
