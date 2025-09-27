package ch.ksrminecraft.murdermystery.managers.game;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.model.Role;
import ch.ksrminecraft.murdermystery.model.RoundStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class RoundResultManager {

    private final MurderMystery plugin;
    private final PointsManager pointsManager;

    public enum EndCondition { MURDERER_WIN, DETECTIVE_WIN, TIME_UP }

    public RoundResultManager(MurderMystery plugin, PointsManager pointsManager) {
        this.plugin = plugin;
        this.pointsManager = pointsManager;
    }

    public void handleRoundEnd(EndCondition condition,
                               Map<UUID, Role> roles,
                               RoundStats roundStats) {

        Map<UUID, Integer> roundPoints = new HashMap<>();

        for (UUID uuid : roles.keySet()) {
            Role role = roles.get(uuid);
            int total = 0;
            List<String> details = new ArrayList<>();

            // --- Rollen-spezifisch ---
            switch (role) {
                case MURDERER -> {
                    int kills = roundStats.getKills(uuid);
                    int killPoints = kills * plugin.getConfigManager().getPointsKillAsMurderer();

                    if (condition == EndCondition.MURDERER_WIN) {
                        total += plugin.getConfigManager().getPointsWin();
                        details.add("Spielausgang:  Gewonnen -> " + plugin.getConfigManager().getPointsWin());
                        if (kills > 0) {
                            total += killPoints;
                            details.add("Kills: " + kills + " -> " + killPoints);
                        }
                    } else if (condition == EndCondition.DETECTIVE_WIN) {
                        details.add("Spielausgang:  Verloren");
                        if (kills > 0) {
                            total += killPoints;
                            details.add("Kills: " + kills + " -> " + killPoints);
                        } else {
                            int consolation = plugin.getConfigManager().getPointsConsolation();
                            total += consolation;
                            details.add("Trostpunkte: " + consolation);
                        }
                    } else if (condition == EndCondition.TIME_UP) {
                        int tie = plugin.getConfigManager().getPointsTimeUp();
                        total += tie;
                        details.add("Spielausgang:  Unentschieden -> " + tie);
                        if (kills > 0) {
                            total += killPoints;
                            details.add("Kills: " + kills + " -> " + killPoints);
                        }
                    }
                }

                case DETECTIVE -> {
                    if (condition == EndCondition.DETECTIVE_WIN) {
                        if (roundStats.hasSurvived(uuid)) {
                            // Fall 1: Detective gewinnt & lebt
                            total += plugin.getConfigManager().getPointsWin();
                            details.add("Spielausgang:  Gewonnen -> " + plugin.getConfigManager().getPointsWin());

                            total += plugin.getConfigManager().getPointsKillMurderer();
                            details.add("Mörder erwischt -> " + plugin.getConfigManager().getPointsKillMurderer());
                        } else {
                            // Fall 2: Detective stirbt (Rollenwechsel)
                            total += plugin.getConfigManager().getPointsCoWin();
                            details.add("Spielausgang:  Gewonnen -> " + plugin.getConfigManager().getPointsCoWin() + " (co-win)");
                        }
                    } else if (condition == EndCondition.MURDERER_WIN) {
                        // Fall 3: Murderer gewinnt
                        details.add("Spielausgang:  Verloren");
                        int consolation = plugin.getConfigManager().getPointsConsolation();
                        total += consolation;
                        details.add("Trostpunkte: " + consolation);
                    } else if (condition == EndCondition.TIME_UP) {
                        int tie = plugin.getConfigManager().getPointsTimeUp();
                        total += tie;
                        details.add("Spielausgang:  Unentschieden -> " + tie);

                        if (roundStats.hasSurvived(uuid)) {
                            total += plugin.getConfigManager().getPointsSurvive();
                            details.add("Überlebt: " + plugin.getConfigManager().getPointsSurvive());
                        } else {
                            details.add("Überlebt: nein");
                        }
                    }

                    // Fehlabschüsse gelten in allen Fällen
                    int fails = roundStats.getDetectiveInnocentKills(uuid);
                    if (fails > 0) {
                        int penalty = fails * plugin.getConfigManager().getPointsKillInnocent();
                        total += penalty; // kann negativ sein, wird unten auf ≥0 gekappt
                        details.add("Fehlabschüsse: " + fails + " -> -" + Math.abs(penalty));
                    }
                }

                case BYSTANDER -> {
                    if (condition == EndCondition.DETECTIVE_WIN) {
                        if (roundStats.hasSurvived(uuid)) {
                            total += plugin.getConfigManager().getPointsCoWin();
                            details.add("Spielausgang:  Sieg -> " + plugin.getConfigManager().getPointsCoWin());

                            total += plugin.getConfigManager().getPointsSurvive();
                            details.add("Überlebt: ja -> " + plugin.getConfigManager().getPointsSurvive());
                        } else {
                            total += plugin.getConfigManager().getPointsWin();
                            details.add("Spielausgang:  Gewonnen -> " + plugin.getConfigManager().getPointsWin());
                            details.add("Überlebt: nein");
                        }
                    } else if (condition == EndCondition.MURDERER_WIN) {
                        details.add("Spielausgang:  Verloren");
                        if (!roundStats.hasSurvived(uuid)) {
                            int consolation = plugin.getConfigManager().getPointsConsolation();
                            total += consolation;
                            details.add("Überlebt: nein");
                            details.add("Trostpunkte: " + consolation);
                        } else {
                            details.add("Überlebt: ja");
                            details.add("→ keine Punkte");
                        }
                    } else if (condition == EndCondition.TIME_UP) {
                        int tie = plugin.getConfigManager().getPointsTimeUp();
                        total += tie;
                        details.add("Spielausgang:  Unentschieden -> " + tie);
                        if (roundStats.hasSurvived(uuid)) {
                            total += plugin.getConfigManager().getPointsSurvive();
                            details.add("Überlebt: ja -> " + plugin.getConfigManager().getPointsSurvive());
                        } else {
                            details.add("Überlebt: nein");
                        }
                    }
                }
            }

            // keine negativen Ergebnisse
            total = Math.max(0, total);
            roundPoints.put(uuid, total);

            // --- Nachricht an Spieler ---
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage("§e===== Deine Runde =====");
                p.sendMessage("§7Rolle: " + role);
                for (String d : details) p.sendMessage("§7" + d);
                p.sendMessage("§7Rundenpunkte: §b" + total);
                int newTotal = pointsManager.getPoints(uuid) + total;
                p.sendMessage("§7Neuer Punktestand: §a" + newTotal);
                p.sendMessage("§e=====================");
            }
        }

        // Punkte gutschreiben
        for (UUID uuid : roundPoints.keySet()) {
            pointsManager.addPointsToPlayer(uuid, roundPoints.get(uuid));
        }

        // --- Detaillierte Gesamtübersicht ---
        Map<UUID, String> nameCache = new HashMap<>();
        for (UUID id : roles.keySet()) {
            String name = Bukkit.getOfflinePlayer(id).getName();
            if (name != null) nameCache.put(id, name);
        }

        Bukkit.broadcastMessage(roundStats.formatSummary(nameCache, roles));
    }
}
