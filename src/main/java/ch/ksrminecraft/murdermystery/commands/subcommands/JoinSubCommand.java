package ch.ksrminecraft.murdermystery.commands.subcommands;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.utils.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JoinSubCommand implements SubCommand {

    private final GameManager gameManager;
    private static final String PERMISSION = "murdermystery.join";

    public JoinSubCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getDescription() {
        return "Tritt einer MurderMystery-Runde bei";
    }

    @Override
    public String getUsage() {
        return "/mm join";
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

        MurderMystery.getInstance().debug("Spieler " + player.getName() + " nutzt /mm join");
        gameManager.handleJoin(player);
    }
}
