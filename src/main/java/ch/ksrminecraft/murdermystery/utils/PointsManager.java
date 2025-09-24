package ch.ksrminecraft.murdermystery.utils;

import ch.ksrminecraft.RankPointsAPI.PointsAPI;
import ch.ksrminecraft.murdermystery.MurderMystery;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

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
     * Punkte hinzufügen (niemals negativ).
     */
    public void addPointsToPlayer(UUID uuid, int points) {
        int safePoints = Math.max(0, points);
        api.addPoints(uuid, safePoints);

        int newPoints = api.getPoints(uuid);
        String playerName = getPlayerName(uuid);

        logger.info("Punkte hinzugefügt: +" + safePoints + " an " + playerName +
                " (UUID=" + uuid + "). Neuer Punktestand: " + newPoints);
        plugin.debug("Punkte-Update für " + playerName + ": +" + safePoints + " → " + newPoints);
    }

    /**
     * Punkte direkt setzen (überschreibt).
     */
    public void setPoints(UUID uuid, int points) {
        int safePoints = Math.max(0, points);
        api.setPoints(uuid, safePoints);

        int newPoints = api.getPoints(uuid);
        String playerName = getPlayerName(uuid);

        logger.info("Punkte gesetzt: " + safePoints + " für " + playerName +
                " (UUID=" + uuid + "). Neuer Punktestand: " + newPoints);
        plugin.debug("Punkte gesetzt für " + playerName + " → " + newPoints);
    }

    /**
     * Strafe (Abzug) – niemals unter 0.
     */
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

    /**
     * Punkte einer ganzen Runde verteilen (RoundStats).
     */
    public void distributeRoundPoints(RoundStats stats, Map<UUID, Role> roles) {
        for (UUID uuid : stats.getAllPlayers()) {
            int points = Math.max(0, stats.getPoints(uuid));
            api.addPoints(uuid, points);

            String name = getPlayerName(uuid);
            int newTotal = api.getPoints(uuid);

            // Spieler Chat-Abrechnung
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(ChatColor.GRAY + "===== " + ChatColor.GOLD + "Deine Punkteabrechnung" + ChatColor.GRAY + " =====");
                p.sendMessage(ChatColor.GREEN + "+" + points + " Punkte in dieser Runde");
                p.sendMessage(ChatColor.YELLOW + "Neuer Punktestand: " + newTotal);
                p.sendMessage(ChatColor.GRAY + "================================");
            }

            logger.info("Rundenpunkte: " + points + " an " + name + " (UUID=" + uuid + "), neuer Stand=" + newTotal);
        }

        // Broadcast für Übersicht
        broadcastRoundStats(stats);
    }

    private void broadcastRoundStats(RoundStats stats) {
        Bukkit.broadcastMessage(ChatColor.GRAY + "===== " + ChatColor.AQUA + "Rundenstatistik" + ChatColor.GRAY + " =====");
        for (Map.Entry<UUID, Integer> entry : stats.getAllPoints().entrySet()) {
            String name = getPlayerName(entry.getKey());
            Bukkit.broadcastMessage(ChatColor.YELLOW + name + ": " + ChatColor.GREEN + entry.getValue() + " Punkte");
        }
        Bukkit.broadcastMessage(ChatColor.GRAY + "==============================");
    }

    private String getPlayerName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return (player != null) ? player.getName() : "Unbekannt";
    }
}
