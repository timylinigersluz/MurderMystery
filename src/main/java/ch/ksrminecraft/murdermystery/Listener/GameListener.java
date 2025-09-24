package ch.ksrminecraft.murdermystery.Listener;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.Utils.GameManager;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

public class GameListener implements Listener {
    private final FileConfiguration config = MurderMystery.getInstance().getConfig();
    private final GameManager gameManager;
    public GameListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }


    // Wenn Spieler im Spiel ist, Eliminierung ausführen
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        // Spiel muss aktiv sein
        if (!gameManager.isGameStarted()) return;
        Player dead = e.getEntity();
        // Nur Spieler im Spiel werden eliminiert
        if (gameManager.isPlayerInGame(dead)) {
            gameManager.eliminate(dead);
        }
    }

    // Spiel durch spezielles Schild beitreten
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!(e.getClickedBlock().getState() instanceof Sign)) return;
        Sign sign = (Sign) e.getClickedBlock().getState();
        Player p = e.getPlayer();

        // Lobby-Schild CHECK
        if (sign.getLine(1).equalsIgnoreCase("[Lobby]") &&
                sign.getLine(2).equalsIgnoreCase("MurderMystery")) {
            e.setCancelled(true);
            gameManager.handleJoin(p);
        }
    }




}
