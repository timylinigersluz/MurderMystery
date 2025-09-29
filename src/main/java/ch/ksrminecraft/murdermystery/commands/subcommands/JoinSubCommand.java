package ch.ksrminecraft.murdermystery.commands.subcommands;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.game.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JoinSubCommand implements SubCommand {

    private final GameManager gameManager;
    private final MurderMystery plugin;
    private static final String PERMISSION = "murdermystery.join";

    public JoinSubCommand(GameManager gameManager) {
        this.gameManager = gameManager;
        this.plugin = MurderMystery.getInstance();
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getDescription() {
        return "Tritt einer MurderMystery-Runde bei (optional mit Größenauswahl)";
    }

    @Override
    public String getUsage() {
        return "/mm join [small|mid|large]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Nur Spieler können /mm join nutzen.");
            return;
        }

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(ChatColor.RED + "Du hast keine Berechtigung, einer Runde beizutreten.");
            return;
        }

        // Blockiere Join während laufendem Spiel
        if (gameManager.isGameStarted()) {
            player.sendMessage(ChatColor.RED + "Das Spiel läuft gerade. Bitte warte bis zur nächsten Runde!");
            return;
        }

        String size = null;
        if (args.length > 1) {
            String arg = args[1].toLowerCase();
            if (arg.equals("small") || arg.equals("mid") || arg.equals("large")) {
                size = arg;
            } else {
                player.sendMessage(ChatColor.RED + "Ungültige Größe. Nutze: small, mid oder large.");
                return;
            }
        }

        plugin.debug("Spieler " + player.getName() + " nutzt /mm join"
                + (size != null ? " mit size=" + size : ""));

        if (size != null) {
            gameManager.handleJoin(player, size); // Variante mit Arena-Size
        } else {
            gameManager.handleJoin(player);      // Standard-Join
        }
    }
}
