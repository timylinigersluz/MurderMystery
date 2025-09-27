package ch.ksrminecraft.murdermystery;

import ch.ksrminecraft.murdermystery.commands.MurderMysteryCommand;
import ch.ksrminecraft.murdermystery.listeners.*;
import ch.ksrminecraft.murdermystery.managers.support.ArenaManager;
import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import ch.ksrminecraft.murdermystery.managers.game.GameManager;
import ch.ksrminecraft.murdermystery.managers.game.PointsManager;
import ch.ksrminecraft.murdermystery.utils.MessageLimiter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MurderMystery extends JavaPlugin {

    private static MurderMystery instance;
    private GameManager gameManager;
    private PointsManager pointsManager;
    private ArenaManager arenaManager;
    private ConfigManager configManager;

    private boolean debugEnabled;
    private boolean murdererKilledByBow = false;

    @Override
    public void onEnable() {
        instance = this;

        // === Config laden ===
        this.configManager = new ConfigManager(this);
        this.debugEnabled = configManager.isDebug();
        getLogger().info("Debug-Modus: " + (debugEnabled ? "AKTIVIERT" : "deaktiviert"));

        // === Manager initialisieren ===
        this.arenaManager = new ArenaManager(this, configManager);
        this.pointsManager = new PointsManager(getLogger(), this);
        this.gameManager = new GameManager(pointsManager, arenaManager, this, configManager);
        gameManager.setMinPlayers(configManager.getMinPlayers());
        gameManager.setCountdownTime(configManager.getCountdownSeconds());
        gameManager.setGameMode(configManager.getGameMode());

        // MessageLimiter
        MessageLimiter.init(this);

        // === Listener registrieren ===
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(gameManager, configManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(gameManager, configManager), this);
        getServer().getPluginManager().registerEvents(new SignListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathMessageListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemProtectListener(configManager), this);
        getServer().getPluginManager().registerEvents(new EnvironmentProtectListener(this), this);
        getServer().getPluginManager().registerEvents(new SwordListener(this), this);
        getServer().getPluginManager().registerEvents(new BowListener(this), this);
        getServer().getPluginManager().registerEvents(new SpecialItemListener(this), this);

        debug("Alle Listener erfolgreich registriert.");

        // === Command-Registrierung ===
        MurderMysteryCommand mmCommand = new MurderMysteryCommand(gameManager, configManager);
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

        // RankPointsAPI Check
        if (getServer().getPluginManager().getPlugin("RankPointsAPI") == null) {
            getLogger().severe("RankPointsAPI Plugin nicht gefunden! Punkteverteilung nicht möglich.");
        }

        debug("MurderMystery Plugin erfolgreich aktiviert.");
    }

    // === Getter ===
    public static MurderMystery getInstance() { return instance; }
    public GameManager getGameManager() { return gameManager; }
    public PointsManager getPointsManager() { return pointsManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public ConfigManager getConfigManager() { return configManager; }

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
