package ch.ksrminecraft.murdermystery.utils;

import ch.ksrminecraft.murdermystery.MurderMystery;
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

    /**
     * Startet den Countdown mit BossBar.
     */
    public void startCountdown() {
        if (countdownRunning) {
            plugin.debug("Countdown wurde bereits gestartet.");
            return;
        }

        countdownRunning = true;
        final int[] timeLeft = {countdownTime};

        plugin.debug("Countdown gestartet mit " + countdownTime + " Sekunden.");
        bossBarManager.startLobbyCountdown(countdownTime);

        countdownTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> {
                    // Abbruch, wenn nicht mehr genug Spieler
                    if (gameManager.getPlayers().size() < gameManager.getMinPlayers()) {
                        plugin.debug("Countdown abgebrochen – Spielerzahl unter Minimum.");
                        stopCountdown();
                        bossBarManager.cancelLobbyBar();
                        return;
                    }

                    // Countdown fertig → Spielstart
                    if (timeLeft[0] <= 0) {
                        stopCountdown();
                        bossBarManager.cancelLobbyBar();
                        plugin.debug("Countdown abgelaufen. Spiel startet jetzt.");
                        gameManager.startGame();
                        return;
                    }

                    // Meldung + BossBar-Update
                    gameManager.broadcastToPlayers(ChatColor.AQUA + "Spiel startet in " + timeLeft[0] + " Sekunden...");
                    bossBarManager.updateLobbyCountdown(timeLeft[0], countdownTime);

                    timeLeft[0]--;
                },
                0L, 20L
        );
    }

    /**
     * Stoppt den Countdown (falls nötig).
     */
    public void stopCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            plugin.debug("Countdown gestoppt.");
        }
        countdownRunning = false;
    }

    public boolean isCountdownRunning() {
        return countdownRunning;
    }

    public void setCountdownTime(int countdownTime) {
        this.countdownTime = countdownTime;
    }

    public int getCountdownTime() {
        return countdownTime;
    }
}
