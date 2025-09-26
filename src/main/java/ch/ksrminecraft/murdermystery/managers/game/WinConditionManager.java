package ch.ksrminecraft.murdermystery.managers.game;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.effects.Broadcaster;
import ch.ksrminecraft.murdermystery.managers.effects.CelebrationManager;
import ch.ksrminecraft.murdermystery.model.Role;
import ch.ksrminecraft.murdermystery.model.RoundStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WinConditionManager {

    private final GameManager gameManager;
    private final PointsManager pointsManager;
    private final MurderMystery plugin;
    private final CelebrationManager celebrationManager;

    public WinConditionManager(GameManager gameManager, PointsManager pointsManager, MurderMystery plugin) {
        this.gameManager = gameManager;
        this.pointsManager = pointsManager;
        this.plugin = plugin;
        this.celebrationManager = new CelebrationManager(plugin);
    }

    public void checkWinConditions(Set<UUID> players,
                                   Map<UUID, Role> roles,
                                   int punkteGewinner,
                                   int punkteMitGewinner,
                                   int punkteVerlierer,
                                   RoundStats stats) {

        plugin.debug("WinCondition Check gestartet. Alive=" + players.size());

        boolean murdererAlive = roles.entrySet().stream()
                .anyMatch(e -> e.getValue() == Role.MURDERER && players.contains(e.getKey()));

        if (!murdererAlive) {
            handleMurdererDead(players, roles, punkteGewinner, punkteMitGewinner, punkteVerlierer, stats);
            endRoundWithStats(stats, roles);
            return;
        }

        boolean onlyMurdererLeft = (players.size() == 1 &&
                roles.get(players.iterator().next()) == Role.MURDERER);

        if (onlyMurdererLeft) {
            handleOnlyMurdererAlive(players, punkteGewinner, punkteVerlierer, stats);
            endRoundWithStats(stats, roles);
        }
    }

    public void forceTimeoutEnd(Set<UUID> players,
                                Map<UUID, Role> roles,
                                int punkteMitGewinner,
                                int punkteVerlierer,
                                RoundStats stats) {
        plugin.debug("Timeout-Ende: Bystander + Detective gewinnen, Murderer verliert.");

        Broadcaster.broadcastMessage(gameManager.getPlayers(),
                ChatColor.RED + "⏰ Zeitlimit erreicht! Die Bystander haben gewonnen!");

        for (UUID uuid : players) {
            Role role = roles.get(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (role == Role.BYSTANDER || role == Role.DETECTIVE) {
                stats.addPoints(uuid, punkteMitGewinner);
                stats.markSurvived(uuid);
                handleWinner(p);
            } else if (role == Role.MURDERER) {
                stats.addPoints(uuid, punkteVerlierer);
                handleLoser(p);
            }
        }

        endRoundWithStats(stats, roles);
    }

    // -------------------- Rundenabschluss mit Statistik --------------------

    private void endRoundWithStats(RoundStats stats, Map<UUID, Role> roles) {
        plugin.debug("Runde beendet → Statistiken werden ausgegeben.");

        // Verteile Punkte + Statistiken
        pointsManager.distributeRoundPoints(stats, roles);

        // Debug: vollständige Statistik ins Log
        if (plugin.isDebugEnabled()) {
            Map<UUID, String> nameCache = new HashMap<>();
            for (UUID uuid : stats.getAllPlayers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    nameCache.put(uuid, p.getName());
                } else {
                    String lastName = Bukkit.getOfflinePlayer(uuid).getName();
                    nameCache.put(uuid, lastName != null ? lastName : uuid.toString().substring(0, 8));
                }
            }
            plugin.getLogger().info("===== RUNDENSTATISTIK (DEBUG) =====");
            plugin.getLogger().info("\n" + stats.formatSummary(nameCache));
        }

        // Danach Runde beenden
        gameManager.endRound();
    }

    // -------------------- Win-Logik --------------------

    private void handleMurdererDead(Set<UUID> alive,
                                    Map<UUID, Role> roles,
                                    int punkteGewinner,
                                    int punkteMitGewinner,
                                    int punkteVerlierer,
                                    RoundStats stats) {
        Broadcaster.broadcastMessage(gameManager.getPlayers(),
                ChatColor.AQUA + "✅ Die Bystander haben gewonnen!");

        for (UUID uuid : alive) {
            Role role = roles.get(uuid);
            Player p = Bukkit.getPlayer(uuid);

            if (role == Role.BYSTANDER || role == Role.DETECTIVE) {
                stats.addPoints(uuid, punkteMitGewinner);
                stats.markSurvived(uuid);
                handleWinner(p);
            }
        }

        // Murderer verliert
        UUID murdererUuid = roles.entrySet().stream()
                .filter(e -> e.getValue() == Role.MURDERER)
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);

        if (murdererUuid != null) {
            stats.addPoints(murdererUuid, punkteVerlierer);
            handleLoser(Bukkit.getPlayer(murdererUuid));
        }
    }

    private void handleOnlyMurdererAlive(Set<UUID> alive,
                                         int punkteGewinner,
                                         int punkteVerlierer,
                                         RoundStats stats) {
        Broadcaster.broadcastMessage(gameManager.getPlayers(),
                ChatColor.DARK_RED + "🔪 Der Murderer hat gewonnen!");

        UUID murdererUuid = alive.iterator().next();
        stats.addPoints(murdererUuid, punkteGewinner);
        stats.markSurvived(murdererUuid);

        handleWinner(Bukkit.getPlayer(murdererUuid));

        for (UUID uuid : gameManager.getPlayers()) {
            if (uuid.equals(murdererUuid)) continue;
            stats.addPoints(uuid, punkteVerlierer);
            handleLoser(Bukkit.getPlayer(uuid));
        }
    }

    // -------------------- Hilfsmethoden --------------------

    private void handleWinner(Player p) {
        if (p != null && p.isOnline()) {
            p.sendTitle(ChatColor.GREEN + "🎉 Sieg!", ChatColor.AQUA + "Gut gemacht!", 20, 80, 20);
            p.sendMessage(ChatColor.GREEN + "🎉 Du hast gewonnen!");
            celebrationManager.launchFireworks(p);
        }
    }

    private void handleLoser(Player p) {
        if (p != null && p.isOnline()) {
            p.sendTitle(ChatColor.RED + "❌ Niederlage!", ChatColor.GRAY + "Du hast verloren!", 20, 80, 20);
            p.sendMessage(ChatColor.RED + "❌ Du hast verloren!");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}
