package ch.ksrminecraft.murdermystery.managers.support;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.listeners.SignListener;
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
    private int countdownTime = 5; // Default
    private int timeLeft = 0;      // aktuelle Restzeit

    public CountdownManager(GameManager gameManager, MurderMystery plugin) {
        this.gameManager = gameManager;
        this.plugin = plugin;
        this.bossBarManager = gameManager.getBossBarManager();
    }

    /**
     * Startet den Countdown mit der aktuell gesetzten Zeit.
     */
    public void startCountdown() {
        if (countdownRunning) {
            plugin.debug("[CountdownManager] Countdown wurde bereits gestartet → Abbruch.");
            return;
        }

        countdownRunning = true;
        timeLeft = countdownTime; // Restzeit setzen

        plugin.debug("[CountdownManager] Countdown gestartet mit " + countdownTime + " Sekunden.");
        bossBarManager.startLobbyCountdown(countdownTime);

        Broadcaster.broadcastMessage(gameManager.getPlayers(),
                ChatColor.GREEN + "Genügend Spieler in der Wartelobby, das Spiel beginnt in Kürze...");

        countdownTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> {
                    // Abbruch, wenn Spielerzahl unter Minimum fällt
                    if (gameManager.getPlayers().size() < gameManager.getMinPlayers()) {
                        abortIfNotEnoughPlayers(); // 👈 neue Methode verwenden
                        return;
                    }

                    // Countdown fertig → Spiel starten
                    if (timeLeft <= 0) {
                        plugin.debug("[CountdownManager] Countdown abgelaufen → Spiel startet.");
                        stopCountdown();
                        bossBarManager.cancelLobbyBar();
                        gameManager.startGame();
                        return;
                    }

                    // BossBar updaten
                    bossBarManager.updateLobbyCountdown(timeLeft, countdownTime);

                    // Join-Signs updaten
                    SignListener.updateJoinSigns(plugin, plugin.getGameManagerRegistry());

                    // Weniger Spam: Nur alle 5 Sekunden oder die letzten 5 Sekunden loggen
                    if (timeLeft <= 5 || timeLeft % 5 == 0) {
                        plugin.debug("[CountdownManager] Countdown läuft → noch " + timeLeft + " Sekunden.");
                    }

                    timeLeft--; // Restzeit runterzählen
                },
                20L, 20L
        );
    }

    /**
     * Startet den Countdown mit einer übergebenen Zeit (z. B. aus der Config).
     */
    public void startCountdown(int seconds) {
        this.countdownTime = seconds;
        this.timeLeft = seconds;
        plugin.debug("[CountdownManager] Countdown überladen gestartet mit " + seconds + " Sekunden.");
        startCountdown();
    }

    /**
     * Bricht den Countdown sofort ab, wenn Spielerzahl zu gering ist.
     */
    public void abortIfNotEnoughPlayers() {
        if (countdownRunning && gameManager.getPlayers().size() < gameManager.getMinPlayers()) {
            plugin.debug("[CountdownManager] Sofortabbruch: Spielerzahl (" +
                    gameManager.getPlayers().size() + ") < minPlayers (" +
                    gameManager.getMinPlayers() + ")");
            stopCountdown();
            bossBarManager.cancelLobbyBar();

            Broadcaster.broadcastMessage(gameManager.getPlayers(),
                    ChatColor.RED + "Zu wenige Spieler, Countdown wurde abgebrochen!");
        }
    }

    public void stopCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            plugin.debug("[CountdownManager] Countdown gestoppt.");
        }
        countdownRunning = false;
        timeLeft = 0;

        // Bossbar auch zurücksetzen
        bossBarManager.cancelLobbyBar();

        // Schilder sofort updaten
        SignListener.updateJoinSigns(plugin, plugin.getGameManagerRegistry());
    }

    // --- Getter ---
    public boolean isCountdownRunning() { return countdownRunning; }
    public void setCountdownTime(int countdownTime) {
        plugin.debug("[CountdownManager] setCountdownTime(" + countdownTime + ")");
        this.countdownTime = countdownTime;
    }
    public int getCountdownTime() { return countdownTime; }

    /** Liefert die verbleibende Zeit in Sekunden (für Schilder). */
    public int getRemainingSeconds() {
        return timeLeft;
    }
}
