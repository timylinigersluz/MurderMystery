package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.utils.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class GameListener implements Listener {

    private final MurderMystery plugin;
    private final GameManager gameManager;
    private final FileConfiguration config;

    public GameListener(GameManager gameManager) {
        this.plugin = MurderMystery.getInstance();
        this.gameManager = gameManager;
        this.config = plugin.getConfig();
    }

    /**
     * Spieler im Spiel sterben → werden eliminiert.
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();

        if (!gameManager.isGameStarted()) {
            plugin.debug("Spieler " + dead.getName() + " ist gestorben, aber es läuft kein Spiel.");
            return;
        }

        if (gameManager.isPlayerInGame(dead)) {
            plugin.debug("Spieler " + dead.getName() + " ist im Spiel gestorben und wird eliminiert.");
            gameManager.eliminate(dead);
        } else {
            plugin.debug("Spieler " + dead.getName() + " ist gestorben, war aber nicht Teil des Spiels.");
        }
    }

    /**
     * Spieler klickt auf ein Schild → ggf. Spiel beitreten.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;
        if (!(e.getClickedBlock().getState() instanceof Sign sign)) return;

        Player p = e.getPlayer();

        // Sicherheitscheck: Spieler ist schon im Spiel → nicht erneut joinen
        if (gameManager.isPlayerInGame(p)) {
            plugin.debug("Spieler " + p.getName() + " klickt auf Lobby-Schild, ist aber bereits im Spiel.");
            p.sendMessage(ChatColor.GRAY + "Du bist bereits in einer Runde.");
            e.setCancelled(true);
            return;
        }

        String line1 = sign.getLine(1);
        String line2 = sign.getLine(2);

        // Lobby-Schild: 2. Zeile [Lobby], 3. Zeile MurderMystery
        if ("[Lobby]".equalsIgnoreCase(line1) && "MurderMystery".equalsIgnoreCase(line2)) {
            e.setCancelled(true);
            plugin.debug("Spieler " + p.getName() + " hat ein Lobby-Schild angeklickt.");
            gameManager.handleJoin(p);
        } else {
            plugin.debug("Spieler " + p.getName() + " hat ein Schild angeklickt, aber kein gültiges Lobby-Schild.");
            p.sendMessage(ChatColor.RED + "Dies ist kein gültiges MurderMystery-Lobby-Schild.");
        }
    }
}
