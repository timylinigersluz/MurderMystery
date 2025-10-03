package ch.ksrminecraft.murdermystery.commands.subcommands;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.listeners.SignListener;
import ch.ksrminecraft.murdermystery.managers.game.GameManagerRegistry;
import ch.ksrminecraft.murdermystery.model.ArenaGame;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;

public class StopSubCommand implements SubCommand {

    private final GameManagerRegistry registry;

    public StopSubCommand(GameManagerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() { return "stop"; }

    @Override
    public String getDescription() { return "Beendet die aktuelle MurderMystery-Runde (Admin)"; }

    @Override
    public String getUsage() { return "/mm stop [arenaName]"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("murdermystery.admin")) {
            sender.sendMessage(ChatColor.RED + "Keine Berechtigung!");
            MurderMystery.getInstance().debug("[Command] /mm stop von " + sender.getName() + " → fehlende Permission.");
            return;
        }

        ArenaGame game = null;
        if (args.length >= 2) {
            game = registry.getGameManager(args[1].toLowerCase());
            MurderMystery.getInstance().debug("[Command] /mm stop von " + sender.getName() + " → Arena per Name: " + args[1]);
        } else if (sender instanceof Player p) {
            game = registry.findArenaOfPlayer(p);
            MurderMystery.getInstance().debug("[Command] /mm stop von " + p.getName() + " → Arena über findArenaOfPlayer()");
        }

        if (game == null) {
            sender.sendMessage(ChatColor.RED + "Keine passende Arena gefunden!");
            MurderMystery.getInstance().debug("[Command] /mm stop von " + sender.getName() + " → keine Arena gefunden.");
            return;
        }

        if (!game.isGameStarted()) {
            sender.sendMessage(ChatColor.RED + "Es läuft aktuell keine Runde!");
            MurderMystery.getInstance().debug("[Command] /mm stop von " + sender.getName() + " → in Arena " + game.getArena().getName() + " läuft keine Runde.");
            return;
        }

        // Spieler und Spectators vor Reset sichern
        var playersToKick = new HashSet<>(game.getPlayers());
        var spectatorsToKick = new HashSet<>(game.getSpectators());

        // Reset
        game.resetGame();
        SignListener.updateJoinSigns(MurderMystery.getInstance(), registry);

        // Danach alle in MainLobby teleportieren
        for (var uuid : playersToKick) {
            var p = MurderMystery.getInstance().getServer().getPlayer(uuid);
            if (p != null && p.isOnline()) {
                MurderMystery.getInstance().getMapManager().teleportToMainLobby(p);
            }
        }
        for (var uuid : spectatorsToKick) {
            var p = MurderMystery.getInstance().getServer().getPlayer(uuid);
            if (p != null && p.isOnline()) {
                MurderMystery.getInstance().getMapManager().teleportToMainLobby(p);
            }
        }

        sender.sendMessage(ChatColor.GREEN + "Runde in Arena '" + game.getArena().getName() + "' wurde beendet.");
        MurderMystery.getInstance().debug("[Command] /mm stop von " + sender.getName()
                + " → Arena '" + game.getArena().getName() + "' beendet und alle Spieler in MainLobby teleportiert.");
    }
}
