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

    /**
     * Wird beim Reload aufgerufen und aktualisiert interne Werte
     */
    public void reloadFromConfig(ConfigManager configManager) {
        this.minPlayers = configManager.getMinPlayers();
        this.countdownTime = configManager.getCountdownSeconds();
        this.gameMode = configManager.getGameMode();

        plugin.debug("GameManager reload → MinPlayers=" + minPlayers +
                ", Countdown=" + countdownTime +
                ", GameMode=" + gameMode);
    }

    // ----------------- Spieler-Handling -----------------
    public void handleJoin(Player player) {
        playerManager.handleJoin(player);
    }

    public void handleLeave(Player player) {
        playerManager.handleLeave(player);
        if (gameStarted && roundStats != null) {
            roundStats.markQuitter(player.getUniqueId());
            // -3 Punkte Strafe für Leaver sofort
            pointsManager.applyPenalty(player.getUniqueId(),
                    Math.abs(configManager.getPointsQuit()),
                    "Spiel verlassen");
        }
    }

    public void eliminate(Player player) {
        playerManager.eliminate(player);
        Broadcaster.broadcastMessage(players, ChatColor.RED + "Ein Spieler wurde eliminiert!");
    }

    // ----------------- Countdown & Start -----------------
    public void startCountdown() {
        countdownManager.setCountdownTime(countdownTime);
        countdownManager.startCountdown();
    }

    public void startGame() {
        if (gameStarted) return;
        gameStarted = true;
        roundStats = new RoundStats();

        playerManager.startGame(players, roles);

        // Rollenanzeige
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && roles.containsKey(uuid)) {
                showRoleTitle(p, roles.get(uuid));
            }
        }

        gameTimerManager.start(configManager.getMaxGameSeconds());
        failSafeManager.start();
    }

    public void checkWinConditions() {
        winConditionManager.checkWinConditions(players, roles, roundStats);
    }

    // ----------------- Runden-Ende -----------------
    public void endRound(RoundResultManager.EndCondition condition) {
        if (roundStats != null) {
            roundResultManager.handleRoundEnd(
                    condition,
                    roles,
                    roundStats.getKillsMap(),
                    roundStats.getSurvivors(),
                    roundStats.getQuitters()
            );

            for (UUID uuid : roundStats.getAllPlayers()) {
                if (roundStats.hasSurvived(uuid)) {
                    Player p = Bukkit.getPlayer(uuid);
                    celebrationManager.launchFireworks(p);
                }
            }
        }
        resetGame();
    }

    public void handleTimeout() {
        Broadcaster.broadcastMessage(players, ChatColor.RED + "⏰ Zeitlimit erreicht!");
        if (roundStats != null) {
            roundResultManager.handleRoundEnd(
                    RoundResultManager.EndCondition.TIME_UP,
                    roles,
                    roundStats.getKillsMap(),
                    roundStats.getSurvivors(),
                    roundStats.getQuitters()
            );
        }
        resetGame();
    }

    public void resetGame() {
        gameStarted = false;

        Set<UUID> allPlayers = new HashSet<>();
        allPlayers.addAll(players);
        allPlayers.addAll(spectators);

        playerManager.resetGame(allPlayers);

        // Item-Cleanup
        for (World world : Bukkit.getWorlds()) {
            world.getEntities().stream()
                    .filter(entity -> entity instanceof org.bukkit.entity.Item)
                    .map(entity -> (org.bukkit.entity.Item) entity)
                    .filter(item -> ItemManager.isDetectiveBow(item.getItemStack()) ||
                            ItemManager.isMurdererSword(item.getItemStack()))
                    .forEach(item -> {
                        item.remove();
                        plugin.debug("Cleanup: Entfernt Item " +
                                item.getItemStack().getType() +
                                " in Welt " + world.getName());
                    });
        }

        players.clear();
        spectators.clear();
        roles.clear();
        RoleManager.clearRoles(); // ✅ Rollen resetten

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

    // ----------------- Getter & Setter -----------------
    public boolean isGameStarted() {
        return gameStarted;
    }

    public Set<UUID> getPlayers() {
        return players;
    }

    public Set<UUID> getSpectators() {
        return spectators;
    }

    public Map<UUID, Role> getRoles() {
        return roles;
    }

    public RoundStats getRoundStats() {
        return roundStats;
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int min) {
        this.minPlayers = min;
    }

    public void setCountdownTime(int sec) {
        this.countdownTime = sec;
        this.countdownManager.setCountdownTime(sec);
    }

    public boolean isPlayerInGame(Player player) {
        UUID uuid = player.getUniqueId();
        return players.contains(uuid) || spectators.contains(uuid);
    }

    public boolean isActivePlayer(Player player) {
        return players.contains(player.getUniqueId());
    }

    public String getGameMode() {
        return gameMode != null ? gameMode : "classic"; // Fallback
    }

    public void setGameMode(String mode) {
        this.gameMode = (mode == null || mode.isBlank()) ? "classic" : mode.toLowerCase();
        configManager.setGamemode(this.gameMode);
        plugin.debug("GameMode gesetzt auf: " + this.gameMode);
    }

    // --- Proxy-Getter für Detective-Fehlabschüsse ---
    public boolean didDetectiveKillInnocent(UUID detective) {
        return roundStats != null && roundStats.didDetectiveKillInnocent(detective);
    }
}
