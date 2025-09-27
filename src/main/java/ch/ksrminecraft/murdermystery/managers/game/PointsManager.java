package ch.ksrminecraft.murdermystery.managers.game;

import ch.ksrminecraft.RankPointsAPI.PointsAPI;
import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.model.Role;
import ch.ksrminecraft.murdermystery.model.RoundStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class PointsManager {

    private final PointsAPI api;
    private final Logger logger;
    private final MurderMystery plugin;

    public PointsManager(Logger logger, MurderMystery plugin) {
        this.logger = logger;
        this.plugin = plugin;

        // Daten aus Config laden
        String url = plugin.getConfig().getString("Rank-Points-API-url");
        String user = plugin.getConfig().getString("Rank-Points-API-user");
        String pass = plugin.getConfig().getString("Rank-Points-API-password");

        boolean debug = plugin.getConfig().getBoolean("rankpoints.debug", false);
        boolean excludeStaff = plugin.getConfig().getBoolean("rankpoints.exclude-staff", true);

        if (url == null || user == null || pass == null) {
            logger.severe("Fehlende Konfigurationswerte für RankPointsAPI! Bitte config.yml prüfen.");
            throw new IllegalStateException("Config-Werte für RankPointsAPI unvollständig!");
        }

        // PointsAPI initialisieren
        this.api = new PointsAPI(url, user, pass, logger, debug, excludeStaff);
    }

    /**
     * Punkte einer ganzen Runde verteilen + Statistik ausgeben.
     * (Wird später durch RoundResultManager ersetzt, bleibt aber als Fallback erhalten.)
     */
    public void distributeRoundPoints(RoundStats stats, Map<UUID, Role> roles) {
        Map<UUID, String> nameCache = new HashMap<>();

        // Namen auflösen
        for (UUID uuid : stats.getAllPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                nameCache.put(uuid, p.getName());
            } else {
                String lastName = Bukkit.getOfflinePlayer(uuid).getName();
                nameCache.put(uuid, lastName != null ? lastName : uuid.toString().substring(0, 8));
            }
        }

        // ===== Globale Statistik =====
        String summary = stats.formatSummary(nameCache, roles);
        Bukkit.broadcastMessage(summary);

        // ===== Persönliche Statistik + Punktevergabe =====
        for (UUID uuid : stats.getAllPlayers()) {
            int points = Math.max(0, stats.getPoints(uuid));
            api.addPoints(uuid, points);

            String name = nameCache.getOrDefault(uuid, "Unbekannt");
            int newTotal = api.getPoints(uuid);

            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                int kills = stats.getKills(uuid);
                boolean survived = stats.hasSurvived(uuid);
                boolean quitter = stats.isQuitter(uuid);

                p.sendMessage("§e===== Deine Runde =====");
                p.sendMessage("§7Kills: §c" + kills);
                p.sendMessage("§7Überlebt: " + (survived ? "§aJa" : "§cNein"));
                if (quitter) {
                    p.sendMessage("§7Status: §eVorzeitig verlassen");
                }
                p.sendMessage("§7Rundenpunkte: §b" + points);
                p.sendMessage("§7Neuer Punktestand: §a" + newTotal);
                p.sendMessage("§e=====================");
            }

            logger.info("Rundenpunkte: +" + points + " an " + name + " (UUID=" + uuid + "), neuer Stand=" + newTotal);
        }
    }

    // --- Punktemanagement ---
    public void addPointsToPlayer(UUID uuid, int points) {
        int safePoints = Math.max(0, points);
        api.addPoints(uuid, safePoints);

        int newPoints = api.getPoints(uuid);
        String playerName = getPlayerName(uuid);

        logger.info("Punkte hinzugefügt: +" + safePoints + " an " + playerName +
                " (UUID=" + uuid + "). Neuer Punktestand: " + newPoints);
        plugin.debug("Punkte-Update für " + playerName + ": +" + safePoints + " → " + newPoints);
    }

    public void setPoints(UUID uuid, int points) {
        int safePoints = Math.max(0, points);
        api.setPoints(uuid, safePoints);

        int newPoints = api.getPoints(uuid);
        String playerName = getPlayerName(uuid);

        logger.info("Punkte gesetzt: " + safePoints + " für " + playerName +
                " (UUID=" + uuid + "). Neuer Punktestand: " + newPoints);
        plugin.debug("Punkte gesetzt für " + playerName + " → " + newPoints);
    }

    public void applyPenalty(UUID uuid, int penaltyPoints, String reason) {
        int applied = Math.max(0, penaltyPoints);
        api.addPoints(uuid, -applied);

        int newPoints = api.getPoints(uuid);
        String playerName = getPlayerName(uuid);

        logger.info("Strafe: -" + applied + " Punkte für " + playerName +
                " (UUID=" + uuid + "). Grund: " + reason +
                ". Neuer Punktestand: " + newPoints);
        plugin.debug("Strafe angewendet: -" + applied + " für " + playerName + " (Grund: " + reason + ")");
    }

    // --- Abfragen ---
    public int getPoints(UUID uuid) {
        return api.getPoints(uuid);
    }

    private String getPlayerName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return (player != null) ? player.getName() : "Unbekannt";
    }
}
