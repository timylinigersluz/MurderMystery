package ch.ksrminecraft.murdermystery.commands.subcommands;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import ch.ksrminecraft.murdermystery.managers.game.GameManagerRegistry;
import ch.ksrminecraft.murdermystery.model.ArenaGame;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GamemodeSubCommand implements SubCommand {

    private final GameManagerRegistry registry;
    private final ConfigManager configManager;

    public GamemodeSubCommand(GameManagerRegistry registry, ConfigManager configManager) {
        this.registry = registry;
        this.configManager = configManager;
    }

    @Override
    public String getName() { return "gamemode"; }

    @Override
    public String getDescription() { return "Setzt den Spielmodus (classic oder bow-fallback)"; }

    @Override
    public String getUsage() { return "/mm gamemode <classic|bow-fallback>"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player) || !player.hasPermission("murdermystery.admin")) {
            sender.sendMessage(ChatColor.RED + "Keine Berechtigung!");
            MurderMystery.getInstance().debug("[Command] /mm gamemode von " + sender.getName() + " → fehlende Permission.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Verwendung: " + getUsage());
            MurderMystery.getInstance().debug("[Command] /mm gamemode von " + player.getName() + " → kein Modus angegeben.");
            return;
        }

        String mode = args[1].toLowerCase();
        if (!mode.equals("classic") && !mode.equals("bow-fallback")) {
            sender.sendMessage(ChatColor.RED + "Ungültiger Modus! Erlaubt: classic, bow-fallback");
            MurderMystery.getInstance().debug("[Command] /mm gamemode von " + player.getName() + " → ungültiger Modus: " + mode);
            return;
        }

        ArenaGame game = registry.findArenaOfPlayer(player);
        if (game == null) {
            sender.sendMessage(ChatColor.RED + "Du bist in keiner Arena!");
            MurderMystery.getInstance().debug("[Command] /mm gamemode von " + player.getName() + " → nicht in einer Arena.");
            return;
        }

        configManager.setArenaGameMode(game.getArena().getName(), mode);
        MurderMystery.getInstance().debug("[Command] /mm gamemode von " + player.getName() + " → Arena " + game.getArena().getName() + " Modus=" + mode);
        sender.sendMessage(ChatColor.GREEN + "Spielmodus in Arena '" + game.getArena().getName() + "' geändert auf: " + ChatColor.AQUA + mode);
    }

}
