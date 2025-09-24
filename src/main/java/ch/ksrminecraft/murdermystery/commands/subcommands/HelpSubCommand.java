package ch.ksrminecraft.murdermystery.commands.subcommands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class HelpSubCommand implements SubCommand {

    private final List<SubCommand> subCommands;

    public HelpSubCommand(List<SubCommand> subCommands) {
        this.subCommands = subCommands;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Zeigt alle verfügbaren Befehle an";
    }

    @Override
    public String getUsage() {
        return "/mm help";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.AQUA + "MurderMystery Befehle:");

        boolean isAdmin = sender instanceof Player p && p.hasPermission("murdermystery.admin");

        for (SubCommand sub : subCommands) {
            String name = sub.getName();

            // Admin-Befehle nur anzeigen, wenn Spieler Admin ist
            if ((name.equalsIgnoreCase("forcestart") || name.equalsIgnoreCase("stop")) && !isAdmin) {
                continue;
            }

            sender.sendMessage(ChatColor.GRAY + sub.getUsage() + ChatColor.WHITE + " - " + sub.getDescription());
        }
    }
}
