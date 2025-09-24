package ch.ksrminecraft.murdermystery.commands.subcommands;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.utils.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ForcestartSubCommand implements SubCommand {

    private final GameManager gameManager;

    public ForcestartSubCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public String getName() {
        return "forcestart";
    }

    @Override
    public String getDescription() {
        return "Startet das Spiel sofort (Admin-Befehl)";
    }

    @Override
    public String getUsage() {
        return "/mm forcestart";
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

        if (gameManager.isGameStarted()) {
            player.sendMessage(ChatColor.RED + "Das Spiel läuft bereits!");
            return;
        }

        MurderMystery.getInstance().debug("Admin " + player.getName() + " hat forcestart genutzt.");
        gameManager.startGame();
        player.sendMessage(ChatColor.GREEN + "Spiel wurde per Forcestart gestartet.");
    }
}
