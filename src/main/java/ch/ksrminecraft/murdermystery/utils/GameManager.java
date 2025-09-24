package ch.ksrminecraft.murdermystery.utils;

import ch.ksrminecraft.murdermystery.MurderMystery;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.entity.EntityType;

import java.util.*;

public class GameManager {

    private final MurderMystery plugin;
    private final PointsManager pointsManager;
    private final PlayerManager playerManager;
    private final CountdownManager countdownManager;
    private final WinConditionChecker winConditionChecker;
    private final BossBarManager bossBarManager;

    private final Set<UUID> players = new HashSet<>();     // nur lebende Spieler
    private final Set<UUID> spectators = new HashSet<>();  // ausgeschiedene Spieler
    private final Map<UUID, Role> roles = new HashMap<>();
    private boolean gameStarted = false;

    private int minPlayers = 3;
    private int countdownTime = 5;

    private final int punkteGewinner;
    private final int punkteMitGewinner;
    private final int punkteVerlierer;

    // Game-Timer
    private int gameTimeTask = -1;
    private int maxGameSeconds;
    private int timeLeft;

    // FailSafe-Task
    private int failSafeTask = -1;

    // Runde Stats
    private RoundStats roundStats;

    // Gamemode (classic / bow-fallback)
    private final boolean bowFallbackMode;
    private String gameMode;

    public GameManager(PointsManager pointsManager, MurderMystery plugin) {
        this.pointsManager = pointsManager;
        this.plugin = plugin;

        this.punkteGewinner = plugin.getConfig().getInt("punkte-gewinner");
        this.punkteMitGewinner = plugin.getConfig().getInt("punkte-mitgewinner");
        this.punkteVerlierer = plugin.getConfig().getInt("punkte-verlierer");

        String mode = plugin.getConfig().getString("gamemode", "classic");
        this.bowFallbackMode = mode.equalsIgnoreCase("bow-fallback");

        this.bossBarManager = new BossBarManager(plugin);
        this.countdownManager = new CountdownManager(this, plugin);
        this.winConditionChecker = new WinConditionChecker(this, pointsManager, plugin);
        this.playerManager = new PlayerManager(this, plugin);
    }

    // Delegation
    public void handleJoin(Player player) {
        playerManager.handleJoin(player);
    }

    public void handleLeave(Player player) {
        playerManager.handleLeave(player);
        if (gameStarted && roundStats != null) {
            roundStats.markQuitter(player.getUniqueId());
        }
    }

    public void eliminate(Player player) {
        playerManager.eliminate(player);

        // Kill-Title an alle broadcasten
        String victim = player.getName();
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendTitle(
                        ChatColor.RED + victim + " wurde eliminiert!",
                        ChatColor.GRAY + "Bleib wachsam...",
                        10, 50, 20
                );
            }
        }
    }

    public void startCountdown() {
        countdownManager.setCountdownTime(countdownTime);
        countdownManager.startCountdown();
    }

    public void checkWinConditions() {
        winConditionChecker.checkWinConditions(players, roles,
                punkteGewinner, punkteMitGewinner, punkteVerlierer, roundStats);
    }

    public void startGame() {
        if (gameStarted) return;
        gameStarted = true;
        roundStats = new RoundStats(); // Neue Runde -> neue Stats
        playerManager.startGame(players, roles);

        // Zeitlimit starten
        maxGameSeconds = plugin.getConfig().getInt("max-game-seconds", 600);
        timeLeft = maxGameSeconds;
        startGameTimer();

        // FailSafe starten
        startFailSafeTask();
    }

    private void startGameTimer() {
        bossBarManager.startGameTimer(maxGameSeconds);

        gameTimeTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!gameStarted) {
                stopGameTimer();
                return;
            }

            if (timeLeft <= 0) {
                stopGameTimer();
                plugin.debug("Zeitlimit erreicht → Runde endet automatisch.");
                broadcastToPlayers(ChatColor.RED + "Zeitlimit erreicht! Die Bystander gewinnen.");
                winConditionChecker.forceTimeoutEnd(players, roles,
                        punkteMitGewinner, punkteVerlierer, roundStats);
                endRound();
                return;
            }

            bossBarManager.updateGameTimer(timeLeft, maxGameSeconds);
            timeLeft--;
        }, 20L, 20L);
    }

    private void stopGameTimer() {
        bossBarManager.cancelGameBar();
        if (gameTimeTask != -1) {
            Bukkit.getScheduler().cancelTask(gameTimeTask);
            gameTimeTask = -1;
        }
    }

    private void startFailSafeTask() {
        failSafeTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!gameStarted) return;

            for (UUID uuid : players) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) continue;

                Role role = roles.get(uuid);
                if (role == null) continue;

                switch (role) {
                    case DETECTIVE -> {
                        if (!p.getInventory().contains(ItemManager.createDetectiveBow())) {
                            p.getInventory().addItem(ItemManager.createDetectiveBow());
                            p.sendMessage(ChatColor.YELLOW + "Dein Detective-Bogen wurde wiederhergestellt!");
                            plugin.debug("FailSafe: Detective-Bogen für " + p.getName() + " wiederhergestellt.");
                        }
                    }
                    case MURDERER -> {
                        if (!p.getInventory().contains(ItemManager.createMurdererSword())) {
                            p.getInventory().addItem(ItemManager.createMurdererSword());
                            p.sendMessage(ChatColor.YELLOW + "Dein Murderer-Schwert wurde wiederhergestellt!");
                            plugin.debug("FailSafe: Murderer-Schwert für " + p.getName() + " wiederhergestellt.");
                        }
                    }
                }
            }
        }, 0L, 200L); // alle 10 Sekunden
    }

    private void stopFailSafeTask() {
        if (failSafeTask != -1) {
            Bukkit.getScheduler().cancelTask(failSafeTask);
            failSafeTask = -1;
        }
    }

    public void endRound() {
        if (roundStats != null) {
            pointsManager.distributeRoundPoints(roundStats, roles);

            // 🎆 Feuerwerk für Gewinner (Survivors)
            for (UUID uuid : roundStats.getAllPlayers()) {
                if (roundStats.hasSurvived(uuid)) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> launchCelebrationFireworks(p), 40L); // 2s Delay
                    }
                }
            }
        }
        resetGame();
    }

    private void launchCelebrationFireworks(Player player) {
        for (int i = 0; i < 3; i++) {
            int delay = i * 20; // 1 Sekunde Abstand
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location loc = player.getLocation();
                Firework firework = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);

                FireworkMeta meta = firework.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .withColor(Color.AQUA, Color.GREEN, Color.YELLOW)
                        .withFade(Color.WHITE)
                        .trail(true)
                        .flicker(true)
                        .build());
                meta.setPower(1);
                firework.setFireworkMeta(meta);
            }, delay);
        }
    }

    public void resetGame() {
        gameStarted = false;
        spectators.clear();
        roles.clear();
        playerManager.resetGame(players);
        stopGameTimer();
        stopFailSafeTask();
        roundStats = null;
    }

    public void broadcastToPlayers(String message) {
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        }
    }

    public boolean isPlayerInGame(Player player) {
        UUID uuid = player.getUniqueId();
        return players.contains(uuid) || spectators.contains(uuid);
    }

    public boolean isActivePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        return players.contains(uuid);
    }

    // Getter
    public boolean isGameStarted() {
        return gameStarted;
    }

    public boolean isBowFallbackMode() {
        return bowFallbackMode;
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

    public String getGameMode() {
        return gameMode;
    }

    public void setGameMode(String mode) {
        this.gameMode = mode;
    }
}
