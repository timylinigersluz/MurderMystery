package ch.ksrminecraft.murdermystery.commands.subcommands;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.game.GameManagerRegistry;
import ch.ksrminecraft.murdermystery.model.ArenaGame;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JoinSubCommand implements SubCommand {

    private final GameManagerRegistry registry;
    private static final String PERMISSION = "murdermystery.join";

    public JoinSubCommand(GameManagerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getDescription() {
        return "Tritt einer MurderMystery-Runde bei (Arena-Name angeben)";
    }

    @Override
    public String getUsage() {
        return "/mm join <arenaName>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Nur Spieler können /mm join nutzen.");
            MurderMystery.getInstance().debug("[Command] /mm join von Konsole → abgebrochen (nur Spieler).");
            return;
        }

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(ChatColor.RED + "Du hast keine Berechtigung, einer Runde beizutreten.");
            MurderMystery.getInstance().debug("[Command] /mm join von " + player.getName() + " → fehlende Permission.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Bitte gib eine Arena an: " + getUsage());
            MurderMystery.getInstance().debug("[Command] /mm join von " + player.getName() + " → kein ArenaName angegeben.");
            return;
        }

        String arenaName = args[1].toLowerCase();
        ArenaGame game = registry.getGameManager(arenaName);

        if (game == null) {
            player.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' existiert nicht!");
            MurderMystery.getInstance().debug("[Command] /mm join von " + player.getName() + " → Arena '" + arenaName + "' nicht gefunden.");
            return;
        }

        // Spieler ins Spiel aufnehmen
        MurderMystery.getInstance().debug("[Command] /mm join von " + player.getName() + " → betritt Arena '" + arenaName + "'.");
        game.handleJoin(player);

        // Spieler sofort in die Arena-Lobby teleportieren
        MurderMystery.getInstance().getMapManager().teleportToArenaLobby(player, game.getArena());
    }


}
