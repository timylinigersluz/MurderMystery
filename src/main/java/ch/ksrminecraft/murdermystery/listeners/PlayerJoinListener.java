package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.utils.BossBarManager;
import ch.ksrminecraft.murdermystery.utils.GameManager;
import ch.ksrminecraft.murdermystery.utils.QuitTracker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final MurderMystery plugin;
    private final GameManager gameManager;
    private final FileConfiguration config;

    public PlayerJoinListener(GameManager gameManager) {
        this.plugin = MurderMystery.getInstance();
        this.gameManager = gameManager;
        this.config = plugin.getConfig();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.debug("Spieler " + p.getName() + " hat den Server betreten.");

        // Falls Spieler während eines Spiels quitted → zurück in MainWorld setzen
        if (QuitTracker.hasQuit(p)) {
            QuitTracker.clear(p);
            sendToMainWorld(p);
            return;
        }

        // Wenn Spiel läuft → Spieler bleibt in Lobby und sieht Game-BossBar
        if (gameManager.isGameStarted()) {
            sendToLobby(p);
            p.sendMessage(ChatColor.YELLOW + "Es läuft gerade eine MurderMystery-Runde.");
            p.sendMessage(ChatColor.GRAY + "Bitte warte in der Lobby, bis die Runde vorbei ist.");
            gameManager.getBossBarManager().addPlayer(p, BossBarManager.Mode.GAME);
            return;
        }

        // Wenn kein Spiel läuft, aber Countdown schon aktiv → Lobby-BossBar anzeigen
        if (gameManager.getBossBarManager() != null) {
            gameManager.getBossBarManager().addPlayer(p, BossBarManager.Mode.LOBBY);
        }
    }

    private void sendToLobby(Player p) {
        String lobbyWorldName = config.getString("worlds.lobby");
        World lobby = Bukkit.getWorld(lobbyWorldName);
        if (lobby != null) {
            Location spawn = lobby.getSpawnLocation();
            p.teleport(spawn);
            plugin.debug("Spieler " + p.getName() + " wurde in die Lobby teleportiert.");
        } else {
            plugin.getLogger().severe("Lobby-Welt '" + lobbyWorldName + "' nicht gefunden!");
        }
    }

    private void sendToMainWorld(Player p) {
        String mainWorldName = config.getString("worlds.main");
        World main = Bukkit.getWorld(mainWorldName);
        if (main != null) {
            p.teleport(main.getSpawnLocation());
            plugin.debug("Spieler " + p.getName() + " wurde in die Hauptwelt teleportiert.");
        } else {
            plugin.getLogger().severe("Hauptwelt '" + mainWorldName + "' nicht gefunden!");
        }
    }
}
