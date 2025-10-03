package ch.ksrminecraft.murdermystery.managers.game;

import ch.ksrminecraft.murdermystery.managers.effects.CelebrationManager;
import ch.ksrminecraft.murdermystery.model.ArenaGame;
import ch.ksrminecraft.murdermystery.model.Role;
import ch.ksrminecraft.murdermystery.model.RoundStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Berechnet Punkte & Nachrichten am Ende einer Runde.
 */
public class RoundResultManager {

    private final ArenaGame gameManager;
    private final PointsManager pointsManager;
    private final CelebrationManager celebrationManager;

    public enum EndCondition { MURDERER_WIN, DETECTIVE_WIN, TIME_UP }

    public RoundResultManager(ArenaGame gameManager, PointsManager pointsManager) {
        this.gameManager = gameManager;
        this.pointsManager = pointsManager;
        this.celebrationManager = new CelebrationManager(gameManager.getPlugin());
    }

    public void handleRoundEnd(EndCondition condition,
                               Map<UUID, Role> roles,
                               RoundStats roundStats) {

        Map<UUID, Integer> roundPoints = new HashMap<>();

        for (UUID uuid : roles.keySet()) {
            Role role = roles.get(uuid);
            int total = 0;
            List<String> details = new ArrayList<>();

            boolean survived = gameManager.getPlayers().contains(uuid);
            gameManager.getConfigManager().debug("Überlebensstatus für "
                    + Bukkit.getOfflinePlayer(uuid).getName() + ": " + survived);

            switch (role) {
                case MURDERER -> {
                    int kills = roundStats.getKills(uuid);
                    int killPoints = kills * gameManager.getConfigManager().getPointsKillAsMurderer();

                    if (condition == EndCondition.MURDERER_WIN) {
                        total += gameManager.getConfigManager().getPointsWin();
                        details.add(ChatColor.GRAY + "Spielausgang: "
                                + ChatColor.GREEN + "Gewonnen -> " + gameManager.getConfigManager().getPointsWin());
                        if (kills > 0) {
                            total += killPoints;
                            details.add(ChatColor.GRAY + "Kills: " + ChatColor.GREEN + kills + " -> " + killPoints);
                        }
                    } else if (condition == EndCondition.DETECTIVE_WIN) {
                        details.add(ChatColor.GRAY + "Spielausgang: " + ChatColor.RED + "Verloren");
                        if (kills > 0) {
                            total += killPoints;
                            details.add(ChatColor.GRAY + "Kills: " + ChatColor.GREEN + kills + " -> " + killPoints);
                        } else {
                            int consolation = gameManager.getConfigManager().getPointsConsolation();
                            total += consolation;
                            details.add(ChatColor.GRAY + "Trostpunkte: " + ChatColor.GREEN + consolation);
                        }
                    } else if (condition == EndCondition.TIME_UP) {
                        int tie = gameManager.getConfigManager().getPointsTimeUp();
                        total += tie;
                        details.add(ChatColor.GRAY + "Spielausgang: "
                                + ChatColor.YELLOW + "Unentschieden -> " + tie);
                        if (kills > 0) {
                            total += killPoints;
                            details.add(ChatColor.GRAY + "Kills: " + ChatColor.GREEN + kills + " -> " + killPoints);
                        }
                    }
                }

                case DETECTIVE -> {
                    if (condition == EndCondition.DETECTIVE_WIN) {
                        if (survived) {
                            // Sieg für aktuellen Detective
                            total += gameManager.getConfigManager().getPointsWin();
                            details.add(ChatColor.GRAY + "Spielausgang: "
                                    + ChatColor.GREEN + "Gewonnen -> " + gameManager.getConfigManager().getPointsWin());

                            // Hat dieser Detective den Murderer gekillt?
                            int kills = roundStats.getKills(uuid);
                            if (kills > 0) {
                                int bonus = gameManager.getConfigManager().getPointsKillMurderer();
                                total += bonus;
                                details.add(ChatColor.GRAY + "Mörder getötet: "
                                        + ChatColor.GREEN + "ja -> " + bonus);
                            } else {
                                details.add(ChatColor.GRAY + "Mörder getötet: " + ChatColor.RED + "nein");
                            }
                        } else {
                            // Detective tot → Co-Win, wie bisher
                            total += gameManager.getConfigManager().getPointsCoWin();
                            details.add(ChatColor.GRAY + "Spielausgang: "
                                    + ChatColor.GREEN + "Co-Gewinner -> " + gameManager.getConfigManager().getPointsCoWin());
                        }
                    } else if (condition == EndCondition.MURDERER_WIN) {
                        details.add(ChatColor.GRAY + "Spielausgang: " + ChatColor.RED + "Verloren");
                        int consolation = gameManager.getConfigManager().getPointsConsolation();
                        total += consolation;
                        details.add(ChatColor.GRAY + "Trostpunkte: " + ChatColor.GREEN + consolation);
                    } else if (condition == EndCondition.TIME_UP) {
                        int tie = gameManager.getConfigManager().getPointsTimeUp();
                        total += tie;
                        details.add(ChatColor.GRAY + "Spielausgang: "
                                + ChatColor.YELLOW + "Unentschieden -> " + tie);
                        if (survived) {
                            total += gameManager.getConfigManager().getPointsSurvive();
                            details.add(ChatColor.GRAY + "Überlebt: "
                                    + ChatColor.GREEN + "ja -> " + gameManager.getConfigManager().getPointsSurvive());
                        } else {
                            details.add(ChatColor.GRAY + "Überlebt: " + ChatColor.RED + "nein");
                        }
                    }

                    int fails = roundStats.getDetectiveInnocentKills(uuid);
                    if (fails > 0) {
                        int penalty = fails * gameManager.getConfigManager().getPointsKillInnocent();
                        total += penalty;
                        details.add(ChatColor.GRAY + "Fehlabschüsse: " + ChatColor.RED + fails + " -> -" + Math.abs(penalty));
                    }
                }

                case BYSTANDER -> {
                    if (condition == EndCondition.DETECTIVE_WIN) {
                        if (survived) {
                            total += gameManager.getConfigManager().getPointsCoWin();
                            details.add(ChatColor.GRAY + "Spielausgang: "
                                    + ChatColor.GREEN + "Co-Gewinner -> " + gameManager.getConfigManager().getPointsCoWin());
                            total += gameManager.getConfigManager().getPointsSurvive();
                            details.add(ChatColor.GRAY + "Überlebt: "
                                    + ChatColor.GREEN + "ja -> " + gameManager.getConfigManager().getPointsSurvive());
                        } else {
                            total += gameManager.getConfigManager().getPointsCoWin();
                            details.add(ChatColor.GRAY + "Spielausgang: "
                                    + ChatColor.GREEN + "Co-Gewinner -> " + gameManager.getConfigManager().getPointsCoWin());
                            details.add(ChatColor.GRAY + "Überlebt: " + ChatColor.RED + "nein");
                        }
                    } else if (condition == EndCondition.MURDERER_WIN) {
                        details.add(ChatColor.GRAY + "Spielausgang: " + ChatColor.RED + "Verloren");
                        if (!survived) {
                            int consolation = gameManager.getConfigManager().getPointsConsolation();
                            total += consolation;
                            details.add(ChatColor.GRAY + "Überlebt: " + ChatColor.RED + "nein");
                            details.add(ChatColor.GRAY + "Trostpunkte: " + ChatColor.GREEN + consolation);
                        } else {
                            details.add(ChatColor.GRAY + "Überlebt: " + ChatColor.GREEN + "ja");
                            details.add(ChatColor.GRAY + "→ keine Punkte");
                        }
                    } else if (condition == EndCondition.TIME_UP) {
                        int tie = gameManager.getConfigManager().getPointsTimeUp();
                        total += tie;
                        details.add(ChatColor.GRAY + "Spielausgang: " + ChatColor.YELLOW + "Unentschieden -> " + tie);
                        if (survived) {
                            total += gameManager.getConfigManager().getPointsSurvive();
                            details.add(ChatColor.GRAY + "Überlebt: "
                                    + ChatColor.GREEN + "ja -> " + gameManager.getConfigManager().getPointsSurvive());
                        } else {
                            details.add(ChatColor.GRAY + "Überlebt: " + ChatColor.RED + "nein");
                        }
                    }
                }
            }

            total = Math.max(0, total);
            roundPoints.put(uuid, total);

            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(ChatColor.YELLOW + "===== Deine Runde ========");
                p.sendMessage(ChatColor.GRAY + "Rolle: " + ChatColor.AQUA + role);
                for (String d : details) p.sendMessage(d);
                p.sendMessage(ChatColor.GRAY + "Rundenpunkte: " + ChatColor.AQUA + total);
                int newTotal = pointsManager.getPoints(uuid) + total;
                p.sendMessage(ChatColor.GRAY + "Neuer Punktestand: " + ChatColor.GREEN + newTotal);
                p.sendMessage(ChatColor.YELLOW + "========================");
            }
        }

        // Punkte gutschreiben
        for (UUID uuid : roundPoints.keySet()) {
            pointsManager.addPointsToPlayer(uuid, roundPoints.get(uuid));
        }

        // --- Rundenstatistik ---
        List<UUID> sorted = new ArrayList<>(roundPoints.keySet());
        sorted.sort((a, b) -> roundPoints.get(b) - roundPoints.get(a));

        Bukkit.broadcastMessage("§6===== Rundenstatistik (" + gameManager.getArena().getName() + ") =====");
        for (UUID id : sorted) {
            String name = Bukkit.getOfflinePlayer(id).getName();
            if (name == null) name = id.toString();

            int pts = roundPoints.get(id);
            ChatColor color;
            if (condition == EndCondition.TIME_UP) {
                color = ChatColor.YELLOW;
            } else {
                boolean winner = false;
                Role role = roles.get(id);
                if (role != null) {
                    winner = switch (condition) {
                        case MURDERER_WIN -> role == Role.MURDERER;
                        case DETECTIVE_WIN -> role == Role.DETECTIVE || role == Role.BYSTANDER;
                        case TIME_UP -> false;
                    };
                }
                color = winner ? ChatColor.GREEN : ChatColor.RED;
            }
            Bukkit.broadcastMessage(color + "• " + name + " | " + pts + " Punkte");
        }
        Bukkit.broadcastMessage("§6========================");

        // --- Celebration starten ---
        celebrationManager.startCelebration(roles.keySet(), condition, roles);
    }
}
