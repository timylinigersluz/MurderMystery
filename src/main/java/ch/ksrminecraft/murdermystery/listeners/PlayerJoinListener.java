package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.game.GameManagerRegistry;
import ch.ksrminecraft.murdermystery.managers.support.BossBarManager;
import ch.ksrminecraft.murdermystery.managers.support.MapManager;
import ch.ksrminecraft.murdermystery.model.ArenaGame;
import ch.ksrminecraft.murdermystery.model.QuitTracker;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final MurderMystery plugin;
    private final GameManagerRegistry registry;
    private final MapManager mapManager;

    public PlayerJoinListener(GameManagerRegistry registry, MapManager mapManager) {
        this.plugin = MurderMystery.getInstance();
        this.registry = registry;
        this.mapManager = mapManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.debug("Spieler " + p.getName() + " hat den Server betreten.");

        // Falls Spieler während eines Spiels gequitted ist → zurück in MainLobby setzen
        if (QuitTracker.hasQuit(p)) {
            QuitTracker.clear(p);
            mapManager.teleportToMainLobby(p);
            return;
        }

        // Standard: Spieler immer in MainLobby
        mapManager.teleportToMainLobby(p);

        // BossBar: Wenn irgendwo ein Spiel läuft → Spieler soll Game-BossBar sehen
        boolean anyGameRunning = registry.getAllManagers().values().stream()
                .anyMatch(ArenaGame::isGameStarted);

        if (anyGameRunning) {
            p.sendMessage(ChatColor.YELLOW + "Es läuft gerade eine MurderMystery-Runde.");
            p.sendMessage(ChatColor.GRAY + "Bitte warte in der Lobby, bis die Runde vorbei ist.");
            // Irgendeinen Manager nehmen für BossBar (globale Anzeige)
            registry.getAllManagers().values().iterator().next()
                    .getBossBarManager().addPlayer(p, BossBarManager.Mode.GAME);
        } else {
            // Kein Spiel läuft → Lobby-BossBar anzeigen
            registry.getAllManagers().values().forEach(mgr ->
                    mgr.getBossBarManager().addPlayer(p, BossBarManager.Mode.LOBBY));
        }
    }
}
