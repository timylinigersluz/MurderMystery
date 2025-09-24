package ch.ksrminecraft.murdermystery.Utils;

import ch.ksrminecraft.murdermystery.MurderMystery;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public class GameManager {

    private final MurderMystery plugin;
    private final PointsManager pointsManager;
    private final Set<UUID> players = new HashSet<>();
    private final Set<UUID> spectators = new HashSet<>();
    private final Map<UUID, Role> roles = new HashMap<>();
    private boolean gameStarted = false;
    private boolean countdownRunning = false;
    private BukkitTask countdownTask;

    private int minPlayers = 3;
    private int countdownTime = 5;

    private final int punkteGewinner;
    private final int punkteMitGewinner;
    private final int punkteVerlierer;

    // Konstruktor
    public GameManager(PointsManager pointsManager, MurderMystery plugin) {
        this.pointsManager = pointsManager;
        this.plugin = plugin;
        this.punkteGewinner = plugin.getConfig().getInt("punkte-gewinner");
        this.punkteMitGewinner = plugin.getConfig().getInt("punkte-mitgewinner");
        this.punkteVerlierer = plugin.getConfig().getInt("punkte-verlierer");
    }

    // Getter für Spieler
    public Collection<UUID> getPlayers() {
        return this.players;
    }

    public void setMinPlayers(int min) {
        this.minPlayers = min;
    }

    public void setCountdownTime(int sec) {
        this.countdownTime = sec;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    // Tote Spieler zählen nicht als "im Spiel"
    public boolean isPlayerInGame(Player p) {
        UUID uuid = p.getUniqueId();
        return players.contains(uuid) && !spectators.contains(uuid);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Spieler dem Spiel hinzufügen
    public void handleJoin(Player p) {
        UUID uuid = p.getUniqueId();

        if (gameStarted) {
            p.sendMessage(ChatColor.YELLOW + "Spiel wurde schon gestartet :(");
            return;
        }
        if (players.add(uuid)) {
            p.setGameMode(GameMode.ADVENTURE);
            p.getInventory().clear();
            p.getInventory().setArmorContents(null);
            p.getInventory().setItemInOffHand(null);

            FileConfiguration config = MurderMystery.getInstance().getConfig();
            String lobbyWorldName = config.getString("worlds.lobby");
            World lobbyWorld = Bukkit.getWorld(lobbyWorldName);

            if (lobbyWorld != null) {
                p.teleport(lobbyWorld.getSpawnLocation());
            } else {
                Bukkit.getLogger().severe("Lobbywelt '" + lobbyWorldName + "' wurde nicht gefunden!");
            }

            // Spiel starten, sobald Mindestanzahl Spieler erreicht wird
            if (players.size() >= minPlayers && !countdownRunning) {
                startCountdown();
            }
        } else {
            p.sendMessage(ChatColor.GRAY + "Du bist schon in der Lobby");
        }
    }

    // Countdown in der Lobby vor Spielstart (Zeit s ---> Config)
    private void startCountdown() {
        countdownRunning = true;
        final int[] timeLeft = {countdownTime};

        countdownTask = Bukkit.getScheduler().runTaskTimer(
                MurderMystery.getInstance(),
                () -> {
                    if (timeLeft[0] <= 0) {
                        countdownTask.cancel();
                        countdownRunning = false;
                        startGame();
                        return;
                    }
                    broadcastToPlayers(ChatColor.AQUA + "Spiel startet in " + timeLeft[0] + " Sekunden...");
                    timeLeft[0]--;
                },
                0L, 20L
        );
    }

    // Spiel nach Countdown starten
    public void startGame() {
        if (gameStarted) return;
        gameStarted = true;

        broadcastToPlayers(ChatColor.LIGHT_PURPLE+ "Spiel startet");

        // Rollenverteilung
        roles.putAll(RoleManager.assignRoles(players));

        // Spieler in zufällige Map teleportieren
        MapManager.teleportToRandomMap(players);

        // Spieler erfahren ihre Rolle
        for (Map.Entry<UUID, Role> e : roles.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null && p.isOnline()) {
                sendRoleMessage(p, e.getValue());

                // Items nach Rolle verteilen
                switch (e.getValue()) {
                    case DETECTIVE -> {
                        p.getInventory().clear();
                        p.getInventory().addItem(ItemManager.createDetectiveBow());
                        p.getInventory().addItem(new ItemStack(Material.ARROW, 1));
                    }
                    case MURDERER -> {
                        p.getInventory().clear();
                        p.getInventory().addItem(ItemManager.createMurdererSword());
                    }
                    case BYSTANDER -> {
                        p.getInventory().clear();

                    }
                }

            }
        }
    }

    // Methode nur für Spieler, die im Spiel sterben
    public void eliminate(Player player) {
        UUID uuid = player.getUniqueId();
        if (spectators.add(uuid)) {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(ChatColor.RED + "Du wurdest getötet");
            checkWinConditions();
        }
    }

    // Spiel für neue Runde vorbereiten und jetzige Runde beenden
    public void resetGame() {
        gameStarted = false;
        countdownRunning = false;
        if (countdownTask != null) countdownTask.cancel();

        spectators.clear();
        roles.clear();

        MapManager.teleportToMainWorld(players);

        players.clear();
    }

    // Wird nach jeder Eliminierung aufgerufen und überprüft
    private void checkWinConditions() {
        // Alle Spieler (lebend + tote)
        Set<UUID> alive = new HashSet<>(players);
        // Spectators = tote Spieler -> abziehen
        alive.removeAll(spectators);

        boolean murdererAlive = roles.entrySet().stream()
                .anyMatch(e -> e.getValue() == Role.MURDERER && alive.contains(e.getKey()));

        // Murderer ist tot
        if (!murdererAlive) {
            Player detective = RoleManager.getDetective();

            // Murderer durch Detective (-Bogen) getötet
            if (MurderMystery.getInstance().isMurdererKilledByBow()) {
                pointsManager.addPointsToPlayer(detective.getUniqueId(), this.punkteGewinner);
                detective.sendMessage(ChatColor.GREEN + "Du hast gewonnen!");
                broadcastToPlayers(ChatColor.AQUA + "Der Detective hat gewonnen!");

            } else {
                // Murderer nicht durch Bogen getötet ---> Bystander gewinnen, Detective bekommt Mitgewinner-Punkte
                if (detective != null) {
                    pointsManager.addPointsToPlayer(detective.getUniqueId(), this.punkteMitGewinner);
                }
                broadcastToPlayers(ChatColor.AQUA + "Die Bystander haben gewonnen!");
            }

            // Punkte an alle lebenden Bystander in jedem Fall
            for (UUID uuid : alive) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline() && roles.get(uuid) == Role.BYSTANDER) {
                    pointsManager.addPointsToPlayer(uuid, this.punkteMitGewinner);
                    p.sendMessage(ChatColor.GREEN + "Du hast gewonnen!");
                }
            }
            // Murderer Verlierer Punkte
            pointsManager.addPointsToPlayer(
                    roles.entrySet().stream()
                    .filter(e -> e.getValue() == Role.MURDERER)
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null), punkteVerlierer);

            resetGame();

        }

        // Wenn nur noch Murderer lebt
        boolean onlyMurdererLeft = (alive.size() == 1 &&
                roles.get(alive.iterator().next()) == Role.MURDERER);

        if (onlyMurdererLeft) {
            UUID murdererUuid = alive.iterator().next();
            Player murderer = Bukkit.getPlayer(murdererUuid);
            // Murderer PunkteGewinner
            pointsManager.addPointsToPlayer(murdererUuid, this.punkteGewinner);
            // Punkte an alle lebenden Bystander und Detective
            for (UUID uuid : alive) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline() && (roles.get(uuid) == Role.BYSTANDER || roles.get(uuid) == Role.DETECTIVE)) {
                    pointsManager.addPointsToPlayer(uuid, this.punkteVerlierer);
                }
            }
            murderer.sendMessage(ChatColor.DARK_RED + "Du hast gewonnen!");
            broadcastToPlayers(ChatColor.DARK_RED + "Der Murderer hat gewonnen!");

            resetGame();

        }
    }

    // Spieler erfahren ihre Rolle
    private void sendRoleMessage(Player player, Role role) {
        player.sendMessage(ChatColor.GRAY + "=========================");
        switch (role) {
            case MURDERER:
                player.sendMessage(ChatColor.GOLD + "Du bist der " + ChatColor.DARK_RED + "Murderer");
                break;
            case DETECTIVE:
                player.sendMessage(ChatColor.GOLD + "Du bist der " + ChatColor.BLUE + "Detective");
                break;
            default:
                player.sendMessage(ChatColor.GOLD + "Du bist ein " + ChatColor.GREEN + "Innocent");
        }
        player.sendMessage(ChatColor.GRAY + "=========================");
    }

    // Nachricht an Spieler des Spiels, nicht an alle auf dem Server
    public void broadcastToPlayers(String message) {
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        }
    }
}