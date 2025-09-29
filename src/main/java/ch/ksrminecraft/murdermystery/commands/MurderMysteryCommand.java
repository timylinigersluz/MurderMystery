package ch.ksrminecraft.murdermystery.commands;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.commands.subcommands.*;
import ch.ksrminecraft.murdermystery.managers.game.GameManager;
import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import ch.ksrminecraft.murdermystery.model.Arena;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MurderMysteryCommand implements CommandExecutor, TabCompleter {

    private final MurderMystery plugin;
    private final List<SubCommand> subCommands = new ArrayList<>();

    public MurderMysteryCommand(MurderMystery plugin, GameManager gameManager, ConfigManager configManager) {
        this.plugin = plugin; //

        subCommands.add(new JoinSubCommand(gameManager));
        subCommands.add(new LeaveSubCommand(gameManager));
        subCommands.add(new HelpSubCommand(subCommands));
        subCommands.add(new ForcestartSubCommand(gameManager));
        subCommands.add(new GamemodeSubCommand(gameManager, configManager));
        subCommands.add(new StopSubCommand(gameManager));
        subCommands.add(new ReloadSubCommand(configManager, gameManager));
        subCommands.add(new SetSpawnSubCommand(plugin.getArenaManager(), configManager));
        subCommands.add(new ResetSubCommand(plugin, gameManager));
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

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command cmd,
                                                @NotNull String alias,
                                                @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            for (SubCommand sub : subCommands) {
                suggestions.add(sub.getName());
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("setspawn")) {
            suggestions.add("lobby");
            suggestions.addAll(plugin.getArenaManager().getAllArenas()
                    .stream().map(Arena::getName).toList());
        }
        return suggestions;
    }
}
