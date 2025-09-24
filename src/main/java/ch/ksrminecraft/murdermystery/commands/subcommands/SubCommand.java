package ch.ksrminecraft.murdermystery.commands.subcommands;

import org.bukkit.command.CommandSender;

public interface SubCommand {
    String getName();             // z. B. "join"
    String getDescription();      // Beschreibung für /mm help
    String getUsage();            // z. B. "/mm join"
    void execute(CommandSender sender, String[] args);
}
