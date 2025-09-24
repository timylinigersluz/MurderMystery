package ch.ksrminecraft.murdermystery.utils;

import ch.ksrminecraft.murdermystery.MurderMystery;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerManager {

    private final GameManager gameManager;
    private final MurderMystery plugin;
    private final MapManager mapManager;

    public PlayerManager(GameManager gameManager, MurderMystery plugin) {
        this.gameManager = gameManager;
        this.plugin = plugin;
        this.mapManager = new MapManager(plugin);
    }

    public void handleJoin(Player p) {
        UUID uuid = p.getUniqueId();

        // Spiel läuft schon → Join blockieren, Spieler bleibt in Lobby
        if (gameManager.isGameStarted()) {
            sendToLobby(p);
            p.sendMessage(ChatColor.YELLOW + "Es läuft gerade eine MurderMystery-Runde.");
            p.sendMessage(ChatColor.GRAY + "Bitte warte in der Lobby, bis die Runde vorbei ist.");
            plugin.debug("Join von " + p.getName() + " blockiert → Spiel läuft bereits.");
            gameManager.getBossBarManager().addPlayer(p, BossBarManager.Mode.GAME);
            return;
        }

        // Spieler zur Lobby hinzufügen
        if (gameManager.getPlayers().add(uuid)) {
            resetPlayer(p);
            sendToLobby(p);

            plugin.debug("Spieler " + p.getName() + " ist der Lobby beigetreten. Spielerzahl="
                    + gameManager.getPlayers().size());

            gameManager.getBossBarManager().addPlayer(p, BossBarManager.Mode.LOBBY);

            if (gameManager.getPlayers().size() >= gameManager.getMinPlayers()) {
                plugin.debug("Mindestanzahl erreicht (" + gameManager.getPlayers().size() + "/"
                        + gameManager.getMinPlayers() + "). Countdown wird gestartet.");
                gameManager.startCountdown();
            }
        } else {
            p.sendMessage(ChatColor.GRAY + "Du bist schon in der Lobby");
        }
    }

    public void handleLeave(Player p) {
        UUID uuid = p.getUniqueId();

        if (!gameManager.getPlayers().contains(uuid)) {
            p.sendMessage(ChatColor.GRAY + "Du bist in keinem Spiel.");
            plugin.debug("Leave abgelehnt: Spieler " + p.getName() + " war nicht im Spiel.");
            return;
        }

        gameManager.getPlayers().remove(uuid);
        gameManager.getSpectators().remove(uuid);
        RoleManager.removePlayer(uuid);

        sendToMainWorld(p);
        p.sendMessage(ChatColor.YELLOW + "Du hast die MurderMystery-Runde verlassen.");
        plugin.debug("Spieler " + p.getName() + " hat das Spiel verlassen.");

        if (gameManager.getPlayers().isEmpty()) {
            plugin.debug("Letzter Spieler hat das Spiel verlassen. Reset wird ausgeführt.");
            gameManager.resetGame();
        }
    }

    public void eliminate(Player p) {
        UUID uuid = p.getUniqueId();
        if (gameManager.getSpectators().add(uuid)) {
            gameManager.getPlayers().remove(uuid);
            p.setGameMode(GameMode.SPECTATOR);
            p.sendMessage(ChatColor.RED + "Du wurdest getötet");
            plugin.debug("Spieler " + p.getName() + " wurde eliminiert und ist jetzt Spectator.");
            gameManager.checkWinConditions();
        }
    }

    public void startGame(Set<UUID> players, Map<UUID, Role> roles) {
        plugin.debug("Spielstart mit " + players.size() + " Spielern.");
        roles.putAll(RoleManager.assignRoles(players));

        mapManager.teleportToRandomMap(players);
        plugin.debug("Alle Spieler wurden in eine zufällige Map teleportiert.");

        for (Map.Entry<UUID, Role> entry : roles.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline()) continue;

            sendRoleMessage(p, entry.getValue());
            p.getInventory().clear();

            switch (entry.getValue()) {
                case DETECTIVE -> {
                    p.getInventory().addItem(ItemManager.createDetectiveBow());
                    p.getInventory().addItem(new ItemStack(Material.ARROW, 1));
                }
                case MURDERER -> p.getInventory().addItem(ItemManager.createMurdererSword());
                case BYSTANDER -> {
                    // keine Items
                }
            }
        }
    }

    public void resetGame(Set<UUID> players) {
        mapManager.teleportToMainWorld(players);
        players.clear();
        plugin.debug("Spiel zurückgesetzt: Alle Spieler in MainWorld teleportiert.");
    }

    // ================= Hilfsmethoden =================

    private void resetPlayer(Player p) {
        p.setGameMode(GameMode.ADVENTURE);
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        p.getInventory().setItemInOffHand(null);
    }

    private void sendToLobby(Player p) {
        FileConfiguration cfg = plugin.getConfig();
        String lobbyWorldName = cfg.getString("worlds.lobby");
        World lobby = Bukkit.getWorld(lobbyWorldName);
        if (lobby != null) {
            p.teleport(lobby.getSpawnLocation());
        }
    }

    private void sendToMainWorld(Player p) {
        FileConfiguration cfg = plugin.getConfig();
        String mainWorldName = cfg.getString("worlds.main");
        World main = Bukkit.getWorld(mainWorldName);
        if (main != null) {
            p.teleport(main.getSpawnLocation());
        }
    }

    private void sendRoleMessage(Player player, Role role) {
        player.sendMessage(ChatColor.GRAY + "=========================");
        switch (role) {
            case MURDERER -> player.sendMessage(ChatColor.GOLD + "Du bist der " + ChatColor.DARK_RED + "Murderer");
            case DETECTIVE -> player.sendMessage(ChatColor.GOLD + "Du bist der " + ChatColor.BLUE + "Detective");
            default -> player.sendMessage(ChatColor.GOLD + "Du bist ein " + ChatColor.GREEN + "Innocent");
        }
        player.sendMessage(ChatColor.GRAY + "=========================");
    }
}
