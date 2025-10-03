package ch.ksrminecraft.murdermystery.managers.game;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.listeners.SignListener;
import ch.ksrminecraft.murdermystery.managers.effects.Broadcaster;
import ch.ksrminecraft.murdermystery.managers.support.ArenaManager;
import ch.ksrminecraft.murdermystery.managers.support.BossBarManager;
import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import ch.ksrminecraft.murdermystery.managers.support.GameTimerManager;
import ch.ksrminecraft.murdermystery.model.ArenaGame;
import ch.ksrminecraft.murdermystery.model.Role;
import ch.ksrminecraft.murdermystery.model.RoundStats;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Abstrakter Manager für eine MurderMystery-Instanz (z. B. Arena).
 * ArenaGameManager erweitert diese Klasse für konkrete Arenen.
 */
public class GameManager {

    protected final MurderMystery plugin;
    protected final PointsManager pointsManager;
    protected final PlayerManager playerManager;
    protected final ArenaManager arenaManager;
    protected final ConfigManager configManager;

    // Spieler-Sets
    protected final Set<UUID> players = new HashSet<>();
    protected final Set<UUID> spectators = new HashSet<>();

    // Rollen pro Spieler
    protected final Map<UUID, Role> roles = new HashMap<>();

    // Manager
    protected final BossBarManager bossBarManager;
    protected final GameTimerManager gameTimerManager;

    // Spielstatus
    protected boolean gameStarted = false;
    protected int minPlayers;
    protected int countdownTime;
    protected GameMode playerGameMode;

    // Stats
    protected RoundStats roundStats;

    public GameManager(PointsManager pointsManager,
                       ArenaManager arenaManager,
                       MurderMystery plugin,
                       ConfigManager configManager) {
        this.plugin = plugin;
        this.pointsManager = pointsManager;
        this.arenaManager = arenaManager;
        this.configManager = configManager;

        this.playerManager = new PlayerManager(this, plugin, arenaManager, configManager);
        this.bossBarManager = new BossBarManager(this);
        this.gameTimerManager = new GameTimerManager(this, plugin);

        // Defaults aus Config
        this.minPlayers = configManager.getMinPlayers();
        this.countdownTime = configManager.getCountdownSeconds();
        this.playerGameMode = configManager.getPlayerGameMode();

        plugin.debug("[GameManager] Instanz erstellt → minPlayers=" + minPlayers
                + ", countdown=" + countdownTime
                + ", gameMode=" + playerGameMode);
    }

    // -------------------- Getter --------------------
    public Set<UUID> getPlayers() { return players; }
    public Set<UUID> getSpectators() { return spectators; }
    public Map<UUID, Role> getRoles() { return roles; }

    public PlayerManager getPlayerManager() { return playerManager; }
    public BossBarManager getBossBarManager() { return bossBarManager; }
    public GameTimerManager getGameTimerManager() { return gameTimerManager; }
    public PointsManager getPointsManager() { return pointsManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public ArenaManager getArenaManager() { return arenaManager; }

    public boolean isGameStarted() { return gameStarted; }
    public int getMinPlayers() { return minPlayers; }
    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
        plugin.debug("[GameManager] minPlayers gesetzt auf " + minPlayers);
    }

    public int getCountdownTime() { return countdownTime; }
    public void setCountdownTime(int countdownTime) {
        this.countdownTime = countdownTime;
        plugin.debug("[GameManager] countdownTime gesetzt auf " + countdownTime);
    }

    public GameMode getPlayerGameMode() { return playerGameMode; }
    public void setPlayerGameMode(GameMode gm) {
        this.playerGameMode = gm;
        plugin.debug("[GameManager] playerGameMode geändert zu " + gm);
    }

    public RoundStats getOrCreateRoundStats() {
        if (roundStats == null) {
            roundStats = new RoundStats();
            plugin.debug("[GameManager] Neue RoundStats erstellt.");
        }
        return roundStats;
    }

    // -------------------- Spiel-Flow --------------------
    public void startCountdown() {
        Broadcaster.broadcastMessage(players, ChatColor.YELLOW + "Der Countdown startet bald...");
        plugin.debug("[GameManager] startCountdown() → Spieler=" + players.size());
    }

    public void startGame() {
        this.gameStarted = true;
        this.roundStats = new RoundStats();
        plugin.debug("[GameManager] startGame() → Spiel gestartet, Spieler=" + players.size());
    }

    public void endRound(RoundResultManager.EndCondition condition) {
        this.gameStarted = false;
        plugin.debug("[GameManager] endRound() → Beende Runde mit Condition=" + condition);
        resetGame();
    }

    public void checkWinConditions() {
        // wird in ArenaGameManager implementiert
        plugin.debug("[GameManager] checkWinConditions() → Platzhalter (in ArenaGame überschrieben)");
    }

    public void resetGame() {
        int playerCount = players.size();
        int spectatorCount = spectators.size();

        // --- Items in der Arena-Welt aufräumen ---
        if (this instanceof ArenaGame arenaGame) {
            var world = arenaGame.getArena().getWorld();
            if (world != null) {
                long removed = world.getEntities().stream()
                        .filter(e -> e instanceof org.bukkit.entity.Item)
                        .peek(org.bukkit.entity.Entity::remove)
                        .count();
                plugin.debug("[GameManager] resetGame() → " + removed + " gedroppte Items in Arena '"
                        + arenaGame.getArena().getName() + "' entfernt.");
            }
        }

        // --- Spielerlisten leeren ---
        players.clear();
        spectators.clear();
        roles.clear();
        roundStats = null;
        gameStarted = false;

        // --- BossBars stoppen ---
        bossBarManager.cancelGameBar();
        bossBarManager.cancelLobbyBar();

        // --- Join-Signs aktualisieren ---
        SignListener.updateJoinSigns(plugin, plugin.getGameManagerRegistry());

        plugin.debug("[GameManager] resetGame() → Arena zurückgesetzt. Spieler="
                + playerCount + ", Spectators=" + spectatorCount + " (vor Reset)");
    }

    public void handleTimeout() {
        // Standard: Runde mit TIME_UP beenden
        plugin.debug("[GameManager] handleTimeout() → Timeout erkannt, Runde endet als Unentschieden.");

        // EndCondition setzen
        RoundResultManager.EndCondition condition = RoundResultManager.EndCondition.TIME_UP;

        // Runde sauber beenden
        endRound(condition);
    }

    public void eliminate(Player victim, Player killer) {
        UUID uuid = victim.getUniqueId();

        // Spieler aus aktiven Spielern entfernen und zu Spectators verschieben
        if (players.remove(uuid)) {
            spectators.add(uuid);
        }

        // Rolle beibehalten oder ändern?
        // Für Spectators entfernen wir z. B. die Items
        victim.getInventory().clear();
        victim.setGameMode(GameMode.SPECTATOR);

        // BossBar-Update
        bossBarManager.removePlayer(victim);
        bossBarManager.addPlayer(victim, BossBarManager.Mode.GAME);

        // Debug-Log
        String killerName = (killer != null ? killer.getName() : "Umwelt");
        plugin.debug("[GameManager] eliminate() → Opfer=" + victim.getName() + ", Killer=" + killerName);

        // Option: hier Statistiken eintragen
        getOrCreateRoundStats().addKill(killer != null ? killer.getUniqueId() : null, uuid);

        // Siegbedingungen prüfen
        checkWinConditions();
    }

    public MurderMystery getPlugin() {
        return plugin;
    }
}
