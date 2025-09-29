package ch.ksrminecraft.murdermystery.managers.game;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.effects.Broadcaster;
import ch.ksrminecraft.murdermystery.model.Role;
import ch.ksrminecraft.murdermystery.model.RoundStats;
import org.bukkit.ChatColor;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WinConditionManager {

    private final GameManager gameManager;
    private final MurderMystery plugin;

    public WinConditionManager(GameManager gameManager, PointsManager pointsManager, MurderMystery plugin) {
        this.gameManager = gameManager;
        this.plugin = plugin;
    }

    /**
     * Prüft die aktuellen Siegbedingungen.
     * Gibt eine EndCondition zurück, falls das Spiel beendet ist.
     * Gibt null zurück, wenn das Spiel weiterläuft.
     */
    public RoundResultManager.EndCondition checkWinConditions(Set<UUID> players,
                                                              Map<UUID, Role> roles,
                                                              RoundStats stats) {

        // Zähle lebende Rollen
        int aliveMurderers = 0;
        int aliveDetectives = 0;
        int aliveBystanders = 0;

        for (Map.Entry<UUID, Role> e : roles.entrySet()) {
            if (!players.contains(e.getKey())) continue;
            switch (e.getValue()) {
                case MURDERER -> aliveMurderers++;
                case DETECTIVE -> aliveDetectives++;
                case BYSTANDER -> aliveBystanders++;
            }
        }

        plugin.debug("WinCondition: alive M=" + aliveMurderers +
                ", D=" + aliveDetectives + ", B=" + aliveBystanders);

        // === Murderer tot → Detective/Bystander gewinnen ===
        if (aliveMurderers == 0) {
            Broadcaster.broadcastMessage(gameManager.getPlayers(),
                    ChatColor.AQUA + "✅ Die Bystander haben gewonnen!");
            return RoundResultManager.EndCondition.DETECTIVE_WIN;
        }

        // === Alle Unschuldigen (Bystander) tot und mindestens ein Murderer lebt → Murderer gewinnt ===
        // Deckt sowohl "nur Murderer lebt" als auch "Murderer + Detective leben" ab.
        if (aliveBystanders == 0 && aliveMurderers > 0) {
            Broadcaster.broadcastMessage(gameManager.getPlayers(),
                    ChatColor.DARK_RED + "🔪 Alle Unschuldigen sind tot! Der Murderer hat gewonnen!");
            return RoundResultManager.EndCondition.MURDERER_WIN;
        }

        // Spiel geht weiter
        return null;
    }

    /**
     * Timeout-Ende erzwingen → immer Unentschieden.
     */
    public RoundResultManager.EndCondition forceTimeoutEnd(Set<UUID> players,
                                                           Map<UUID, Role> roles,
                                                           RoundStats stats) {
        plugin.debug("Timeout-Ende: Zeit abgelaufen, niemand gewinnt.");

        Broadcaster.broadcastMessage(gameManager.getPlayers(),
                ChatColor.YELLOW + "⏰ Zeitlimit erreicht! Niemand hat gewonnen.");

        return RoundResultManager.EndCondition.TIME_UP;
    }
}
