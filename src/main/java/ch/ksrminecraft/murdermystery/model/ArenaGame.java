package ch.ksrminecraft.murdermystery.model;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.effects.Broadcaster;
import ch.ksrminecraft.murdermystery.managers.effects.CelebrationManager;
import ch.ksrminecraft.murdermystery.managers.effects.ItemManager;
import ch.ksrminecraft.murdermystery.managers.game.*;
import ch.ksrminecraft.murdermystery.managers.support.BossBarManager;
import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import ch.ksrminecraft.murdermystery.managers.support.CountdownManager;
import ch.ksrminecraft.murdermystery.managers.support.MapManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ArenaGame extends GameManager {

    // ---------------- FIELDS ----------------
    private final Arena arena;
    private final MapManager mapManager;

    private final CountdownManager countdownManager;
    private final WinConditionManager winConditionManager;
    private final RoundResultManager roundResultManager;

    // ---------------- CONSTRUCTOR ----------------
    public ArenaGame(MurderMystery plugin,
                     Arena arena,
                     PointsManager pointsManager,
                     ConfigManager configManager,
                     MapManager mapManager) {
        super(pointsManager, plugin.getArenaManager(), plugin, configManager);
        this.arena = arena;
        this.mapManager = mapManager;

        this.countdownManager = new CountdownManager(this, plugin);
        this.winConditionManager = new WinConditionManager(this);
        CelebrationManager celebrationManager = new CelebrationManager(plugin);
        this.roundResultManager = new RoundResultManager(this, pointsManager);

        plugin.debug("[ArenaGame] Arena '" + arena.getName() + "' erstellt.");
    }

    // ---------------- GAME FLOW ----------------

    @Override
    public void startCountdown() {
        super.startCountdown();
        plugin.debug("[ArenaGame] startCountdown() → Arena=" + arena.getName());
        countdownManager.startCountdown(getCountdownTime());
    }

    @Override
    public void startGame() {
        super.startGame();

        plugin.debug("[ArenaGame] startGame() → Arena=" + arena.getName() + ", Spieler=" + getPlayers().size());

        // Rollen zuweisen
        Map<UUID, Role> assigned = RoleManager.assignRoles(getPlayers());
        roles.clear();
        roles.putAll(assigned);

        // Items und Nachrichten verteilen
        for (UUID uuid : assigned.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) continue;

            Role role = assigned.get(uuid);
            plugin.debug("[ArenaGame] Rolle zugewiesen → Spieler=" + p.getName() + ", Rolle=" + role);

            switch (role) {
                case MURDERER -> {
                    p.getInventory().addItem(ItemManager.createMurdererSword());
                    p.sendMessage(ChatColor.DARK_RED + "Du bist der Mörder!");
                    p.sendTitle(ChatColor.DARK_RED + "🔪 Mörder", ChatColor.GRAY + "Eliminiere alle Spieler!", 10, 60, 20);
                }
                case DETECTIVE -> {
                    p.getInventory().addItem(ItemManager.createDetectiveBow());
                    p.getInventory().addItem(new ItemStack(Material.ARROW, 1));
                    p.sendMessage(ChatColor.AQUA + "Du bist der Detective!");
                    p.sendTitle(ChatColor.AQUA + "🔎 Detective", ChatColor.GRAY + "Beschütze die Unschuldigen!", 10, 60, 20);
                }
                case BYSTANDER -> {
                    p.sendMessage(ChatColor.YELLOW + "Du bist unschuldig. Überlebe!");
                    p.sendTitle(ChatColor.YELLOW + "👤 Unschuldig", ChatColor.GRAY + "Überlebe so lange wie möglich!", 10, 60, 20);
                }
            }
        }

        bossBarManager.cancelLobbyBar();
        gameTimerManager.start(configManager.getMaxGameSeconds());

        Broadcaster.broadcastMessage(getPlayers(), ChatColor.GREEN + "Das Spiel in Arena '" + arena.getName() + "' hat begonnen!");

        // Spieler auf definierte Arena-Spawns verteilen
        List<Location> spawns = arena.getArenaGameSpawnPoints();
        if (spawns != null && !spawns.isEmpty()) {
            List<UUID> shuffled = new ArrayList<>(getPlayers());
            Collections.shuffle(shuffled);

            for (int i = 0; i < shuffled.size(); i++) {
                Player p = Bukkit.getPlayer(shuffled.get(i));
                if (p == null || !p.isOnline()) continue;

                // Teleport über MapManager → Gamemode & Debug inklusive
                mapManager.teleportToArenaGameSpawn(p, arena);
            }
        } else {
            plugin.debug("[ArenaGame] Keine GameSpawns → Teleportiere Spieler in ArenaLobby.");
            for (UUID uuid : getPlayers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    mapManager.teleportToArenaLobby(p, arena);
                }
            }
        }
    }

    @Override
    public void checkWinConditions() {
        RoundResultManager.EndCondition condition =
                winConditionManager.checkWinConditions(getPlayers(), getRoles(), getOrCreateRoundStats());

        plugin.debug("[ArenaGame] checkWinConditions() aufgerufen (Arena=" + arena.getName() +
                ", Spieler=" + getPlayers().size() + ")");

        if (condition != null) {
            plugin.debug("[ArenaGame] WinCondition erfüllt → Runde wird beendet (" + condition + ")");
            endRound(condition);
        }
    }

    @Override
    public void handleTimeout() {
        plugin.debug("[ArenaGame] Timeout → Arena=" + arena.getName());
        RoundResultManager.EndCondition condition =
                winConditionManager.forceTimeoutEnd(getPlayers(), getRoles(), getOrCreateRoundStats());
        endRound(condition);
    }

    @Override
    public void endRound(RoundResultManager.EndCondition condition) {
        if (!isGameStarted()) {
            plugin.debug("[ArenaGame] endRound() aufgerufen, aber kein Spiel aktiv (Arena=" + arena.getName() + ")");
            return;
        }
        gameStarted = false;

        plugin.debug("[ArenaGame] Arena '" + arena.getName() + "' endet mit Condition=" + condition);

        // Punkte & Stats
        roundResultManager.handleRoundEnd(condition, getRoles(), getOrCreateRoundStats());

        // Spieler und Spectator-UUIDs sichern
        var playersToKick = new HashSet<>(getPlayers());
        var spectatorsToKick = new HashSet<>(getSpectators());
        plugin.debug("[ArenaGame] Vor Reset → Players=" + playersToKick.size() + ", Spectators=" + spectatorsToKick.size());

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            resetGame();

            // Alle Spieler & Spectators zurück in MainLobby
            for (UUID uuid : playersToKick) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    mapManager.teleportToMainLobby(p);
                }
            }
            for (UUID uuid : spectatorsToKick) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    mapManager.teleportToMainLobby(p);
                }
            }

            plugin.debug("[ArenaGame] Spieler aus Arena '" + arena.getName() + "' nach MainLobby teleportiert.");
        }, 100L);
    }

    // ---------------- PLAYER HANDLING ----------------

    @Override
    public void eliminate(Player victim, Player killer) {
        UUID uuid = victim.getUniqueId();

        if (players.remove(uuid)) {
            spectators.add(uuid);
            plugin.debug("[ArenaGame] Spieler " + victim.getName() +
                    " aus Players entfernt und zu Spectators hinzugefügt (Arena=" + arena.getName() + ")");
        }

        // Items clearen & Spectator setzen
        victim.getInventory().clear();
        victim.setGameMode(GameMode.SPECTATOR);

        bossBarManager.removePlayer(victim);
        bossBarManager.addPlayer(victim, BossBarManager.Mode.GAME);

        String killerName = killer != null ? killer.getName() : "Umwelt";
        plugin.debug("[ArenaGame] Spieler " + victim.getName() + " eliminiert von " + killerName);

        getOrCreateRoundStats().addKill(killer != null ? killer.getUniqueId() : null, uuid);

        Role role = roles.get(uuid);
        plugin.debug("[ArenaGame] Rolle von " + victim.getName() + " war " + role);

        if (role == Role.DETECTIVE) {
            Broadcaster.broadcastMessage(players, ChatColor.BLUE + "👤 Der Detective wurde eliminiert!");
            victim.getWorld().dropItemNaturally(victim.getLocation(), ItemManager.createDetectiveBow());
            victim.getWorld().dropItemNaturally(victim.getLocation(), new ItemStack(Material.ARROW, 1));
        } else if (role == Role.BYSTANDER) {
            Broadcaster.broadcastMessage(players, ChatColor.YELLOW + "👤 Ein Unschuldiger wurde eliminiert!");
        } else if (role == Role.MURDERER) {
            Broadcaster.broadcastMessage(players, ChatColor.DARK_RED + "🔪 Der Mörder " + victim.getName() + " wurde eliminiert!");
        }

        // Spectator-Spawn nutzen (MapManager)
        mapManager.teleportToSpectatorSpawn(victim, arena);

        checkWinConditions();
    }

    public void handleJoin(Player player) {
        getPlayerManager().handleJoin(player, arena);
        plugin.debug("[ArenaGame] Spieler '" + player.getName() +
                "' Arena='" + arena.getName() + "' beigetreten. Spieler=" + getPlayers().size());
    }

    public void handleLeave(Player player) {
        getPlayerManager().handleLeave(player, arena);
        plugin.debug("[ArenaGame] Spieler '" + player.getName() +
                "' Arena='" + arena.getName() + "' verlassen. Spieler=" + getPlayers().size());
    }

    // ---------------- GETTER / UTILITY ----------------

    public Arena getArena() {
        return arena;
    }

    public CountdownManager getCountdownManager() {
        return countdownManager;
    }

    public boolean isPlayerInGame(Player player) {
        UUID uuid = player.getUniqueId();
        // Spieler gilt als "inGame", wenn er aktiv spielt oder Spectator ist
        return getPlayers().contains(uuid) || getSpectators().contains(uuid);
    }
}
