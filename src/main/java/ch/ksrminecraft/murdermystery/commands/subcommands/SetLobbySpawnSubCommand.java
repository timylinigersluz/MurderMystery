package ch.ksrminecraft.murdermystery.commands.subcommands;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.support.ArenaManager;
import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import ch.ksrminecraft.murdermystery.model.Arena;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetLobbySpawnSubCommand implements SubCommand {

    private final ArenaManager arenaManager;
    private final ConfigManager configManager;

    public SetLobbySpawnSubCommand(ArenaManager arenaManager, ConfigManager configManager) {
        this.arenaManager = arenaManager;
        this.configManager = configManager;
    }

    @Override
    public String getName() {
        return "setlobbyspawn";
    }

    @Override
    public String getDescription() {
        return "Setzt den Lobby-Spawn (MainWorldLobby oder ArenaLobby).";
    }

    @Override
    public String getUsage() {
        return "/mm setlobbyspawn [arena]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(ChatColor.RED + "Dieser Befehl kann nur ingame genutzt werden.");
            MurderMystery.getInstance().debug("[Command] /mm setlobbyspawn von " + sender.getName() + " → abgebrochen (kein Spieler).");
            return;
        }

        Location loc = p.getLocation();
        String locStr = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() +
                " (Yaw=" + loc.getYaw() + ", Pitch=" + loc.getPitch() + ")";

        if (args.length == 1) {
            // MainWorld-Lobby setzen
            configManager.setMainLobby(loc);
            sender.sendMessage(ChatColor.GREEN + "MainWorld-Lobby-Spawn gesetzt: "
                    + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
            MurderMystery.getInstance().debug("[Command] /mm setlobbyspawn von " + p.getName() +
                    " → MainWorldLobby-Spawn gesetzt bei " + locStr);
        } else if (args.length == 2) {
            // Arena-Lobby setzen
            String arenaName = args[1].toLowerCase();
            Arena arena = arenaManager.getArena(arenaName);
            if (arena == null) {
                sender.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' existiert nicht.");
                MurderMystery.getInstance().debug("[Command] /mm setlobbyspawn von " + p.getName() +
                        " → Fehler: Arena '" + arenaName + "' existiert nicht.");
                return;
            }

            arena.setArenaLobbySpawnPoint(loc);
            configManager.setArenaLobby(arenaName, loc);

            sender.sendMessage(ChatColor.GREEN + "ArenaLobby-Spawn für Arena '" + arenaName + "' gesetzt: "
                    + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
            MurderMystery.getInstance().debug("[Command] /mm setlobbyspawn von " + p.getName() +
                    " → ArenaLobby '" + arenaName + "' gesetzt bei " + locStr);
        } else {
            sender.sendMessage(ChatColor.RED + "Benutzung: " + getUsage());
            MurderMystery.getInstance().debug("[Command] /mm setlobbyspawn von " + p.getName() + " → falsche Argumente.");
        }
    }
}
