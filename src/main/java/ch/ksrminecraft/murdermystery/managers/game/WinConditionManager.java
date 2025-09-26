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

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WinConditionManager {

    private final GameManager gameManager;
    private final MurderMystery plugin;
    private final CelebrationManager celebrationManager;

    public WinConditionManager(GameManager gameManager, PointsManager pointsManager, MurderMystery plugin) {
        this.gameManager = gameManager;
        this.plugin = plugin;
        this.celebrationManager = new CelebrationManager(plugin);
    }

    public void checkWinConditions(Set<UUID> players,
                                   Map<UUID, Role> roles,
                                   RoundStats stats) {

        plugin.debug("WinCondition Check gestartet. Alive=" + players.size());

        boolean murdererAlive = roles.entrySet().stream()
                .anyMatch(e -> e.getValue() == Role.MURDERER && players.contains(e.getKey()));

        // === Fall: Murderer tot → Detective/Bystander gewinnen ===
        if (!murdererAlive) {
            Broadcaster.broadcastMessage(gameManager.getPlayers(),
                    ChatColor.AQUA + "✅ Die Bystander haben gewonnen!");
            gameManager.endRound(RoundResultManager.EndCondition.DETECTIVE_WIN);
            return;
        }

        // === Fall: nur noch Murderer lebt → Murderer gewinnt ===
        boolean onlyMurdererLeft = (players.size() == 1 &&
                roles.get(players.iterator().next()) == Role.MURDERER);

        if (onlyMurdererLeft) {
            Broadcaster.broadcastMessage(gameManager.getPlayers(),
                    ChatColor.DARK_RED + "🔪 Der Murderer hat gewonnen!");
            gameManager.endRound(RoundResultManager.EndCondition.MURDERER_WIN);
        }
    }

    public void forceTimeoutEnd(Set<UUID> players,
                                Map<UUID, Role> roles,
                                RoundStats stats) {
        plugin.debug("Timeout-Ende: Zeit abgelaufen, niemand gewinnt.");

        Broadcaster.broadcastMessage(gameManager.getPlayers(),
                ChatColor.YELLOW + "⏰ Zeitlimit erreicht! Niemand hat gewonnen.");

        gameManager.endRound(RoundResultManager.EndCondition.TIME_UP);
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
