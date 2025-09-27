package ch.ksrminecraft.murdermystery.managers.game;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.effects.Broadcaster;
import ch.ksrminecraft.murdermystery.managers.effects.CelebrationManager;
import ch.ksrminecraft.murdermystery.managers.effects.ItemManager;
import ch.ksrminecraft.murdermystery.managers.support.*;
import ch.ksrminecraft.murdermystery.model.Role;
import ch.ksrminecraft.murdermystery.model.RoundStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public class GameManager {

    private final MurderMystery plugin;
    private final PointsManager pointsManager;
    private final PlayerManager playerManager;
    private final CountdownManager countdownManager;
    private final WinConditionManager winConditionManager;
    private final BossBarManager bossBarManager;
    private final ArenaManager arenaManager;
    private final ConfigManager configManager;

    // Neue Manager
    private final GameTimerManager gameTimerManager;
    private final FailSafeManager failSafeManager;
    private final CelebrationManager celebrationManager;
    private final RoundResultManager roundResultManager;

    private final Set<UUID> players = new HashSet<>();     // lebende Spieler
    private final Set<UUID> spectators = new HashSet<>();  // ausgeschiedene Spieler
    private final Map<UUID, Role> roles = new HashMap<>();
    private boolean gameStarted = false;

    private int minPlayers;
    private int countdownTime;

    private RoundStats roundStats;
    private String gameMode;

    // Arena-Size-Wahl speichern
    private String chosenArenaSize = null;

    public GameManager(PointsManager pointsManager,
                       ArenaManager arenaManager,
                       MurderMystery plugin,
                       ConfigManager configManager) {
        this.pointsManager = pointsManager;
        this.arenaManager = arenaManager;
        this.plugin = plugin;
        this.configManager = configManager;

        // Initiale Config-Werte laden
        reloadFromConfig(configManager);

        this.bossBarManager = new BossBarManager(plugin);
        this.countdownManager = new CountdownManager(this, plugin);
        this.winConditionManager = new WinConditionManager(this, pointsManager, plugin);
        this.playerManager = new PlayerManager(this, plugin, arenaManager, configManager);

        // Neue Manager initialisieren
        this.gameTimerManager = new GameTimerManager(this, plugin);
        this.failSafeManager = new FailSafeManager(this, plugin);
        this.celebrationManager = new CelebrationManager(plugin);
        this.roundResultManager = new RoundResultManager(plugin, pointsManager);
    }

    // ----------------- Config -----------------
    public void reloadFromConfig(ConfigManager configManager) {
        this.minPlayers = configManager.getMinPlayers();
        this.countdownTime = configManager.getCountdownSeconds();
        this.gameMode = configManager.getGameMode();

        plugin.debug("GameManager reload → MinPlayers=" + minPlayers +
                ", Countdown=" + countdownTime +
                ", GameMode=" + gameMode);
    }

    // ----------------- Spieler-Handling -----------------
    public void handleJoin(Player player) { playerManager.handleJoin(player); }

    public void handleJoin(Player player, String size) {
        this.chosenArenaSize = size;
        playerManager.handleJoin(player, size);
    }

    public void handleLeave(Player player) {
        playerManager.handleLeave(player);
        if (gameStarted && roundStats != null) {
            roundStats.markQuitter(player.getUniqueId());
            pointsManager.applyPenalty(player.getUniqueId(),
                    Math.abs(configManager.getPointsQuit()),
                    "Spiel verlassen");
        }
    }

    public void eliminate(Player victim) { playerManager.eliminate(victim, null); }

    public void eliminate(Player victim, Player killer) { playerManager.eliminate(victim, killer); }

    // ----------------- Countdown & Start -----------------
    public void startCountdown() {
        countdownManager.setCountdownTime(countdownTime);
        countdownManager.startCountdown();
    }

    public void startGame() {
        if (gameStarted) return;
        gameStarted = true;

        roundStats = new RoundStats();

        playerManager.startGame(players, roles, chosenArenaSize);
        chosenArenaSize = null;

        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && roles.containsKey(uuid)) {
                showRoleTitle(p, roles.get(uuid));
            }
        }

        gameTimerManager.start(configManager.getMaxGameSeconds());
        failSafeManager.start();
    }

    public void checkWinConditions() { winConditionManager.checkWinConditions(players, roles, roundStats); }

    // ----------------- Runden-Ende -----------------
    public void endRound(RoundResultManager.EndCondition condition) {
        if (roundStats != null) {
            // (1) Teleport alle Spieler zurück in Main-World (inkl. Reset)
            Set<UUID> allPlayers = new HashSet<>();
            allPlayers.addAll(players);
            allPlayers.addAll(spectators);
            playerManager.resetGame(allPlayers);

            // (2) Titel-Anzeige
            for (UUID uuid : allPlayers) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    boolean isWinner = isWinner(uuid, condition);
                    if (isWinner) {
                        Broadcaster.sendTitle(p, "§aSieg!", "Gut gemacht!");
                    } else {
                        Broadcaster.sendTitle(p, "§cNiederlage!", "Versuch's nochmal!");
                    }
                }
            }

            // (3) Persönliche Stats + Punkte
            roundResultManager.handleRoundEnd(condition, roles, roundStats);

            // (4) Feuerwerk für Sieger
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (UUID uuid : allPlayers) {
                    if (!isWinner(uuid, condition)) continue;
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        celebrationManager.launchFireworks(p);
                    }
                }
            }, 40L);
        }
        resetGame();
    }

    public void handleTimeout() {
        winConditionManager.forceTimeoutEnd(players, roles, roundStats);
    }

    public void resetGame() {
        gameStarted = false;

        // Sets und Rollen zurücksetzen
        players.clear();
        spectators.clear();
        roles.clear();
        RoleManager.clearRoles();

        // Manager stoppen
        gameTimerManager.stop();
        failSafeManager.stop();
        roundStats = null;

        plugin.setMurdererKilledByBow(false);
        plugin.debug("resetGame() abgeschlossen → Spieler, Items & Rollen aufgeräumt.");
    }

    // ----------------- Hilfsmethoden -----------------
    private void showRoleTitle(Player p, Role role) {
        String title;
        String subtitle;

        switch (role) {
            case MURDERER -> {
                title = ChatColor.DARK_RED + "" + ChatColor.BOLD + "MÖRDER";
                subtitle = ChatColor.GRAY + "Eliminiere alle Unschuldigen!";
            }
            case DETECTIVE -> {
                title = ChatColor.BLUE + "" + ChatColor.BOLD + "DETEKTIV";
                subtitle = ChatColor.GRAY + "Finde und stoppe den Mörder!";
            }
            default -> {
                title = ChatColor.GREEN + "" + ChatColor.BOLD + "UNSCHULDIGER";
                subtitle = ChatColor.GRAY + "Überlebe so lange wie möglich!";
            }
        }
        p.sendTitle(title, subtitle, 10, 60, 10);
    }

    private boolean isWinner(UUID uuid, RoundResultManager.EndCondition condition) {
        Role role = roles.get(uuid);
        if (role == null) return false;
        return switch (condition) {
            case MURDERER_WIN -> role == Role.MURDERER;
            case DETECTIVE_WIN -> role == Role.DETECTIVE || role == Role.BYSTANDER;
            case TIME_UP -> false; // Unentschieden = niemand
        };
    }

    public RoundStats getOrCreateRoundStats() {
        if (roundStats == null) {
            roundStats = new RoundStats();
        }
        return roundStats;
    }

    // ----------------- Getter & Setter -----------------
    public boolean isGameStarted() { return gameStarted; }
    public Set<UUID> getPlayers() { return players; }
    public Set<UUID> getSpectators() { return spectators; }
    public Map<UUID, Role> getRoles() { return roles; }
    public RoundStats getRoundStats() { return roundStats; }
    public BossBarManager getBossBarManager() { return bossBarManager; }
    public int getMinPlayers() { return minPlayers; }
    public void setMinPlayers(int min) { this.minPlayers = min; }
    public void setCountdownTime(int sec) { this.countdownTime = sec; this.countdownManager.setCountdownTime(sec); }
    public boolean isPlayerInGame(Player player) {
        UUID uuid = player.getUniqueId();
        return players.contains(uuid) || spectators.contains(uuid);
    }
    public boolean isActivePlayer(Player player) { return players.contains(player.getUniqueId()); }
    public String getGameMode() { return gameMode != null ? gameMode : "classic"; }
    public void setGameMode(String mode) {
        this.gameMode = (mode == null || mode.isBlank()) ? "classic" : mode.toLowerCase();
        configManager.setGameMode(this.gameMode);
        plugin.debug("GameMode gesetzt auf: " + this.gameMode);
    }
    public boolean didDetectiveKillInnocent(UUID detective) {
        return roundStats != null && roundStats.getDetectiveInnocentKills(detective) > 0;
    }
}
