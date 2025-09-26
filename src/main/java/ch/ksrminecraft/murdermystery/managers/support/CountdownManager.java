package ch.ksrminecraft.murdermystery.managers.support;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.effects.Broadcaster;
import ch.ksrminecraft.murdermystery.managers.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitTask;

public class CountdownManager {

    private final GameManager gameManager;
    private final MurderMystery plugin;
    private final BossBarManager bossBarManager;

    private boolean countdownRunning = false;
    private BukkitTask countdownTask;
    private int countdownTime = 5;

    public CountdownManager(GameManager gameManager, MurderMystery plugin) {
        this.gameManager = gameManager;
        this.plugin = plugin;
        this.bossBarManager = gameManager.getBossBarManager();
    }

    public void startCountdown() {
        if (countdownRunning) {
            plugin.debug("Countdown wurde bereits gestartet.");
            return;
        }

        countdownRunning = true;
        final int[] timeLeft = {countdownTime};

        plugin.debug("Countdown gestartet mit " + countdownTime + " Sekunden.");
        bossBarManager.startLobbyCountdown(countdownTime);

        // 👉 Einmalige Chat-Nachricht
        Broadcaster.broadcastMessage(gameManager.getPlayers(),
                ChatColor.GREEN + "Genügend Spieler in der Wartelobby, das Spiel beginnt in Kürze..."
        );

        countdownTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> {
                    if (gameManager.getPlayers().size() < gameManager.getMinPlayers()) {
                        plugin.debug("Countdown abgebrochen – Spielerzahl unter Minimum.");
                        stopCountdown();
                        bossBarManager.cancelLobbyBar();
                        return;
                    }

                    if (timeLeft[0] <= 0) {
                        stopCountdown();
                        bossBarManager.cancelLobbyBar();
                        plugin.debug("Countdown abgelaufen. Spiel startet jetzt.");
                        gameManager.startGame();
                        return;
                    }

                    bossBarManager.updateLobbyCountdown(timeLeft[0], countdownTime);
                    timeLeft[0]--;
                },
                20L, 20L
        );
    }

    public void stopCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            plugin.debug("Countdown gestoppt.");
        }
        countdownRunning = false;
    }

    public boolean isCountdownRunning() { return countdownRunning; }
    public void setCountdownTime(int countdownTime) { this.countdownTime = countdownTime; }
    public int getCountdownTime() { return countdownTime; }
}
