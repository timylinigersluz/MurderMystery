package ch.ksrminecraft.murdermystery.commands;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.commands.subcommands.*;
import ch.ksrminecraft.murdermystery.managers.game.GameManager;
import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MurderMysteryCommand extends Command implements TabCompleter {

    private final List<SubCommand> subCommands = new ArrayList<>();

    public MurderMysteryCommand(GameManager gameManager, ConfigManager configManager) {
        super("mm"); // Hauptbefehl

        MurderMystery plugin = MurderMystery.getInstance();

        this.setDescription("Hauptbefehl für MurderMystery");
        this.setUsage("/mm <subcommand>");
        this.setPermission("murdermystery.use");

        // Standard-Subcommands
        subCommands.add(new JoinSubCommand(gameManager));
        subCommands.add(new LeaveSubCommand(gameManager));
        subCommands.add(new HelpSubCommand(subCommands));
        subCommands.add(new ForcestartSubCommand(gameManager));
        subCommands.add(new GamemodeSubCommand(gameManager, configManager));
        subCommands.add(new StopSubCommand(gameManager));
        subCommands.add(new ReloadSubCommand(configManager, gameManager));

        // Admin-Befehl
        subCommands.add(new SetSpawnSubCommand(plugin.getArenaManager(), configManager));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender,
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

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            for (SubCommand sub : subCommands) {
                suggestions.add(sub.getName());
            }
        }
        return suggestions;
    }

    public List<SubCommand> getSubCommands() {
        return subCommands;
    }
}
