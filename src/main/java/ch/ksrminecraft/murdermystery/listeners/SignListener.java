package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.Set;

public class SignListener implements Listener {

    private final MurderMystery plugin;
    private static final Set<Location> joinSigns = new HashSet<>();

    public SignListener(MurderMystery plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSignCreate(SignChangeEvent event) {
        Player player = event.getPlayer();
        String firstLine = event.getLine(0);

        if (firstLine != null && firstLine.equalsIgnoreCase("[MurderMystery]")) {
            String size = event.getLine(1) != null ? event.getLine(1).toLowerCase() : "";
            if (!(size.equals("small") || size.equals("mid") || size.equals("large"))) {
                player.sendMessage(ChatColor.RED + "Bitte gib in Zeile 2 eine Größe an: small, mid oder large!");
                return;
            }

            int minPlayers = plugin.getConfigManager().getMinPlayers();

            event.setLine(0, ChatColor.DARK_RED + "[MurderMystery]");
            event.setLine(1, ChatColor.GREEN + size);
            event.setLine(2, ChatColor.YELLOW + "0 von mind. " + minPlayers);
            event.setLine(3, ChatColor.GRAY + "Wartelobby");

            joinSigns.add(event.getBlock().getLocation());
            player.sendMessage(ChatColor.AQUA + "MurderMystery-Join-Schild (" + size + ") erstellt!");
        }
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (!(event.getClickedBlock().getState() instanceof Sign sign)) return;

        String firstLine = ChatColor.stripColor(sign.getLine(0)).trim();
        if (!firstLine.equalsIgnoreCase("[MurderMystery]")) return;

        Player player = event.getPlayer();

        if (player.hasPermission("murdermystery.admin") && player.getGameMode() == GameMode.CREATIVE) return;
        if (plugin.getGameManager().isGameStarted()) {
            player.sendMessage(ChatColor.RED + "Das Spiel läuft gerade. Bitte warten!");
            return;
        }

        String arenaSize = ChatColor.stripColor(sign.getLine(1)).trim().toLowerCase();
        if (arenaSize.equals("small") || arenaSize.equals("mid") || arenaSize.equals("large")) {
            Bukkit.dispatchCommand(player, "mm join " + arenaSize);
        } else {
            Bukkit.dispatchCommand(player, "mm join");
        }
    }

    @EventHandler
    public void onSignBreak(BlockBreakEvent event) {
        if (!(event.getBlock().getState() instanceof Sign)) return;

        Player player = event.getPlayer();
        String firstLine = ChatColor.stripColor(((Sign) event.getBlock().getState()).getLine(0)).trim();

        if (firstLine.equalsIgnoreCase("[MurderMystery]")) {
            if (player.hasPermission("murdermystery.admin") && player.getGameMode() == GameMode.CREATIVE) {
                joinSigns.remove(event.getBlock().getLocation());
                player.sendMessage(ChatColor.AQUA + "MurderMystery-Schild entfernt.");
                return;
            }
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Du darfst dieses Schild nicht abbauen!");
        }
    }

    public static void updateJoinSigns(MurderMystery plugin) {
        int current = plugin.getGameManager().getPlayers().size();
        int min = plugin.getConfigManager().getMinPlayers();
        boolean gameRunning = plugin.getGameManager().isGameStarted();

        for (Location loc : joinSigns) {
            if (loc.getBlock().getState() instanceof Sign sign) {
                if (gameRunning) {
                    sign.setLine(1, ChatColor.RED + "Spiel läuft");
                    sign.setLine(2, ChatColor.YELLOW + "Bitte warten");
                    sign.setLine(3, "");
                } else {
                    String size = ChatColor.stripColor(sign.getLine(1)).toLowerCase();
                    sign.setLine(1, ChatColor.GREEN + size);
                    sign.setLine(2, ChatColor.YELLOW + "" + current + " von mind. " + min);
                    sign.setLine(3, (current >= min ? ChatColor.GREEN + "Startbereit" : ChatColor.GRAY + "Wartelobby"));
                }
                sign.update();
            }
        }
    }
}
