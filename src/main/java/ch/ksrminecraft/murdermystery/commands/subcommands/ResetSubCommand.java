package ch.ksrminecraft.murdermystery.commands.subcommands;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.listeners.SignListener;
import ch.ksrminecraft.murdermystery.managers.game.GameManagerRegistry;
import ch.ksrminecraft.murdermystery.model.ArenaGame;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResetSubCommand implements SubCommand {

    private final MurderMystery plugin;
    private final GameManagerRegistry registry;

    public ResetSubCommand(MurderMystery plugin, GameManagerRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "reset";
    }

    @Override
    public String getDescription() {
        return "Bricht die aktuelle Runde ab und setzt alles zurück.";
    }

    @Override
    public String getUsage() {
        return "/mm reset [arenaName]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("murdermystery.admin")) {
            sender.sendMessage(ChatColor.RED + "Keine Berechtigung!");
            return;
        }

        ArenaGame game = null;
        if (args.length >= 2) {
            game = registry.getGameManager(args[1].toLowerCase());
        } else if (sender instanceof Player p) {
            game = registry.findArenaOfPlayer(p);
        }

        if (game == null) {
            sender.sendMessage(ChatColor.RED + "Keine passende Arena gefunden!");
            return;
        }

        // Reset der Arena
        game.resetGame();
        SignListener.updateJoinSigns(plugin, registry);

        // final-Referenz für die Lambdas
        final ArenaGame finalGame = game;

        // Alle Spieler sicher in die MainLobby schicken
        game.getPlayers().forEach(uuid -> {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null && p.isOnline()) {
                finalGame.getPlayerManager().getMapManager().teleportToMainLobby(p);
            }
        });

        // Alle Spectators sicher in die MainLobby schicken
        game.getSpectators().forEach(uuid -> {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null && p.isOnline()) {
                finalGame.getPlayerManager().getMapManager().teleportToMainLobby(p);
            }
        });
    }
}