package ch.ksrminecraft.murdermystery.commands.subcommands;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import ch.ksrminecraft.murdermystery.managers.game.GameManagerRegistry;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReloadSubCommand implements SubCommand {

    private final ConfigManager configManager;
    private final GameManagerRegistry registry;

    public ReloadSubCommand(ConfigManager configManager, GameManagerRegistry registry) {
        this.configManager = configManager;
        this.registry = registry;
    }

    @Override
    public String getName() { return "reload"; }

    @Override
    public String getDescription() { return "Lädt die Config neu (nur Admins)"; }

    @Override
    public String getUsage() { return "/mm reload"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        MurderMystery.getInstance().debug("[Command] /mm reload ausgeführt von " + sender.getName());

        if (!(sender instanceof Player p) || !p.hasPermission("murdermystery.admin")) {
            sender.sendMessage(ChatColor.RED + "Keine Berechtigung!");
            MurderMystery.getInstance().debug("[Command] /mm reload → fehlende Berechtigung bei " + sender.getName());
            return;
        }

        MurderMystery.getInstance().debug("[Command] /mm reload → starte Config-Reload …");
        configManager.reload();
        MurderMystery.getInstance().debug("[Command] /mm reload → ConfigManager erfolgreich neu geladen.");

        MurderMystery.getInstance().getArenaManager().reload();
        MurderMystery.getInstance().debug("[Command] /mm reload → ArenaManager erfolgreich neu geladen.");

        sender.sendMessage(ChatColor.GREEN + "Config erfolgreich neu geladen!");
        MurderMystery.getInstance().debug("[Command] /mm reload von " + sender.getName() + " erfolgreich abgeschlossen.");
    }
}
