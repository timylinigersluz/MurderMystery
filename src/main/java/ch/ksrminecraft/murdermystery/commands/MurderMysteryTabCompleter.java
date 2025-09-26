package ch.ksrminecraft.murdermystery.commands;

import ch.ksrminecraft.murdermystery.commands.subcommands.SubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MurderMysteryTabCompleter implements TabCompleter {

    private final List<SubCommand> subCommands;

    public MurderMysteryTabCompleter(List<SubCommand> subCommands) {
        this.subCommands = subCommands;
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
}
