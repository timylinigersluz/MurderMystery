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

    public void start(int maxSeconds) {
        this.maxGameSeconds = maxSeconds;
        this.timeLeft = maxSeconds;

        stop(); // Safety

        // Bossbar für Spiel starten
        gameManager.getBossBarManager().startGameTimer(maxSeconds);

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!gameManager.isGameStarted()) {
                stop();
                return;
            }

            if (timeLeft <= 0) {
                stop();
                plugin.debug("Zeitlimit erreicht → Runde endet automatisch.");
                gameManager.handleTimeout();
                return;
            }

            // BossBar aktualisieren
            gameManager.getBossBarManager().updateGameTimer(timeLeft, maxGameSeconds);

            // Join-Signs aktualisieren
            SignListener.updateJoinSigns(plugin);

            // Restzeit herunterzählen
            timeLeft--;
        }, 20L, 20L);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        gameManager.getBossBarManager().cancelGameBar(); // BossBar verstecken
    }

    /** Gibt die verbleibenden Sekunden zurück */
    public int getRemainingSeconds() {
        return timeLeft;
    }

    /** Gibt die maximale Rundendauer zurück */
    public int getMaxGameSeconds() {
        return maxGameSeconds;
    }
}
