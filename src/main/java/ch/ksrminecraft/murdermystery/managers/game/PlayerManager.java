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
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PlayerManager {

    private final GameManager gameManager;
    private final MurderMystery plugin;
    private final MapManager mapManager;
    private final ConfigManager configManager;
    private final ArenaManager arenaManager;

    public PlayerManager(GameManager gameManager,
                         MurderMystery plugin,
                         ArenaManager arenaManager,
                         ConfigManager configManager) {
        this.gameManager = gameManager;
        this.plugin = plugin;
        this.mapManager = new MapManager(plugin, arenaManager);
        this.configManager = configManager;
        this.arenaManager = arenaManager;

        plugin.debug("[PlayerManager] Instanziiert für GameManager.");
    }

    // --- Join Handling ---
    public void handleJoin(Player player, Arena arena) {
        UUID uuid = player.getUniqueId();

        plugin.debug("[PlayerManager] handleJoin() → Spieler=" + player.getName() + ", Arena=" + arena.getName());

        // Wenn Spiel läuft → nur als Spectator zulassen
        if (gameManager.isGameStarted()) {
            mapManager.teleportToArenaLobby(player, arena);
            player.sendMessage(ChatColor.YELLOW + "In Arena '" + arena.getName() + "' läuft gerade eine Runde.");
            plugin.debug("[PlayerManager] Join blockiert → Spiel läuft bereits (Arena=" + arena.getName() + ")");
            gameManager.getBossBarManager().addPlayer(player, BossBarManager.Mode.GAME);
            return;
        }

        // Spieler registrieren
        if (gameManager.getPlayers().add(uuid)) {
            resetPlayer(player);
            mapManager.teleportToArenaLobby(player, arena);

            plugin.debug("[PlayerManager] Spieler " + player.getName() +
                    " erfolgreich zur Arena hinzugefügt. Spielerzahl=" + gameManager.getPlayers().size());

            gameManager.getBossBarManager().addPlayer(player, BossBarManager.Mode.LOBBY);

            int needed = gameManager.getMinPlayers() - gameManager.getPlayers().size();
            plugin.debug("[PlayerManager] Arena '" + arena.getName() + "' → noch benötigt: " + needed + " Spieler bis Start");

            if (needed > 0) {
                Broadcaster.broadcastMessage(gameManager.getPlayers(),
                        ChatColor.AQUA + player.getName() + ChatColor.GRAY +
                                " hat die Lobby von Arena '" + arena.getName() +
                                "' betreten. Es fehlen noch " + ChatColor.GOLD +
                                needed + ChatColor.GRAY + " Spieler.");
            } else {
                Broadcaster.broadcastMessage(gameManager.getPlayers(),
                        ChatColor.GREEN + "Mindestanzahl in Arena '" + arena.getName() + "' erreicht! Spiel startet bald...");
                plugin.debug("[PlayerManager] Mindestanzahl erreicht → Countdown startet.");
                gameManager.startCountdown();
            }

            SignListener.updateJoinSigns(plugin, plugin.getGameManagerRegistry());
        } else {
            player.sendMessage(ChatColor.GRAY + "Du bist schon in der Lobby dieser Arena.");
            plugin.debug("[PlayerManager] Spieler " + player.getName() + " war bereits in der Arena-Lobby.");
        }
    }

    // --- Leave Handling ---
    public void handleLeave(Player player, Arena arena) {
        UUID uuid = player.getUniqueId();
        Role role = RoleManager.getRole(uuid);

        plugin.debug("[PlayerManager] handleLeave() → Spieler=" + player.getName() +
                ", Arena=" + arena.getName() + ", Rolle=" + role);

        boolean wasPlayer = gameManager.getPlayers().remove(uuid);
        boolean wasSpectator = gameManager.getSpectators().remove(uuid);

        plugin.debug("[PlayerManager] Spielerstatus vor Leave → wasPlayer=" + wasPlayer +
                ", wasSpectator=" + wasSpectator);

        RoleManager.removePlayer(uuid);
        gameManager.getBossBarManager().removePlayer(player);

        // --- Fall 1: Spiel läuft ---
        if (wasPlayer && gameManager.isGameStarted()) {
            int penalty = Math.abs(configManager.getPointsQuit());
            gameManager.getPointsManager().applyPenalty(uuid, penalty, "Spiel verlassen");
            int newPoints = gameManager.getPointsManager().getPoints(uuid);

            player.sendMessage(ChatColor.RED + "Du hast die Arena '" + arena.getName() +
                    "' verlassen und -" + penalty + " Punkte erhalten.");
            player.sendMessage(ChatColor.GRAY + "Neuer Punktestand: " + ChatColor.GOLD + newPoints);
            plugin.debug("[PlayerManager] Strafpunkte vergeben an " + player.getName() + ": -" + penalty);

            if (role == Role.DETECTIVE) {
                player.getInventory().remove(Material.BOW);
                player.getInventory().remove(Material.ARROW);
                player.getWorld().dropItemNaturally(player.getLocation(), ItemManager.createDetectiveBow());
                player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.ARROW, 1));
                Broadcaster.broadcastMessage(gameManager.getPlayers(),
                        ChatColor.BLUE + "🔎 Der Detective hat die Arena verlassen!");
            } else if (role == Role.BYSTANDER) {
                Broadcaster.broadcastMessage(gameManager.getPlayers(),
                        ChatColor.YELLOW + "👤 Ein Unschuldiger hat die Arena verlassen!");
            } else if (role == Role.MURDERER) {
                Broadcaster.broadcastMessage(gameManager.getPlayers(),
                        ChatColor.DARK_RED + "🔪 Der Mörder hat die Arena verlassen!");
                mapManager.teleportToMainLobby(player);
                gameManager.endRound(RoundResultManager.EndCondition.DETECTIVE_WIN);
                return;
            }
        }
        // --- Fall 2: Lobby/Countdown ---
        else if (wasPlayer) {
            int needed = gameManager.getMinPlayers() - gameManager.getPlayers().size();
            Broadcaster.broadcastMessage(gameManager.getPlayers(),
                    ChatColor.AQUA + player.getName() + ChatColor.GRAY +
                            " hat die Lobby verlassen." +
                            (needed > 0 ? " Es fehlen noch " + ChatColor.GOLD + needed + ChatColor.GRAY + " Spieler." : ""));
            plugin.debug("[PlayerManager] Lobby-Leave von " + player.getName());
        }

        // Spieler zurück in MainLobby
        mapManager.teleportToMainLobby(player);

        // Signs updaten
        SignListener.updateJoinSigns(plugin, plugin.getGameManagerRegistry());

        // Arena resetten, wenn leer
        if (gameManager.getPlayers().isEmpty() && gameManager.getSpectators().isEmpty()) {
            plugin.debug("[PlayerManager] Arena '" + arena.getName() + "' ist leer → resetGame().");
            gameManager.resetGame();
        } else {
            gameManager.checkWinConditions();
        }
    }

    // --- Spieler zurücksetzen ---
    public void resetPlayer(Player p) {
        p.setGameMode(configManager.getPlayerGameMode());
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.setSaturation(20);
        p.setFireTicks(0);
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        p.getInventory().setItemInOffHand(null);
        ItemManager.clearSpecialItems(p);

        plugin.debug("[PlayerManager] Spieler " + p.getName() +
                " wurde zurückgesetzt (Gamemode=" + p.getGameMode() + ")");
    }

    public MapManager getMapManager() {
        return mapManager;
    }
}
