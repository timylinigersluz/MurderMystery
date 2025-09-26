package ch.ksrminecraft.murdermystery.managers.game;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.effects.ItemManager;
import ch.ksrminecraft.murdermystery.managers.support.ArenaManager;
import ch.ksrminecraft.murdermystery.managers.support.BossBarManager;
import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import ch.ksrminecraft.murdermystery.managers.support.MapManager;
import ch.ksrminecraft.murdermystery.model.Role;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerManager {

    private final GameManager gameManager;
    private final MurderMystery plugin;
    private final MapManager mapManager;
    private final ConfigManager configManager;

    public PlayerManager(GameManager gameManager, MurderMystery plugin, ArenaManager arenaManager, ConfigManager configManager) {
        this.gameManager = gameManager;
        this.plugin = plugin;
        this.mapManager = new MapManager(plugin, arenaManager);
        this.configManager = configManager;
    }

    public void handleJoin(Player p) {
        UUID uuid = p.getUniqueId();

        if (gameManager.isGameStarted()) {
            sendToLobby(p);
            p.sendMessage(ChatColor.YELLOW + "Es läuft gerade eine MurderMystery-Runde.");
            p.sendMessage(ChatColor.GRAY + "Bitte warte in der Lobby, bis die Runde vorbei ist.");
            plugin.debug("Join von " + p.getName() + " blockiert → Spiel läuft bereits.");
            gameManager.getBossBarManager().addPlayer(p, BossBarManager.Mode.GAME);
            return;
        }

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

        mapManager.teleportToRandomArena(players);
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
                case BYSTANDER -> { /* keine Items */ }
            }
        }
    }

    public void resetGame(Set<UUID> players) {
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                resetPlayer(p);
                sendToLobby(p);
                plugin.debug("Reset für Spieler " + p.getName() + " ausgeführt.");
            }
        }
    }

    // ================= Hilfsmethoden =================

    private void resetPlayer(Player p) {
        p.setGameMode(GameMode.SURVIVAL);
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.setSaturation(20);
        p.setFireTicks(0);

        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        p.getInventory().setItemInOffHand(null);

        for (ItemStack item : p.getInventory().getContents()) {
            if (ItemManager.isDetectiveBow(item) || ItemManager.isMurdererSword(item)) {
                p.getInventory().remove(item);
                plugin.debug("Cleanup: Entfernt Spezialitem bei Spieler " + p.getName());
            }
        }
    }

    private void sendToLobby(Player p) {
        String lobbyWorldName = configManager.getLobbyWorld();
        World lobby = Bukkit.getWorld(lobbyWorldName);
        if (lobby != null) {
            p.teleport(lobby.getSpawnLocation());
        }
    }

    private void sendToMainWorld(Player p) {
        String mainWorldName = configManager.getMainWorld();
        World main = Bukkit.getWorld(mainWorldName);
        if (main != null) {
            p.teleport(main.getSpawnLocation());
        }
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
}
