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
        sender.sendMessage(ChatColor.GOLD + "===== " + ChatColor.AQUA + "MurderMystery Befehle" + ChatColor.GOLD + " =====");

        boolean isAdmin = sender instanceof Player p && p.hasPermission("murdermystery.admin");

        for (SubCommand sub : subCommands) {
            String name = sub.getName().toLowerCase();

            // Admin-Befehle: nur anzeigen, wenn Admin
            if (isAdminOnly(name) && !isAdmin) {
                continue;
            }

            sender.sendMessage(ChatColor.YELLOW + sub.getUsage()
                    + ChatColor.GRAY + " - "
                    + ChatColor.WHITE + sub.getDescription());
        }

        sender.sendMessage(ChatColor.GOLD + "===============================");
    }

    private boolean isAdminOnly(String name) {
        return switch (name) {
            case "forcestart", "stop", "reload", "setspawn", "setlobbyspawn", "reset" -> true;
            default -> false;
        };
    }
}
