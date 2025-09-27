package ch.ksrminecraft.murdermystery.managers.game;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.listeners.SignListener;
import ch.ksrminecraft.murdermystery.managers.effects.ItemManager;
import ch.ksrminecraft.murdermystery.managers.support.ArenaManager;
import ch.ksrminecraft.murdermystery.managers.support.BossBarManager;
import ch.ksrminecraft.murdermystery.managers.support.ConfigManager;
import ch.ksrminecraft.murdermystery.managers.support.MapManager;
import ch.ksrminecraft.murdermystery.model.Arena;
import ch.ksrminecraft.murdermystery.model.Role;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
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

    // Gemeinsame Join-Logik
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

            plugin.debug("Spieler " + p.getName() + " ist der Lobby beigetreten. Spielerzahl="
                    + gameManager.getPlayers().size()
                    + (size != null ? " (gewünschte Größe: " + size + ")" : ""));

            gameManager.getBossBarManager().addPlayer(p, BossBarManager.Mode.LOBBY);

            int needed = gameManager.getMinPlayers() - gameManager.getPlayers().size();
            if (needed > 0) {
                Bukkit.broadcastMessage(ChatColor.AQUA + p.getName() +
                        ChatColor.GRAY + " hat die Wartelobby betreten. Es werden noch " +
                        ChatColor.GOLD + needed + ChatColor.GRAY +
                        " Spieler benötigt, um das Spiel zu starten.");
            } else {
                Bukkit.broadcastMessage(ChatColor.GREEN + "✅ Mindestanzahl erreicht! Spiel startet bald...");
                gameManager.startCountdown();
            }

            SignListener.updateJoinSigns(plugin);

        } else {
            p.sendMessage(ChatColor.GRAY + "Du bist schon in der Lobby");
        }
    }

    public void handleLeave(Player p) {
        UUID uuid = p.getUniqueId();

        gameManager.getPlayers().remove(uuid);
        gameManager.getSpectators().remove(uuid);
        RoleManager.removePlayer(uuid);

        mapManager.teleportToMainWorld(p);
        p.sendMessage(ChatColor.YELLOW + "Du hast die MurderMystery-Runde verlassen.");
        plugin.debug("Spieler " + p.getName() + " hat das Spiel verlassen.");

        // Update Join-Schilder
        SignListener.updateJoinSigns(plugin);

        if (gameManager.getPlayers().isEmpty()) {
            plugin.debug("Letzter Spieler hat das Spiel verlassen. Reset wird ausgeführt.");
            gameManager.resetGame();
        }
    }

    public void eliminate(Player victim, Player killer) {
        UUID victimId = victim.getUniqueId();

        if (gameManager.getSpectators().add(victimId)) {
            gameManager.getPlayers().remove(victimId);

            // Detective droppt Items
            if (RoleManager.getRole(victimId) == Role.DETECTIVE) {
                ItemStack bow = ItemManager.createDetectiveBow();
                ItemStack arrow = new ItemStack(Material.ARROW, 1);
                victim.getWorld().dropItemNaturally(victim.getLocation(), bow);
                victim.getWorld().dropItemNaturally(victim.getLocation(), arrow);
            }

            // Kill-Tracking
            if (killer != null) {
                UUID killerId = killer.getUniqueId();
                Role killerRole = RoleManager.getRole(killerId);

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

    // StartGame jetzt mit Arena-Size
    public void startGame(Set<UUID> players, Map<UUID, Role> roles, String arenaSize) {
        plugin.debug("Spielstart mit " + players.size() + " Spielern.");

        roles.putAll(RoleManager.assignRoles(players));

        Arena chosenArena = null;
        if (arenaSize != null) {
            chosenArena = arenaManager.getRandomArenaBySize(arenaSize);
            plugin.debug("Arena-Auswahl nach Größe (" + arenaSize + "): " +
                    (chosenArena != null ? chosenArena.getName() : "Fallback → random"));
        }
        if (chosenArena == null) {
            chosenArena = arenaManager.getRandomArena();
            plugin.debug("Arena-Auswahl → Standard Random");
        }

        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.teleport(chosenArena.getRandomSpawn());
                plugin.debug("Spieler " + p.getName() + " wurde nach Arena '" + chosenArena.getName() + "' teleportiert.");
            }
        }

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
                mapManager.teleportToMainWorld(p);
                plugin.debug("Reset für Spieler " + p.getName() + " ausgeführt (zurück in Main-World).");
            }
        }
    }

    // Hilfsmethoden
    private void resetPlayer(Player p) {
        p.setGameMode(configManager.getPlayerGameMode());
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
