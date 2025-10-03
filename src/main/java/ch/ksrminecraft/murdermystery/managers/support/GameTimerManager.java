package ch.ksrminecraft.murdermystery.managers.support;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.listeners.SignListener;
import ch.ksrminecraft.murdermystery.managers.game.GameManager;
import org.bukkit.Bukkit;

public class GameTimerManager {

    private final MurderMystery plugin;
    private final GameManager gameManager;
    private int taskId = -1;
    private int timeLeft;
    private int maxGameSeconds;

    public GameTimerManager(GameManager gameManager, MurderMystery plugin) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    /**
     * Startet den Spiel-Timer (zählt von maxSeconds herunter).
     */
    public void start(int maxSeconds) {
        this.maxGameSeconds = maxSeconds;
        this.timeLeft = maxSeconds;

        stop(); // Safety → falls schon ein Timer läuft

        plugin.debug("[GameTimerManager] Neuer Spiel-Timer gestartet mit " + maxSeconds + " Sekunden.");

        // Bossbar für Spiel starten
        gameManager.getBossBarManager().startGameTimer(maxSeconds);

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!gameManager.isGameStarted()) {
                plugin.debug("[GameTimerManager] Spiel nicht aktiv → Timer gestoppt.");
                stop();
                return;
            }

            if (timeLeft <= 0) {
                stop();
                plugin.debug("[GameTimerManager] Zeitlimit erreicht (0s) → Runde endet automatisch.");
                gameManager.handleTimeout();
                return;
            }

            // BossBar aktualisieren
            gameManager.getBossBarManager().updateGameTimer(timeLeft, maxGameSeconds);

            // Join-Signs aktualisieren
            SignListener.updateJoinSigns(plugin, plugin.getGameManagerRegistry());

            // Weniger Spam: Nur alle 30 Sekunden und in den letzten 10 Sekunden loggen
            if (timeLeft % 60 == 0 || timeLeft <= 5) {
                plugin.debug("[GameTimerManager] Restzeit: " + timeLeft + "s von " + maxGameSeconds + "s.");
            }

            // Restzeit herunterzählen
            timeLeft--;
        }, 20L, 20L);
    }

    /**
     * Stoppt den Spiel-Timer und versteckt die BossBar.
     */
    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            plugin.debug("[GameTimerManager] Timer-Task gestoppt.");
            taskId = -1;
        }
        gameManager.getBossBarManager().cancelGameBar(); // BossBar verstecken
    }

    public int getRemainingSeconds() {
        return timeLeft;
    }

    public int getMaxGameSeconds() {
        return maxGameSeconds;
    }
}
