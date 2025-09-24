package ch.ksrminecraft.murdermystery.utils;

import ch.ksrminecraft.murdermystery.MurderMystery;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WinConditionChecker {

    private final GameManager gameManager;
    private final PointsManager pointsManager;
    private final MurderMystery plugin;

    public WinConditionChecker(GameManager gameManager, PointsManager pointsManager, MurderMystery plugin) {
        this.gameManager = gameManager;
        this.pointsManager = pointsManager;
        this.plugin = plugin;
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
            gameManager.endRound();
            return;
        }

        boolean onlyMurdererLeft = (players.size() == 1 &&
                roles.get(players.iterator().next()) == Role.MURDERER);

        if (onlyMurdererLeft) {
            handleOnlyMurdererAlive(players, punkteGewinner, punkteVerlierer, stats);
            gameManager.endRound();
        }
    }

    public void forceTimeoutEnd(Set<UUID> players,
                                Map<UUID, Role> roles,
                                int punkteMitGewinner,
                                int punkteVerlierer,
                                RoundStats stats) {
        plugin.debug("Timeout-Ende: Bystander + Detective gewinnen, Murderer verliert.");

        for (UUID uuid : players) {
            Role role = roles.get(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (role == Role.BYSTANDER || role == Role.DETECTIVE) {
                stats.addPoints(uuid, punkteMitGewinner);
                stats.markSurvived(uuid);
                sendTitle(p, ChatColor.GREEN + "🎉 Sieg!", ChatColor.AQUA + "Du hast überlebt.");
            } else if (role == Role.MURDERER) {
                stats.addPoints(uuid, punkteVerlierer);
                sendTitle(p, ChatColor.RED + "❌ Niederlage!", ChatColor.DARK_RED + "Die Zeit ist abgelaufen.");
            }
        }

        broadcast(ChatColor.RED + "Zeitlimit erreicht! Die Bystander haben gewonnen.");
    }

    private void handleMurdererDead(Set<UUID> alive,
                                    Map<UUID, Role> roles,
                                    int punkteGewinner,
                                    int punkteMitGewinner,
                                    int punkteVerlierer,
                                    RoundStats stats) {
        // Prüfe den aktuellen GameMode
        if ("bow-fallback".equalsIgnoreCase(gameManager.getGameMode())) {
            broadcast(ChatColor.YELLOW + "Der Detective ist gestorben! Sein Bogen kann aufgenommen werden.");
            return;
        }

        // === Classic-Modus → Spiel endet sofort ===
        Player detective = RoleManager.getDetective();

        if (plugin.isMurdererKilledByBow()) {
            if (detective != null) {
                stats.addPoints(detective.getUniqueId(), punkteGewinner);
                stats.markSurvived(detective.getUniqueId());
                sendTitle(detective, ChatColor.GREEN + "🎉 Sieg!", ChatColor.AQUA + "Du hast den Murderer getötet!");
            }
            broadcast(ChatColor.AQUA + "Der Detective hat gewonnen!");
        } else {
            if (detective != null) {
                stats.addPoints(detective.getUniqueId(), punkteMitGewinner);
                stats.markSurvived(detective.getUniqueId());
                sendTitle(detective, ChatColor.GREEN + "🎉 Sieg!", ChatColor.AQUA + "Du hast überlebt.");
            }
            broadcast(ChatColor.AQUA + "Die Bystander haben gewonnen!");
        }

        for (UUID uuid : alive) {
            if (roles.get(uuid) == Role.BYSTANDER) {
                stats.addPoints(uuid, punkteMitGewinner);
                stats.markSurvived(uuid);
                Player p = Bukkit.getPlayer(uuid);
                sendTitle(p, ChatColor.GREEN + "🎉 Sieg!", ChatColor.AQUA + "Ihr habt den Murderer besiegt!");
            }
        }

        UUID murdererUuid = roles.entrySet().stream()
                .filter(e -> e.getValue() == Role.MURDERER)
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);

        if (murdererUuid != null) {
            stats.addPoints(murdererUuid, punkteVerlierer);
            Player murderer = Bukkit.getPlayer(murdererUuid);
            sendTitle(murderer, ChatColor.RED + "❌ Niederlage!", ChatColor.DARK_RED + "Du wurdest besiegt.");
        }
    }

    private void handleOnlyMurdererAlive(Set<UUID> alive,
                                         int punkteGewinner,
                                         int punkteVerlierer,
                                         RoundStats stats) {
        UUID murdererUuid = alive.iterator().next();
        stats.addPoints(murdererUuid, punkteGewinner);
        stats.markSurvived(murdererUuid);

        Player murderer = Bukkit.getPlayer(murdererUuid);
        if (murderer != null) {
            sendTitle(murderer, ChatColor.GREEN + "🎉 Sieg!", ChatColor.DARK_RED + "Du hast alle getötet!");
        }

        broadcast(ChatColor.DARK_RED + "Der Murderer hat gewonnen!");

        for (UUID uuid : gameManager.getPlayers()) {
            if (RoleManager.getRole(uuid) == Role.BYSTANDER || RoleManager.getRole(uuid) == Role.DETECTIVE) {
                stats.addPoints(uuid, punkteVerlierer);
                Player p = Bukkit.getPlayer(uuid);
                sendTitle(p, ChatColor.RED + "❌ Niederlage!", ChatColor.GRAY + "Der Murderer hat überlebt.");
            }
        }
    }

    private void broadcast(String msg) {
        for (UUID uuid : gameManager.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(msg);
            }
        }
    }

    private void sendTitle(Player p, String title, String subtitle) {
        if (p != null && p.isOnline()) {
            p.sendTitle(title, subtitle, 20, 80, 20);
        }
    }
}
