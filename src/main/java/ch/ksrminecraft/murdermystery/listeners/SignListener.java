package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;

public class SignListener implements Listener {

    private final MurderMystery plugin;

    public SignListener(MurderMystery plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSignCreate(SignChangeEvent event) {
        Player player = event.getPlayer();
        String firstLine = event.getLine(0);

        if (firstLine != null && firstLine.equalsIgnoreCase("[MurderMystery]")) {
            // Farbig setzen
            event.setLine(0, ChatColor.DARK_RED + "[MurderMystery]");
            event.setLine(1, ChatColor.GREEN + "Klicke hier,");
            event.setLine(2, ChatColor.YELLOW + "um ein Spiel");
            event.setLine(3, ChatColor.GREEN + "zu starten!");

            player.sendMessage(ChatColor.AQUA + "MurderMystery-Lobby-Schild erstellt!");
        }
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (!(event.getClickedBlock().getState() instanceof Sign sign)) return;

        // StripColor → Vergleich ohne Farben
        String firstLine = ChatColor.stripColor(sign.getLine(0)).trim();

        if (firstLine.equalsIgnoreCase("[MurderMystery]")) {
            Player player = event.getPlayer();
            Bukkit.dispatchCommand(player, "mm join");
            plugin.debug("Spieler " + player.getName() + " klickt auf Lobby-Schild → /mm join ausgeführt.");
        }
    }
}
