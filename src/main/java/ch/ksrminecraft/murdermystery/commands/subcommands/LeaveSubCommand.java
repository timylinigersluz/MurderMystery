package ch.ksrminecraft.murdermystery.commands.subcommands;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.utils.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveSubCommand implements SubCommand {

    private final GameManager gameManager;
    private static final String PERMISSION = "murdermystery.leave";

    public LeaveSubCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public String getDescription() {
        return "Verlasse eine MurderMystery-Runde";
    }

    @Override
    public String getUsage() {
        return "/mm leave";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Nur Spieler können /mm leave nutzen.");
            return;
        }

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(ChatColor.RED + "Du hast keine Berechtigung, eine Runde zu verlassen.");
            return;
        }

        MurderMystery.getInstance().debug("Spieler " + player.getName() + " nutzt /mm leave");
        gameManager.handleLeave(player);
    }
}
