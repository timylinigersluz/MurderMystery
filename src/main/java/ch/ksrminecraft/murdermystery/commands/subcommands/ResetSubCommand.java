package ch.ksrminecraft.murdermystery.commands.subcommands;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.listeners.SignListener;
import ch.ksrminecraft.murdermystery.managers.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResetSubCommand implements SubCommand {

    private final MurderMystery plugin;
    private final GameManager gameManager;

    public ResetSubCommand(MurderMystery plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @Override
    public String getName() {
        return "reset";
    }

    @Override
    public String getDescription() {
        return "Bricht die aktuelle Runde ab und setzt alles zurück.";
    }

    @Override
    public String getUsage() {
        return "/mm reset";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("murdermystery.admin")) {
            sender.sendMessage(ChatColor.RED + "Keine Berechtigung!");
            return;
        }

        // Runde zurücksetzen
        gameManager.resetGame();

        // Alle Spieler in die MainWorld teleportieren
        for (Player p : Bukkit.getOnlinePlayers()) {
            gameManager.getPlayerManager().getMapManager().teleportToMainWorld(p);
        }

        // Join-Signs aktualisieren
        SignListener.updateJoinSigns(plugin);

        sender.sendMessage(ChatColor.GREEN + "MurderMystery wurde komplett zurückgesetzt.");
        plugin.debug("Reset-Befehl von " + sender.getName() + " ausgeführt.");
    }
}
