package ch.ksrminecraft.murdermystery.managers.support;

import ch.ksrminecraft.murdermystery.MurderMystery;
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

            gameManager.getBossBarManager().updateGameTimer(timeLeft, maxGameSeconds);
            timeLeft--;

        }, 20L, 20L);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }
}
