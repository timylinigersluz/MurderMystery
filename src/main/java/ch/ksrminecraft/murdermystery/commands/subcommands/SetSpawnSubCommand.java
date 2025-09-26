package ch.ksrminecraft.murdermystery.commands.subcommands;

import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import ch.ksrminecraft.murdermystery.managers.support.ArenaManager;
import ch.ksrminecraft.murdermystery.model.Arena;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
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
        return "Fügt einen Spawnpunkt in einer Arena hinzu.";
    }

    @Override
    public String getUsage() {
        return "/mm setspawn <arena>";
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

        String arenaName = args[1].toLowerCase();
        Arena arena = arenaManager.getArena(arenaName);
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' existiert nicht!");
            return;
        }

        Location loc = player.getLocation();
        World arenaWorld = arena.getWorld();
        if (arenaWorld == null || !arenaWorld.equals(loc.getWorld())) {
            player.sendMessage(ChatColor.RED + "Du musst dich in der Welt der Arena befinden!");
            return;
        }

        // Koordinaten speichern
        String entry = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();

        List<String> spawns = configManager.getArenaSpawns(arenaName);
        spawns.add(entry);
        configManager.setArenaSpawns(arenaName, spawns);

        // Arenen neu laden
        arenaManager.reload();

        int totalSpawns = arenaManager.getArena(arenaName).getSpawnPoints().size();

        player.sendMessage(ChatColor.GREEN + "Spawnpunkt hinzugefügt in Arena '" + arenaName + "'.");
        player.sendMessage(ChatColor.YELLOW + "Aktuelle Anzahl Spawns: " + totalSpawns);

        configManager.debug("SetSpawn: Neuer Spawn in Arena '" + arenaName + "' bei " + entry);
    }
}
