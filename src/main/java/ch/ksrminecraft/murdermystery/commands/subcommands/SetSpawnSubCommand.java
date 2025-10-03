package ch.ksrminecraft.murdermystery.commands.subcommands;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.support.ArenaManager;
import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import ch.ksrminecraft.murdermystery.model.Arena;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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
        return "Fügt einen Arena-Spawnpunkt oder den Spectator-Spawn hinzu.";
    }

    @Override
    public String getUsage() {
        return "/mm setspawn <arena> [spectator]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Nur Spieler können diesen Befehl ausführen.");
            MurderMystery.getInstance().debug("[Command] /mm setspawn von Konsole → abgebrochen (nur Spieler).");
            return;
        }
        if (!player.hasPermission("murdermystery.admin")) {
            player.sendMessage(ChatColor.RED + "Dafür hast du keine Berechtigung!");
            MurderMystery.getInstance().debug("[Command] /mm setspawn von " + player.getName() + " → fehlende Permission.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Verwendung: " + getUsage());
            MurderMystery.getInstance().debug("[Command] /mm setspawn von " + player.getName() + " → kein ArenaName angegeben.");
            return;
        }

        String arenaName = args[1].toLowerCase();
        Arena arena = arenaManager.getArena(arenaName);
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' existiert nicht!");
            MurderMystery.getInstance().debug("[Command] /mm setspawn von " + player.getName() + " → Arena '" + arenaName + "' nicht gefunden.");
            return;
        }

        if (arena.getWorld() == null) {
            player.sendMessage(ChatColor.RED + "Arena '" + arenaName + "' hat keine gültige Welt!");
            MurderMystery.getInstance().debug("[Command] /mm setspawn von " + player.getName() + " → Arena '" + arenaName + "' ohne gültige Welt.");
            return;
        }

        String playerWorld = player.getWorld().getName();
        String arenaWorld = arena.getWorld().getName();
        if (!arenaWorld.equalsIgnoreCase(playerWorld)) {
            player.sendMessage(ChatColor.RED + "Du musst dich in der Welt '" + arenaWorld
                    + "' befinden (aktuell: " + playerWorld + ")!");
            MurderMystery.getInstance().debug("[Command] /mm setspawn von " + player.getName() + " → falsche Welt (Arena=" + arenaWorld + ", Spieler=" + playerWorld + ")");
            return;
        }

        Location loc = player.getLocation();

        // Mit yaw und pitch
        String entry = loc.getBlockX() + "," +
                loc.getBlockY() + "," +
                loc.getBlockZ() + "," +
                loc.getYaw() + "," +
                loc.getPitch();

        List<String> spawns = configManager.getArenaGameSpawns(arenaName);

        if (args.length >= 3 && args[2].equalsIgnoreCase("spectator")) {
            // Spectator-Spawn → vorherige entfernen
            spawns.removeIf(s -> s.endsWith(",SPECTATOR"));
            entry = entry + ",SPECTATOR";
            spawns.add(entry);

            // Speichern
            configManager.setArenaGameSpawns(arenaName, spawns);

            // Arena-Objekt aktualisieren
            arena.setSpectatorSpawnPoint(loc);

            player.sendMessage(ChatColor.GREEN + "Spectator-Spawn für Arena '" + arenaName + "' gesetzt!");
            MurderMystery.getInstance().debug("[Command] /mm setspawn von " + player.getName()
                    + " → Spectator-Spawn gesetzt für Arena '" + arenaName
                    + "' @ " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        } else {
            // Normaler Game-Spawn
            spawns.add(entry);
            configManager.setArenaGameSpawns(arenaName, spawns);

            // Arena neu laden, damit Liste aktualisiert ist
            arenaManager.reload();

            int spawnCount = arenaManager.getArena(arenaName).getArenaGameSpawnPoints().size();
            player.sendMessage(ChatColor.GREEN + "Spawnpunkt in Arena '" + arenaName + "' hinzugefügt!");
            player.sendMessage(ChatColor.YELLOW + "Aktuelle Anzahl Spawns: " + spawnCount);

            MurderMystery.getInstance().debug("[Command] /mm setspawn von " + player.getName()
                    + " → GameSpawn gesetzt für Arena '" + arenaName
                    + "' @ " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()
                    + " (Total jetzt " + spawnCount + " Spawns).");
        }
    }
}
