package ch.ksrminecraft.murdermystery.managers.game;

import ch.ksrminecraft.RankPointsAPI.PointsAPI;
import ch.ksrminecraft.murdermystery.MurderMystery;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PointsManager {

    private final Logger logger;
    private final MurderMystery plugin;
    private final PointsAPI api;

    public PointsManager(Logger logger, MurderMystery plugin) {
        this.logger = logger;
        this.plugin = plugin;

        String url = plugin.getConfig().getString("Rank-Points-API-url");
        String user = plugin.getConfig().getString("Rank-Points-API-user");
        String pass = plugin.getConfig().getString("Rank-Points-API-password");
        boolean debug = plugin.getConfig().getBoolean("rankpoints.debug", false);
        boolean excludeStaff = plugin.getConfig().getBoolean("rankpoints.exclude-staff", false);

        this.api = new PointsAPI(url, user, pass, logger, debug, excludeStaff);
        plugin.debug("Eigene PointsAPI-Instanz initialisiert.");

        // --- DB-Test mit Dummy-UUID ---
        try {
            api.getPoints(UUID.randomUUID());
            plugin.debug("Verbindungstest zur RankPoints-Datenbank erfolgreich.");
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "RankPointsAPI-DB-Test fehlgeschlagen! Plugin wird deaktiviert.", t);
            throw new IllegalStateException("RankPointsAPI-Datenbank nicht erreichbar.", t);
        }
    }

    // --- API Methoden ---
    public void addPointsToPlayer(UUID uuid, int points) {
        api.addPoints(uuid, Math.max(0, points));
        plugin.debug("addPointsToPlayer → " + uuid + " (+" + points + ")");
    }

    public void setPoints(UUID uuid, int points) {
        api.setPoints(uuid, Math.max(0, points));
        logger.info("[PointsManager] setPoints für " + uuid + " auf " + points);
    }

    public void applyPenalty(UUID uuid, int penaltyPoints, String reason) {
        int applied = Math.max(0, penaltyPoints);
        api.addPoints(uuid, -applied);
        plugin.debug("Penalty -" + applied + " für " + uuid + " (Grund: " + reason + ")");
    }

    public int getPoints(UUID uuid) {
        int points = api.getPoints(uuid);
        plugin.debug("getPoints(" + uuid + ") = " + points);
        return points;
    }
}
