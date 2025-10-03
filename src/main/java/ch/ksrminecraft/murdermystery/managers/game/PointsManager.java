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

        plugin.debug("[PointsManager] Initialisiere PointsAPI mit URL=" + url + ", User=" + user +
                ", Debug=" + debug + ", excludeStaff=" + excludeStaff);

        this.api = new PointsAPI(url, user, pass, logger, debug, excludeStaff);
        plugin.debug("[PointsManager] Eigene PointsAPI-Instanz erstellt.");

        // --- DB-Test mit Dummy-UUID ---
        try {
            UUID dummy = UUID.randomUUID();
            plugin.debug("[PointsManager] Starte Verbindungstest mit Dummy-UUID=" + dummy);
            api.getPoints(dummy);
            plugin.debug("[PointsManager] Verbindungstest zur RankPoints-Datenbank erfolgreich.");
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "[PointsManager] RankPointsAPI-DB-Test fehlgeschlagen!", t);
            throw new IllegalStateException("RankPointsAPI-Datenbank nicht erreichbar.", t);
        }
    }

    // --- API Methoden ---
    public void addPointsToPlayer(UUID uuid, int points) {
        plugin.debug("[PointsManager] addPointsToPlayer → uuid=" + uuid + ", points=+" + points);
        api.addPoints(uuid, Math.max(0, points));
    }

    public void setPoints(UUID uuid, int points) {
        plugin.debug("[PointsManager] setPoints → uuid=" + uuid + ", points=" + points);
        api.setPoints(uuid, Math.max(0, points));
        logger.info("[PointsManager] setPoints für " + uuid + " auf " + points);
    }

    public void applyPenalty(UUID uuid, int penaltyPoints, String reason) {
        int applied = Math.max(0, penaltyPoints);
        plugin.debug("[PointsManager] applyPenalty → uuid=" + uuid + ", penalty=-" + applied + ", reason=" + reason);
        api.addPoints(uuid, -applied);
    }

    public int getPoints(UUID uuid) {
        plugin.debug("[PointsManager] getPoints → Anfrage für uuid=" + uuid);
        int points = api.getPoints(uuid);
        plugin.debug("[PointsManager] getPoints(" + uuid + ") = " + points);
        return points;
    }
}
