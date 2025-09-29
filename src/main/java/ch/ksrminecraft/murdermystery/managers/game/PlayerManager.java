package ch.ksrminecraft.murdermystery.managers.game;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.listeners.SignListener;
import ch.ksrminecraft.murdermystery.managers.effects.Broadcaster;
import ch.ksrminecraft.murdermystery.managers.effects.ItemManager;
import ch.ksrminecraft.murdermystery.managers.support.ArenaManager;
import ch.ksrminecraft.murdermystery.managers.support.BossBarManager;
import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import ch.ksrminecraft.murdermystery.managers.support.MapManager;
import ch.ksrminecraft.murdermystery.model.Arena;
import ch.ksrminecraft.murdermystery.model.Role;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PlayerManager {

    private final GameManager gameManager;
    private final MurderMystery plugin;
    private final MapManager mapManager;
    private final ConfigManager configManager;
    private final ArenaManager arenaManager;

    public PlayerManager(GameManager gameManager, MurderMystery plugin, ArenaManager arenaManager, ConfigManager configManager) {
        this.gameManager = gameManager;
        this.plugin = plugin;
        this.mapManager = new MapManager(plugin, arenaManager);
        this.configManager = configManager;
        this.arenaManager = arenaManager;
    }

    // Standard-Join (ohne Arena-Filter)
    public void handleJoin(Player p) {
        joinInternal(p, null);
    }

    // Join mit Arena-Size (small/mid/large)
    public void handleJoin(Player p, String size) {
        joinInternal(p, size);
    }

    private void joinInternal(Player p, String size) {
        UUID uuid = p.getUniqueId();

        if (gameManager.isGameStarted()) {
            mapManager.teleportToLobby(p);
            p.sendMessage(ChatColor.YELLOW + "Es läuft gerade eine MurderMystery-Runde.");
            p.sendMessage(ChatColor.GRAY + "Bitte warte in der Lobby, bis die Runde vorbei ist.");
            plugin.debug("Join von " + p.getName() + " blockiert → Spiel läuft bereits.");
            gameManager.getBossBarManager().addPlayer(p, BossBarManager.Mode.GAME);
            return;
        }

        if (gameManager.getPlayers().add(uuid)) {
            resetPlayer(p);
            mapManager.teleportToLobby(p);

            plugin.debug("Spieler " + p.getName() + " ist der Lobby beigetreten. Spielerzahl=" + gameManager.getPlayers().size()
                    + (size != null ? " (gewünschte Grösse: " + size + ")" : ""));

            gameManager.getBossBarManager().addPlayer(p, BossBarManager.Mode.LOBBY);

            int needed = gameManager.getMinPlayers() - gameManager.getPlayers().size();
            if (needed > 0) {
                Bukkit.broadcastMessage(ChatColor.AQUA + p.getName() + ChatColor.GRAY + " hat die Wartelobby betreten. Es werden noch "
                        + ChatColor.GOLD + needed + ChatColor.GRAY + " Spieler benötigt, um das Spiel zu starten.");
            } else {
                Bukkit.broadcastMessage(ChatColor.GREEN + "Mindestanzahl erreicht! Spiel startet bald...");
                gameManager.startCountdown();
            }

            SignListener.updateJoinSigns(plugin);
        } else {
            p.sendMessage(ChatColor.GRAY + "Du bist schon in der Lobby");
        }
    }

    /**
     * Leave-Handling: Meldung, Strafpunkte, Rollenlogik.
     */
    public void handleLeave(Player p) {
        UUID uuid = p.getUniqueId();
        Role role = RoleManager.getRole(uuid);

        gameManager.getPlayers().remove(uuid);
        gameManager.getSpectators().remove(uuid);
        RoleManager.removePlayer(uuid);

        gameManager.getBossBarManager().removePlayer(p);

        // --- Strafpunkte ---
        int penalty = Math.abs(configManager.getPointsQuit());
        gameManager.getPointsManager().applyPenalty(uuid, penalty, "Spiel verlassen");
        int newPoints = gameManager.getPointsManager().getPoints(uuid);

        // Nachricht an den Quitter
        p.sendMessage(ChatColor.RED + "Du hast die Runde verlassen und -" + penalty + " Punkte erhalten.");
        p.sendMessage(ChatColor.GRAY + "Neuer Punktestand: " + ChatColor.GOLD + newPoints);

        // --- Rollen-spezifische Behandlung ---
        if (role == Role.DETECTIVE) {
            // Inventar leeren & Bogen/Pfeil droppen
            p.getInventory().remove(Material.BOW);
            p.getInventory().remove(Material.ARROW);
            p.getWorld().dropItemNaturally(p.getLocation(), ItemManager.createDetectiveBow());
            p.getWorld().dropItemNaturally(p.getLocation(), new ItemStack(Material.ARROW, 1));

            Broadcaster.broadcastMessage(gameManager.getPlayers(),
                    ChatColor.BLUE + "🔎 Der Detective hat die Runde verlassen!");
            plugin.debug("Detective " + p.getName() + " hat die Runde verlassen → Items gedroppt.");

        } else if (role == Role.BYSTANDER) {
            Broadcaster.broadcastMessage(gameManager.getPlayers(),
                    ChatColor.YELLOW + "👤 Ein Unschuldiger hat die Runde verlassen!");
            plugin.debug("Bystander " + p.getName() + " hat die Runde verlassen.");

        } else if (role == Role.MURDERER) {
            Broadcaster.broadcastMessage(gameManager.getPlayers(),
                    ChatColor.DARK_RED + "🔪 Der Murderer hat die Runde verlassen!");
            plugin.debug("Murderer " + p.getName() + " hat die Runde verlassen → sofortiges Spielende.");
            gameManager.endRound(RoundResultManager.EndCondition.DETECTIVE_WIN);
            return; // ⬅ nichts mehr danach ausführen!
        }

        // Zurück in MainWorld
        mapManager.teleportToMainWorld(p);
        plugin.debug("Spieler " + p.getName() + " zurück in MainWorld teleportiert.");

        SignListener.updateJoinSigns(plugin);

        if (gameManager.getPlayers().isEmpty()) {
            plugin.debug("Letzter Spieler hat das Spiel verlassen. Reset wird ausgeführt.");
            gameManager.resetGame();
        }
    }

    public void eliminate(Player victim, Player killer) {
        // --- NEU: nach Spielschluss nichts mehr machen ---
        if (!gameManager.isGameStarted()) {
            plugin.debug("Eliminate von " + victim.getName() + " ignoriert (Spiel bereits beendet).");
            return;
        }

        UUID victimId = victim.getUniqueId();

        if (gameManager.getSpectators().add(victimId)) {
            gameManager.getPlayers().remove(victimId);

            if (RoleManager.getRole(victimId) == Role.DETECTIVE) {
                // Bogen + Pfeil aus Inventar entfernen
                victim.getInventory().remove(Material.BOW);
                victim.getInventory().remove(Material.ARROW);

                // Sicherstellen, dass auch custom Detective-Bogen raus ist
                victim.getInventory().removeItem(ItemManager.createDetectiveBow());

                // Drop neuen Detective-Bogen + 1 Pfeil am Todesort
                victim.getWorld().dropItemNaturally(victim.getLocation(), ItemManager.createDetectiveBow());
                victim.getWorld().dropItemNaturally(victim.getLocation(), new ItemStack(Material.ARROW, 1));

                plugin.debug("Detective " + victim.getName() + " wurde getötet → Bogen & Pfeil gedroppt.");
            }

            if (killer != null) {
                UUID killerId = killer.getUniqueId();
                Role killerRole = RoleManager.getRole(killerId);

                // --- Nur Punkte/Kills zählen, wenn Spiel läuft ---
                if (killerRole == Role.MURDERER) {
                    gameManager.getOrCreateRoundStats().addKill(killerId, victimId);
                } else if (killerRole == Role.DETECTIVE &&
                        RoleManager.getRole(victimId) == Role.BYSTANDER) {
                    gameManager.getOrCreateRoundStats().addDetectiveInnocentKill(killerId);
                }
            }

            victim.setGameMode(GameMode.SPECTATOR);
            victim.sendMessage(ChatColor.RED + "Du wurdest getötet");

            gameManager.checkWinConditions();
        }
    }

    public void startGame(Set<UUID> players, Map<UUID, Role> roles, String arenaSize) {
        plugin.debug("Spielstart mit " + players.size() + " Spielern.");
        roles.putAll(RoleManager.assignRoles(players));

        Arena chosenArena = (arenaSize != null)
                ? arenaManager.getRandomArenaBySize(arenaSize)
                : arenaManager.getRandomArena();
        if (chosenArena == null) chosenArena = arenaManager.getRandomArena();

        // --- Spawnpunkte prüfen ---
        List<Location> spawnPoints = new ArrayList<>(chosenArena.getSpawnPoints());
        if (spawnPoints.size() < players.size()) {
            plugin.getLogger().severe("Zu wenige Spawnpunkte in Arena '" + chosenArena.getName() +
                    "'! (" + spawnPoints.size() + " < " + players.size() + ")");
            Bukkit.broadcastMessage(ChatColor.RED + "Arena hat zu wenige Spawnpunkte! Bitte Admin informieren.");
            // Notfall → fallback: alle auf einen Random-Spawn
            spawnPoints = Collections.nCopies(players.size(), chosenArena.getRandomSpawn());
        } else {
            Collections.shuffle(spawnPoints);
        }

        int i = 0;
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                Location spawn = spawnPoints.get(i++);
                p.teleport(spawn);
                plugin.debug("Spieler " + p.getName() + " → Arena '" + chosenArena.getName()
                        + "', Spawn=" + spawn.getBlockX() + "," + spawn.getBlockY() + "," + spawn.getBlockZ());
            }
        }

        // Rollen zuweisen + Inventare
        for (Map.Entry<UUID, Role> entry : roles.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline()) continue;

            sendRoleMessage(p, entry.getValue());
            p.getInventory().clear();

            switch (entry.getValue()) {
                case DETECTIVE -> {
                    p.getInventory().setItem(7, new ItemStack(Material.ARROW, 1));
                    p.getInventory().setItem(8, ItemManager.createDetectiveBow());
                }
                case MURDERER -> p.getInventory().setItem(8, ItemManager.createMurdererSword());
                case BYSTANDER -> {}
            }
            p.updateInventory();
        }
    }

    public void resetGame(Set<UUID> players) {
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                resetPlayer(p);
                mapManager.teleportToMainWorld(p);
                plugin.debug("Reset für Spieler " + p.getName() + " → Main-World.");
            }
        }
        SignListener.updateJoinSigns(plugin);
    }

    private void resetPlayer(Player p) {
        p.setGameMode(configManager.getPlayerGameMode());
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.setSaturation(20);
        p.setFireTicks(0);
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        p.getInventory().setItemInOffHand(null);
        ItemManager.clearSpecialItems(p);
    }

    private void sendRoleMessage(Player player, Role role) {
        player.sendMessage(ChatColor.GRAY + "=========================");
        switch (role) {
            case MURDERER -> player.sendMessage(ChatColor.GOLD + "Du bist der " + ChatColor.DARK_RED + "Mörder");
            case DETECTIVE -> player.sendMessage(ChatColor.GOLD + "Du bist der " + ChatColor.BLUE + "Detektiv");
            default -> player.sendMessage(ChatColor.GOLD + "Du bist ein " + ChatColor.GREEN + "Unschuldiger");
        }
        player.sendMessage(ChatColor.GRAY + "=========================");
    }

    public MapManager getMapManager() {
        return mapManager;
    }
}
