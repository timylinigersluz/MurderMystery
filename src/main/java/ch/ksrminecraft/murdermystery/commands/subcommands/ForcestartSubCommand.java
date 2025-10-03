package ch.ksrminecraft.murdermystery.commands.subcommands;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.game.GameManagerRegistry;
import ch.ksrminecraft.murdermystery.model.ArenaGame;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ForcestartSubCommand implements SubCommand {

    private final GameManagerRegistry registry;

    public ForcestartSubCommand(GameManagerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() { return "forcestart"; }

    @Override
    public String getDescription() { return "Startet das Spiel sofort (Admin-Befehl)"; }

    @Override
    public String getUsage() { return "/mm forcestart [arenaName]"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Nur Spieler können diesen Befehl nutzen.");
            MurderMystery.getInstance().debug("[Command] /mm forcestart von Konsole → abgebrochen (nur Spieler erlaubt).");
            return;
        }

        if (!player.hasPermission("murdermystery.admin")) {
            player.sendMessage(ChatColor.RED + "Keine Berechtigung!");
            MurderMystery.getInstance().debug("[Command] /mm forcestart von " + player.getName() + " → fehlende Permission.");
            return;
        }

        ArenaGame game = null;
        if (args.length >= 2) {
            game = registry.getGameManager(args[1].toLowerCase());
            MurderMystery.getInstance().debug("[Command] /mm forcestart von " + player.getName() + " → Arena per Name: " + args[1]);
        } else {
            game = registry.findArenaOfPlayer(player);
            MurderMystery.getInstance().debug("[Command] /mm forcestart von " + player.getName() + " → Arena über findArenaOfPlayer()");
        }

        if (game == null) {
            player.sendMessage(ChatColor.RED + "Keine passende Arena gefunden!");
            MurderMystery.getInstance().debug("[Command] /mm forcestart von " + player.getName() + " → keine Arena gefunden.");
            return;
        }

        if (game.isGameStarted()) {
            player.sendMessage(ChatColor.RED + "Das Spiel läuft bereits!");
            MurderMystery.getInstance().debug("[Command] /mm forcestart von " + player.getName() + " in Arena "
                    + game.getArena().getName() + " → Runde läuft bereits.");
            return;
        }

        MurderMystery.getInstance().debug("[Command] /mm forcestart von " + player.getName() + " → Arena "
                + game.getArena().getName() + " wird gestartet.");
        game.startGame();
        player.sendMessage(ChatColor.GREEN + "Arena '" + game.getArena().getName() + "' wurde gestartet!");
    }
}
