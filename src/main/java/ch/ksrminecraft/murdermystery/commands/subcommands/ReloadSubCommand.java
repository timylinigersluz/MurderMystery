package ch.ksrminecraft.murdermystery.commands.subcommands;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.game.GameManager;
import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReloadSubCommand implements SubCommand {

    private final ConfigManager configManager;
    private final GameManager gameManager;

    public ReloadSubCommand(ConfigManager configManager, GameManager gameManager) {
        this.configManager = configManager;
        this.gameManager = gameManager;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Lädt die Config neu (nur Admins)";
    }

    @Override
    public String getUsage() {
        return "/mm reload";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // nur Admins
        if (!(sender instanceof Player p) || !p.hasPermission("murdermystery.admin")) {
            sender.sendMessage(ChatColor.RED + "Du hast keine Berechtigung, diesen Befehl zu nutzen!");
            return;
        }

        // Config neu laden
        configManager.reload();

        // Werte ins GameManager übertragen
        gameManager.setMinPlayers(configManager.getMinPlayers());
        gameManager.setCountdownTime(configManager.getCountdownSeconds());
        gameManager.setGameMode(configManager.getGameMode());

        // Arenen ebenfalls neu laden (damit /mm setspawn & Co. direkt wirken)
        MurderMystery.getInstance().getArenaManager().reload();

        sender.sendMessage(ChatColor.GREEN + "Config erfolgreich neu geladen!");
        MurderMystery.getInstance().debug("Config wurde von " + sender.getName() + " neu geladen.");
    }
}
