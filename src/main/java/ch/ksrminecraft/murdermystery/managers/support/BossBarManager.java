package ch.ksrminecraft.murdermystery.managers.support;

import ch.ksrminecraft.murdermystery.managers.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class BossBarManager {

    public enum Mode { LOBBY, GAME }

    private final GameManager gameManager;
    private final BossBar lobbyBar;
    private final BossBar gameBar;

    public BossBarManager(GameManager gameManager) {
        this.gameManager = gameManager;
        this.lobbyBar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
        this.gameBar = Bukkit.createBossBar("", BarColor.RED, BarStyle.SOLID);
        this.lobbyBar.setVisible(false);
        this.gameBar.setVisible(false);

        gameManager.getPlugin().debug("[BossBarManager] Initialisiert für GameManager.");
    }

    // ----------------- Lobby BossBar -----------------
    public void startLobbyCountdown(int seconds) {
        lobbyBar.setTitle("Spiel startet in " + seconds + "s");
        lobbyBar.setProgress(1.0);
        lobbyBar.setVisible(true);
        addPlayers(gameManager.getPlayers(), Mode.LOBBY);

        gameManager.getPlugin().debug("[BossBarManager] LobbyCountdown gestartet (" + seconds + "s).");
    }

    public void updateLobbyCountdown(int secondsLeft, int total) {
        if (!lobbyBar.isVisible()) {
            gameManager.getPlugin().debug("[BossBarManager] updateLobbyCountdown abgebrochen → LobbyBar unsichtbar.");
            return;
        }
        lobbyBar.setTitle("Spiel startet in " + secondsLeft + "s");
        lobbyBar.setProgress(Math.max(0.0, (double) secondsLeft / total));
    }

    public void cancelLobbyBar() {
        lobbyBar.setVisible(false);
        lobbyBar.removeAll();
        gameManager.getPlugin().debug("[BossBarManager] LobbyBar beendet.");
    }

    // ----------------- Game BossBar -----------------
    public void startGameTimer(int seconds) {
        updateGameTimer(seconds, seconds);
        gameBar.setVisible(true);
        addPlayers(gameManager.getPlayers(), Mode.GAME);
        addPlayers(gameManager.getSpectators(), Mode.GAME);

        gameManager.getPlugin().debug("[BossBarManager] GameTimer gestartet (" + seconds + "s).");
    }

    public void updateGameTimer(int timeLeft, int total) {
        if (!gameBar.isVisible()) return;
        int minutes = timeLeft / 60;
        int seconds = timeLeft % 60;
        gameBar.setTitle("Spielzeit: " + minutes + "m " + seconds + "s");
        gameBar.setProgress((double) timeLeft / total);

        // Debug reduziert: Nur jede volle Minute oder letzte 10 Sekunden loggen
        if (timeLeft % 60 == 0 || timeLeft <= 5) {
            gameManager.getPlugin().debug("[BossBarManager] GameTimer Update → " + timeLeft + "s/" + total + "s.");
        }
    }

    public void cancelGameBar() {
        gameBar.setVisible(false);
        gameBar.removeAll();
        gameManager.getPlugin().debug("[BossBarManager] GameBar beendet.");
    }

    // ----------------- Utility -----------------
    public void addPlayer(Player player, Mode mode) {
        if (mode == Mode.LOBBY && lobbyBar.isVisible()) {
            lobbyBar.addPlayer(player);
            gameManager.getPlugin().debug("[BossBarManager] Spieler " + player.getName() + " → LobbyBar hinzugefügt.");
        }
        if (mode == Mode.GAME && gameBar.isVisible()) {
            gameBar.addPlayer(player);
            gameManager.getPlugin().debug("[BossBarManager] Spieler " + player.getName() + " → GameBar hinzugefügt.");
        }
    }

    public void removePlayer(Player player) {
        lobbyBar.removePlayer(player);
        gameBar.removePlayer(player);
        gameManager.getPlugin().debug("[BossBarManager] Spieler " + player.getName() + " aus allen Bars entfernt.");
    }

    private void addPlayers(Set<UUID> uuids, Mode mode) {
        for (UUID uuid : uuids) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                addPlayer(p, mode);
            } else {
                gameManager.getPlugin().debug("[BossBarManager] UUID " + uuid + " konnte nicht hinzugefügt werden (offline).");
            }
        }
    }
}
