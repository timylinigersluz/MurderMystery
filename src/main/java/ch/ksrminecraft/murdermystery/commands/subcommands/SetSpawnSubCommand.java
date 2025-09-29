package ch.ksrminecraft.murdermystery.commands.subcommands;

import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import ch.ksrminecraft.murdermystery.managers.support.ArenaManager;
import ch.ksrminecraft.murdermystery.model.Arena;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SetSpawnSubCommand implements SubCommand {

    private final ArenaManager arenaManager;
    private final ConfigManager configManager;

    public SetSpawnSubCommand(ArenaManager arenaManager, ConfigManager configManager) {
        this.arenaManager = arenaManager;
        this.configManager = configManager;
    }

    @Override
    public String getName() {
        return "setspawn";
    }

    @Override
    public String getDescription() {
        return "Fügt einen Spawnpunkt in einer Arena oder der Lobby hinzu.";
    }

    @Override
    public String getUsage() {
        return "/mm setspawn <arena|lobby>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Nur Spieler können diesen Befehl ausführen.");
            return;
        }
        if (!player.hasPermission("murdermystery.admin")) {
            player.sendMessage(ChatColor.RED + "Dafür hast du keine Berechtigung!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Verwendung: " + getUsage());
            return;
        }

        String target = args[1].toLowerCase();

        // --- Lobby ---
        if (target.equals("lobby")) {
            if (!player.getWorld().getName().equalsIgnoreCase(configManager.getLobbyWorld())) {
                player.sendMessage(ChatColor.RED + "Du musst dich in der Lobby-Welt befinden ("
                        + configManager.getLobbyWorld() + ")!");
                return;
            }

            String entry = player.getLocation().getBlockX() + "," +
                    player.getLocation().getBlockY() + "," +
                    player.getLocation().getBlockZ();

            List<String> spawns = configManager.getLobbySpawns();
            spawns.add(entry);
            configManager.setLobbySpawns(spawns);

            player.sendMessage(ChatColor.GREEN + "Spawnpunkt in der Lobby hinzugefügt!");
            player.sendMessage(ChatColor.YELLOW + "Aktuelle Anzahl Lobby-Spawns: " + spawns.size());
            configManager.debug("SetSpawn: Neuer Lobby-Spawn bei " + entry);
            return;
        }

        // --- Arena ---
        Arena arena = arenaManager.getArena(target);
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Arena '" + target + "' existiert nicht!");
            return;
        }

        // Sicherstellen, dass Arena überhaupt eine Welt hat
        if (arena.getWorld() == null) {
            player.sendMessage(ChatColor.RED + "Arena '" + target + "' hat keine gültige Welt in der Config!");
            return;
        }

        // Vergleich über den Welt-Namen statt World-Objekt
        String playerWorld = player.getWorld().getName();
        String arenaWorld = arena.getWorld().getName();
        if (!arenaWorld.equalsIgnoreCase(playerWorld)) {
            player.sendMessage(ChatColor.RED + "Du musst dich in der Welt '" + arenaWorld
                    + "' befinden (aktuell: " + playerWorld + ")!");
            return;
        }

        String entry = player.getLocation().getBlockX() + "," +
                player.getLocation().getBlockY() + "," +
                player.getLocation().getBlockZ();

        List<String> spawns = configManager.getArenaSpawns(target);
        spawns.add(entry);
        configManager.setArenaSpawns(target, spawns);

        arenaManager.reload();

        player.sendMessage(ChatColor.GREEN + "Spawnpunkt hinzugefügt in Arena '" + target + "'.");
        player.sendMessage(ChatColor.YELLOW + "Aktuelle Anzahl Spawns: " +
                arenaManager.getArena(target).getSpawnPoints().size());
        configManager.debug("SetSpawn: Neuer Spawn in Arena '" + target + "' bei " + entry);
    }
}
