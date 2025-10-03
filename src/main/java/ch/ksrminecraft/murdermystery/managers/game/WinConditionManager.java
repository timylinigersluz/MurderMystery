package ch.ksrminecraft.murdermystery.managers.game;

import ch.ksrminecraft.murdermystery.managers.effects.Broadcaster;
import ch.ksrminecraft.murdermystery.model.ArenaGame;
import ch.ksrminecraft.murdermystery.model.Role;
import ch.ksrminecraft.murdermystery.model.RoundStats;
import org.bukkit.ChatColor;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Prüft Siegbedingungen arena-spezifisch.
 */
public class WinConditionManager {

    private final ArenaGame game;

    public WinConditionManager(ArenaGame game) {
        this.game = game;
    }

    /**
     * Prüft die aktuellen Siegbedingungen.
     * @return EndCondition oder null wenn Spiel weiterläuft
     */
    public RoundResultManager.EndCondition checkWinConditions(Set<UUID> players,
                                                              Map<UUID, Role> roles,
                                                              RoundStats stats) {
        // Sicherstellen: Nur prüfen, wenn Spiel läuft
        if (!game.isGameStarted()) {
            game.getConfigManager().debug("[WinCondition] Abgebrochen → Spiel läuft nicht (Lobby/Countdown).");
            return null;
        }

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

        game.getConfigManager().debug("[WinCondition]: M=" + aliveMurderers +
                ", D=" + aliveDetectives + ", B=" + aliveBystanders);

        // Murderer tot → Detective/Bystander gewinnen
        if (aliveMurderers == 0) {
            Broadcaster.broadcastMessage(game.getPlayers(),
                    ChatColor.AQUA + "✅ Die Bystander haben gewonnen!");
            return RoundResultManager.EndCondition.DETECTIVE_WIN;
        }

        // Alle Innocents (Detectives + Bystanders) tot → Murderer gewinnt
        if (aliveDetectives == 0 && aliveBystanders == 0 && aliveMurderers > 0) {
            Broadcaster.broadcastMessage(game.getPlayers(),
                    ChatColor.DARK_RED + "🔪 Alle Unschuldigen sind tot! Der Murderer hat gewonnen!");
            return RoundResultManager.EndCondition.MURDERER_WIN;
        }

        // Wenn nur noch Murderer + Detective leben → Murderer gewinnt
        if (aliveMurderers > 0 && aliveBystanders == 0 && aliveDetectives == 1) {
            Broadcaster.broadcastMessage(game.getPlayers(),
                    ChatColor.DARK_RED + "🔪 Keine Unschuldigen mehr am Leben! Der Murderer gewinnt!");
            return RoundResultManager.EndCondition.MURDERER_WIN;
        }

        // Noch kein Ende → Spiel läuft weiter
        return null;
    }

    /**
     * Timeout-Ende → immer Unentschieden
     */
    public RoundResultManager.EndCondition forceTimeoutEnd(Set<UUID> players,
                                                           Map<UUID, Role> roles,
                                                           RoundStats stats) {
        game.getConfigManager().debug("Timeout-Ende: Zeit abgelaufen, niemand gewinnt.");

        Broadcaster.broadcastMessage(game.getPlayers(),
                ChatColor.YELLOW + "⏰ Zeitlimit erreicht! Niemand hat gewonnen.");

        return RoundResultManager.EndCondition.TIME_UP;
    }
}
