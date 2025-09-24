package ch.ksrminecraft.murdermystery.commands;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.commands.subcommands.*;
import ch.ksrminecraft.murdermystery.utils.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MurderMysteryCommand implements CommandExecutor {

    private final List<SubCommand> subCommands = new ArrayList<>();

    public MurderMysteryCommand(GameManager gameManager) {
        MurderMystery plugin = MurderMystery.getInstance();

        // Standard-Subcommands
        subCommands.add(new JoinSubCommand(gameManager));
        subCommands.add(new LeaveSubCommand(gameManager));
        subCommands.add(new HelpSubCommand(subCommands));
        subCommands.add(new ForcestartSubCommand(gameManager));
        subCommands.add(new GamemodeSubCommand(gameManager));

        // Neuer Admin-Befehl: /mm setspawn <arena>
        subCommands.add(new SetSpawnSubCommand(plugin.getArenaManager(), plugin));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command cmd,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Verwendung: /mm <subcommand>");
            return true;
        }

        String subName = args[0].toLowerCase();

        for (SubCommand sub : subCommands) {
            if (sub.getName().equalsIgnoreCase(subName)) {
                sub.execute(sender, args);
                return true;
            }
        }

        sender.sendMessage(ChatColor.RED + "Unbekannter Subcommand. Nutze /mm help");
        return true;
    }

    public List<SubCommand> getSubCommands() {
        return subCommands;
    }
}
