package ch.ksrminecraft.murdermystery.commands.subcommands;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.game.GameManagerRegistry;
import ch.ksrminecraft.murdermystery.model.ArenaGame;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveSubCommand implements SubCommand {

    private final GameManagerRegistry registry;
    private static final String PERMISSION = "murdermystery.leave";

    public LeaveSubCommand(GameManagerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public String getDescription() {
        return "Verlasse eine MurderMystery-Runde oder gehe zurück in die MainLobby";
    }

    @Override
    public String getUsage() {
        return "/mm leave";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Nur Spieler können /mm leave nutzen.");
            MurderMystery.getInstance().debug("[Command] /mm leave von Konsole → abgebrochen (nur Spieler).");
            return;
        }

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(ChatColor.RED + "Du hast keine Berechtigung, /mm leave zu nutzen.");
            MurderMystery.getInstance().debug("[Command] /mm leave von " + player.getName() + " → fehlende Permission.");
            return;
        }

        ArenaGame game = registry.findArenaOfPlayer(player);

        if (game != null) {
            MurderMystery.getInstance().debug("[Command] /mm leave von " + player.getName() + " → verlässt Arena '" + game.getArena().getName() + "'.");
            game.handleLeave(player);
            MurderMystery.getInstance().getMapManager().teleportToMainLobby(player);
            player.sendMessage(ChatColor.YELLOW + "Du hast die MurderMystery-Runde verlassen.");
        } else {
            MurderMystery.getInstance().debug("[Command] /mm leave von " + player.getName() + " → war in keiner Arena, teleportiere MainLobby.");
            MurderMystery.getInstance().getMapManager().teleportToMainLobby(player);
            player.sendMessage(ChatColor.GREEN + "Du bist zurück in der MainLobby.");
        }
    }
}