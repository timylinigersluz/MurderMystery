package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.game.GameManagerRegistry;
import ch.ksrminecraft.murdermystery.model.Arena;
import ch.ksrminecraft.murdermystery.model.ArenaGame;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SignListener implements Listener {

    private final MurderMystery plugin;
    private final GameManagerRegistry registry;

    public SignListener(MurderMystery plugin, GameManagerRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    // Absicherung: Nur Admins im Creative dürfen Schilder erstellen/umbenennen
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        String firstLine = event.getLine(0);

        boolean isMMSign = firstLine != null && ChatColor.stripColor(firstLine).equalsIgnoreCase("[MurderMystery]");

        if (isMMSign) {
            if (!(player.hasPermission("murdermystery.admin") && player.getGameMode() == GameMode.CREATIVE)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Du darfst dieses Schild nicht umbenennen!");
                return;
            }

            // Arena-Name aus Zeile 2
            String arenaName = event.getLine(1) != null ? event.getLine(1).toLowerCase() : "";
            Arena arena = plugin.getArenaManager().getArena(arenaName);

            if (arena == null) {
                player.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' existiert nicht!");
                return;
            }

            int minPlayers = plugin.getConfigManager().getMinPlayers();

            event.setLine(0, ChatColor.DARK_RED + "[MurderMystery]");
            event.setLine(1, ChatColor.GREEN + arenaName);
            event.setLine(2, ChatColor.YELLOW + "0 von mind. " + minPlayers);
            event.setLine(3, ChatColor.GRAY + "Wartelobby");

            player.sendMessage(ChatColor.AQUA + "MurderMystery-Join-Schild für Arena '" + arenaName + "' erstellt!");
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
        String arenaName = ChatColor.stripColor(sign.getLine(1)).trim().toLowerCase();

        ArenaGame arenaGame = registry.getGameManager(arenaName);
        if (arenaName.isEmpty() || arenaGame == null) {
            player.sendMessage(ChatColor.RED + "Dieses Schild verweist auf keine gültige Arena!");
            return;
        }

        // Wenn Spiel läuft → blockieren
        if (arenaGame.isGameStarted()) {
            player.sendMessage(ChatColor.RED + "Dieses Spiel läuft bereits!");
            player.sendTitle(ChatColor.RED + "Gesperrt", ChatColor.YELLOW + "Das Spiel ist schon gestartet.", 10, 40, 10);
            return;
        }

        // Spieler zur Arena joinen lassen
        Bukkit.dispatchCommand(player, "mm join " + arenaName);
    }

    @EventHandler
    public void onSignBreak(BlockBreakEvent event) {
        if (!(event.getBlock().getState() instanceof Sign)) return;

        Player player = event.getPlayer();
        String firstLine = ChatColor.stripColor(((Sign) event.getBlock().getState()).getLine(0)).trim();

        if (firstLine.equalsIgnoreCase("[MurderMystery]")) {
            if (player.hasPermission("murdermystery.admin") &&
                    player.getGameMode() == GameMode.CREATIVE) {
                player.sendMessage(ChatColor.AQUA + "MurderMystery-Schild entfernt.");
                return;
            }
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Du darfst dieses Schild nicht abbauen!");
        }
    }

    public static void updateJoinSigns(MurderMystery plugin, GameManagerRegistry registry) {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState state : chunk.getTileEntities()) {
                    if (!(state instanceof Sign sign)) continue;

                    String firstLine = ChatColor.stripColor(sign.getLine(0)).trim();
                    if (!firstLine.equalsIgnoreCase("[MurderMystery]")) continue;

                    String arenaName = ChatColor.stripColor(sign.getLine(1)).toLowerCase();
                    Arena arena = plugin.getArenaManager().getArena(arenaName);
                    if (arena == null) continue;

                    ArenaGame arenaGame = registry.getGameManager(arenaName);
                    if (arenaGame == null) continue;

                    int current = arenaGame.getPlayers().size();
                    int min = plugin.getConfigManager().getMinPlayers();

                    // Zeile 0 bleibt immer gleich
                    sign.setLine(0, ChatColor.DARK_RED + "[MurderMystery]");
                    sign.setLine(1, ChatColor.GREEN + arenaName);

                    if (arenaGame.isGameStarted()) {
                        // Spiel läuft → GameTimer anzeigen
                        int secondsLeft = arenaGame.getGameTimerManager().getRemainingSeconds();
                        int minutes = secondsLeft / 60;
                        int seconds = secondsLeft % 60;
                        sign.setLine(2, ChatColor.RED.toString() + ChatColor.BOLD +
                                String.format("Läuft: %02d:%02d", minutes, seconds) + ChatColor.RED);

                        sign.setLine(3, ChatColor.DARK_RED + "Beitritt gesperrt");
                    } else if (arenaGame.getCountdownManager().isCountdownRunning()) {
                        // Lobby-Countdown läuft → Start-Timer anzeigen
                        int secondsLeft = arenaGame.getCountdownManager().getRemainingSeconds();
                        sign.setLine(2, ChatColor.GREEN + "Startet in " + ChatColor.DARK_RED + ChatColor.BOLD + secondsLeft + "s");
                        sign.setLine(3, ChatColor.RED + "hurry up!");
                    } else {
                        // Lobby, kein Countdown
                        sign.setLine(2, ChatColor.YELLOW + "" + current + " von mind. " + min);
                        sign.setLine(3, (current >= min ? ChatColor.GREEN + "Startbereit" : ChatColor.GRAY + "Wartelobby"));
                    }
                    sign.update();
                }
            }
        }
    }

}
