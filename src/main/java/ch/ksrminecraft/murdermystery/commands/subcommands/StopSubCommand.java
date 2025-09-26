package ch.ksrminecraft.murdermystery.commands.subcommands;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.game.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StopSubCommand implements SubCommand {

    private final GameManager gameManager;

    public StopSubCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public String getName() {
        return "stop";
    }

    @Override
    public String getDescription() {
        return "Beendet die aktuelle MurderMystery-Runde (Admin-Befehl)";
    }

    @Override
    public String getUsage() {
        return "/mm stop";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Nur Spieler können diesen Befehl nutzen.");
            return;
        }

        if (!player.hasPermission("murdermystery.admin")) {
            player.sendMessage(ChatColor.RED + "Du hast keine Berechtigung, diesen Befehl zu nutzen.");
            return;
        }

        if (!gameManager.isGameStarted()) {
            player.sendMessage(ChatColor.RED + "Es läuft aktuell keine Runde!");
            return;
        }

        MurderMystery.getInstance().debug("Admin " + player.getName() + " hat den Befehl /mm stop genutzt.");
        gameManager.resetGame();

        player.sendMessage(ChatColor.GREEN + "Die MurderMystery-Runde wurde erfolgreich beendet und zurückgesetzt.");
    }
}
