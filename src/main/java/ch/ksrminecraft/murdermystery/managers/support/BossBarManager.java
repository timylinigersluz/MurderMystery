package ch.ksrminecraft.murdermystery.managers.support;

import ch.ksrminecraft.murdermystery.MurderMystery;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BossBarManager {

    public enum Mode { LOBBY, GAME }

    private final MurderMystery plugin;
    private final BossBar lobbyBar;
    private final BossBar gameBar;

    public BossBarManager(MurderMystery plugin) {
        this.plugin = plugin;
        this.lobbyBar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
        this.gameBar = Bukkit.createBossBar("", BarColor.RED, BarStyle.SOLID);
        this.lobbyBar.setVisible(false);
        this.gameBar.setVisible(false);
    }

    // ----------------- Lobby BossBar -----------------
    public void startLobbyCountdown(int seconds) {
        lobbyBar.setTitle("Spiel startet in " + seconds + "s");
        lobbyBar.setProgress(1.0);
        lobbyBar.setVisible(true);

        // Alle Online-Spieler sehen die Lobby-Bar
        for (Player p : Bukkit.getOnlinePlayers()) {
            lobbyBar.addPlayer(p);
        }
    }

    public void updateLobbyCountdown(int secondsLeft, int total) {
        if (!lobbyBar.isVisible()) return;
        lobbyBar.setTitle("Spiel startet in " + secondsLeft + "s");
        lobbyBar.setProgress(Math.max(0.0, (double) secondsLeft / total));
    }

    public void cancelLobbyBar() {
        lobbyBar.setVisible(false);
        lobbyBar.removeAll();
    }

    // ----------------- Game BossBar -----------------
    public void startGameTimer(int seconds) {
        int minutes = seconds / 60;
        int sec = seconds % 60;
        String formatted = String.format("%02d:%02d", minutes, sec);

        gameBar.setTitle("Spielzeit: " + formatted);
        gameBar.setProgress(1.0);
        gameBar.setVisible(true);

        // Nur aktive Spieler + Spectators hinzufügen
        for (UUID uuid : plugin.getGameManager().getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) gameBar.addPlayer(p);
        }
        for (UUID uuid : plugin.getGameManager().getSpectators()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) gameBar.addPlayer(p);
        }
    }

    public void updateGameTimer(int timeLeft, int total) {
        if (!gameBar.isVisible()) return;
        int minutes = timeLeft / 60;
        int seconds = timeLeft % 60;
        gameBar.setTitle("Spielzeit: " + minutes + "m " + seconds + "s");
        gameBar.setProgress((double) timeLeft / total);
    }

    public void cancelGameBar() {
        gameBar.setVisible(false);
        gameBar.removeAll();
    }

    // ----------------- Utility -----------------
    public void addPlayer(Player player, Mode mode) {
        if (mode == Mode.LOBBY && lobbyBar.isVisible()) lobbyBar.addPlayer(player);
        if (mode == Mode.GAME && gameBar.isVisible()) gameBar.addPlayer(player);
    }

    public void removePlayer(Player player) {
        lobbyBar.removePlayer(player);
        gameBar.removePlayer(player);
    }
}
