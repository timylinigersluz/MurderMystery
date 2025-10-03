package ch.ksrminecraft.murdermystery.commands;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.commands.subcommands.*;
import ch.ksrminecraft.murdermystery.managers.game.GameManagerRegistry;
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

    public MurderMysteryCommand(MurderMystery plugin, GameManagerRegistry registry, ConfigManager configManager) {
        this.plugin = plugin;

        // SubCommands bekommen jetzt die Registry statt eines einzelnen GameManagers
        subCommands.add(new JoinSubCommand(registry));
        subCommands.add(new LeaveSubCommand(registry));
        subCommands.add(new HelpSubCommand(subCommands));
        subCommands.add(new ForcestartSubCommand(registry));
        subCommands.add(new GamemodeSubCommand(registry, configManager));
        subCommands.add(new StopSubCommand(registry));
        subCommands.add(new ReloadSubCommand(configManager, registry));
        subCommands.add(new SetSpawnSubCommand(plugin.getArenaManager(), configManager));
        subCommands.add(new ResetSubCommand(plugin, registry));
        subCommands.add(new SetLobbySpawnSubCommand(plugin.getArenaManager(), configManager));

        plugin.debug("[Command] MurderMysteryCommand initialisiert mit " + subCommands.size() + " SubCommands.");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command cmd,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Verwendung: /mm <subcommand>");
            plugin.debug("[Command] /mm von " + sender.getName() + " → keine Argumente.");
            return true;
        }

        String subName = args[0].toLowerCase();
        plugin.debug("[Command] /mm von " + sender.getName() + " → SubCommand=" + subName);

        for (SubCommand sub : subCommands) {
            if (sub.getName().equalsIgnoreCase(subName)) {
                plugin.debug("[Command] /mm → " + sender.getName() + " führt SubCommand '" + sub.getName() + "' aus.");
                sub.execute(sender, args);
                return true;
            }
        }

        sender.sendMessage(ChatColor.RED + "Unbekannter Subcommand. Nutze /mm help");
        plugin.debug("[Command] /mm von " + sender.getName() + " → SubCommand nicht gefunden: " + subName);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command cmd,
                                                @NotNull String alias,
                                                @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();
        String subName = args.length > 0 ? args[0].toLowerCase() : "";

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (SubCommand sub : subCommands) {
                if (sub.getName().toLowerCase().startsWith(prefix)) {
                    suggestions.add(sub.getName());
                }
            }
        } else if (args.length == 2) {
            switch (subName) {
                case "setspawn":
                case "setlobbyspawn":
                case "forcestart":
                case "stop":
                case "reset":
                    String arenaPrefix = args[1].toLowerCase();
                    plugin.getArenaManager().getAllArenas().stream()
                            .map(Arena::getName)
                            .filter(name -> name.toLowerCase().startsWith(arenaPrefix))
                            .forEach(suggestions::add);
                    break;
            }
        }
        return suggestions;
    }
}
