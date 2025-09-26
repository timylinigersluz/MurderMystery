package ch.ksrminecraft.murdermystery.managers.game;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.model.Role;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
                               Map<UUID, Integer> kills,
                               Set<UUID> survivors,
                               Set<UUID> quitters) {

        Map<UUID, Integer> roundPoints = new HashMap<>();

        switch (condition) {
            case MURDERER_WIN -> distributeMurdererWin(roundPoints, roles, kills, quitters);
            case DETECTIVE_WIN -> distributeDetectiveWin(roundPoints, roles, kills, survivors, quitters);
            case TIME_UP -> distributeTimeUp(roundPoints, roles, kills, survivors, quitters);
        }

        for (UUID uuid : roundPoints.keySet()) {
            int pts = Math.max(0, roundPoints.get(uuid));
            if (!quitters.contains(uuid)) {
                pointsManager.addPointsToPlayer(uuid, pts);
            }
        }

        for (UUID uuid : roles.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                int points = roundPoints.getOrDefault(uuid, 0);
                int newTotal = pointsManager.getPoints(uuid);

                p.sendMessage("§e===== Deine Runde =====");
                p.sendMessage("§7Rolle: " + roles.get(uuid));
                p.sendMessage("§7Kills: §c" + kills.getOrDefault(uuid, 0));
                p.sendMessage("§7Überlebt: " + (survivors.contains(uuid) ? "§aJa" : "§cNein"));
                if (quitters.contains(uuid)) {
                    p.sendMessage("§7Status: §eVorzeitig verlassen");
                }
                p.sendMessage("§7Rundenpunkte: §b" + points);
                p.sendMessage("§7Neuer Punktestand: §a" + newTotal);
                p.sendMessage("§e=====================");
            }
        }

        broadcastSummary(roundPoints, roles, condition);
    }

    private void distributeMurdererWin(Map<UUID, Integer> points,
                                       Map<UUID, Role> roles,
                                       Map<UUID, Integer> kills,
                                       Set<UUID> quitters) {
        for (UUID uuid : roles.keySet()) {
            Role role = roles.get(uuid);
            int base = 0;

            switch (role) {
                case MURDERER -> {
                    base += kills.getOrDefault(uuid, 0) * plugin.getConfigManager().getPointsKillAsMurderer();
                    base += plugin.getConfigManager().getPointsWin();
                }
                case DETECTIVE -> {
                    base += plugin.getConfigManager().getPointsLose();
                    if (plugin.getGameManager().didDetectiveKillInnocent(uuid)) {
                        base -= plugin.getConfigManager().getPointsKillInnocent();
                    }
                }
                case BYSTANDER -> base += plugin.getConfigManager().getPointsLose();
            }

            if (quitters.contains(uuid)) base = 0;
            points.put(uuid, base);
        }
    }

    private void distributeDetectiveWin(Map<UUID, Integer> points,
                                        Map<UUID, Role> roles,
                                        Map<UUID, Integer> kills,
                                        Set<UUID> survivors,
                                        Set<UUID> quitters) {
        for (UUID uuid : roles.keySet()) {
            Role role = roles.get(uuid);
            int base = 0;

            switch (role) {
                case MURDERER -> {
                    base += plugin.getConfigManager().getPointsLose();
                    base += kills.getOrDefault(uuid, 0) * plugin.getConfigManager().getPointsKillAsMurderer();
                }
                case DETECTIVE -> {
                    base += plugin.getConfigManager().getPointsWin();
                    base += plugin.getConfigManager().getPointsKillMurderer();
                    if (plugin.getGameManager().didDetectiveKillInnocent(uuid)) {
                        base -= plugin.getConfigManager().getPointsKillInnocent();
                    }
                }
                case BYSTANDER -> {
                    if (survivors.contains(uuid)) {
                        base += plugin.getConfigManager().getPointsCoWin();
                        base += plugin.getConfigManager().getPointsSurvive();
                    } else {
                        base += plugin.getConfigManager().getPointsCoWin();
                    }
                }
            }

            if (quitters.contains(uuid)) base = 0;
            points.put(uuid, base);
        }
    }

    private void distributeTimeUp(Map<UUID, Integer> points,
                                  Map<UUID, Role> roles,
                                  Map<UUID, Integer> kills,
                                  Set<UUID> survivors,
                                  Set<UUID> quitters) {
        for (UUID uuid : roles.keySet()) {
            Role role = roles.get(uuid);
            int base = 0;

            switch (role) {
                case MURDERER -> {
                    base += kills.getOrDefault(uuid, 0) * plugin.getConfigManager().getPointsKillAsMurderer();
                    if (survivors.contains(uuid)) base += plugin.getConfigManager().getPointsSurvive();
                }
                case DETECTIVE -> {
                    if (survivors.contains(uuid)) base += plugin.getConfigManager().getPointsSurvive();
                    if (plugin.getGameManager().didDetectiveKillInnocent(uuid)) {
                        base -= plugin.getConfigManager().getPointsKillInnocent();
                    }
                }
                case BYSTANDER -> {
                    if (survivors.contains(uuid)) {
                        base += plugin.getConfigManager().getPointsSurvive();
                    } else {
                        base += plugin.getConfigManager().getPointsLose();
                    }
                }
            }

            if (quitters.contains(uuid)) base = 0;
            points.put(uuid, base);
        }
    }

    private void broadcastSummary(Map<UUID, Integer> roundPoints,
                                  Map<UUID, Role> roles,
                                  EndCondition condition) {

        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(roundPoints.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        ChatColor color = (condition == EndCondition.TIME_UP ? ChatColor.YELLOW :
                (condition == EndCondition.MURDERER_WIN ? ChatColor.RED : ChatColor.GREEN));

        Bukkit.broadcastMessage("§6===== Rundenstatistik =====");
        for (Map.Entry<UUID, Integer> entry : sorted) {
            UUID uuid = entry.getKey();
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            int pts = entry.getValue();
            Bukkit.broadcastMessage(color + name + " §7→ §b" + pts + " Punkte");
        }
        Bukkit.broadcastMessage("§6===========================");
    }
}
