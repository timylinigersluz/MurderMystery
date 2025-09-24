package ch.ksrminecraft.murdermystery.commands.subcommands;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.utils.Arena;
import ch.ksrminecraft.murdermystery.utils.ArenaManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class SetSpawnSubCommand implements SubCommand {

    private final MurderMystery plugin;
    private final ArenaManager arenaManager;

    public SetSpawnSubCommand(ArenaManager arenaManager, MurderMystery plugin) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
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
            sender.sendMessage(ChatColor.RED + "Dafür hast du keine Berechtigung!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Verwendung: " + getUsage());
            return;
        }

        String arenaName = args[1].toLowerCase();
        Arena arena = arenaManager.getArena(arenaName);
        if (arena == null) {
            sender.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' existiert nicht!");
            return;
        }

        Location loc = player.getLocation();
        World arenaWorld = arena.getWorld();
        if (arenaWorld == null || !arenaWorld.equals(loc.getWorld())) {
            sender.sendMessage(ChatColor.RED + "Du musst dich in der Welt der Arena befinden!");
            return;
        }

        // Koordinaten speichern: nur x,y,z
        String entry = loc.getBlockX() + ", " +
                loc.getBlockY() + ", " +
                loc.getBlockZ();

        FileConfiguration cfg = plugin.getConfig();
        List<String> spawns = cfg.getStringList("arenas." + arenaName + ".spawns");
        spawns.add(entry);
        cfg.set("arenas." + arenaName + ".spawns", spawns);
        plugin.saveConfig();

        // Arenen neu laden
        arenaManager.reload();

        // Anzahl Spawns in Arena anzeigen
        Arena updated = arenaManager.getArena(arenaName);
        int totalSpawns = (updated != null) ? updated.getSpawnPoints().size() : spawns.size();

        player.sendMessage(ChatColor.GREEN + "Spawnpunkt hinzugefügt in Arena '" + arenaName + "'.");
        player.sendMessage(ChatColor.YELLOW + "Aktuelle Anzahl Spawns: " + totalSpawns);
        plugin.debug("SetSpawn: Neuer Spawn in Arena '" + arenaName + "' bei " + entry);
    }
}
