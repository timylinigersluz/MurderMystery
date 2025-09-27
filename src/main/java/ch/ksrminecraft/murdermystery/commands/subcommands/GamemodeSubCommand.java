package ch.ksrminecraft.murdermystery.commands.subcommands;

import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import ch.ksrminecraft.murdermystery.managers.game.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class GamemodeSubCommand implements SubCommand {

    private final GameManager gameManager;
    private final ConfigManager configManager;

    public GamemodeSubCommand(GameManager gameManager, ConfigManager configManager) {
        this.gameManager = gameManager;
        this.configManager = configManager;
    }

    @Override
    public String getName() {
        return "gamemode";
    }

    @Override
    public String getDescription() {
        return "Setzt den Spielmodus (classic oder bow-fallback)";
    }

    @Override
    public String getUsage() {
        return "/mm gamemode <classic|bow-fallback>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("murdermystery.admin")) {
            sender.sendMessage(ChatColor.RED + "Keine Berechtigung!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Verwendung: " + getUsage());
            return;
        }

        String mode = args[1].toLowerCase();
        if (!mode.equals("classic") && !mode.equals("bow-fallback")) {
            sender.sendMessage(ChatColor.RED + "Ungültiger Modus! Erlaubt: classic, bow-fallback");
            return;
        }

        gameManager.setGameMode(mode);
        configManager.setGameMode(mode);

        sender.sendMessage(ChatColor.GREEN + "Spielmodus geändert auf: " + ChatColor.AQUA + mode);
        configManager.debug("Admin hat Spielmodus geändert: " + mode);
    }
}
