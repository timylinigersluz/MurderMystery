package ch.ksrminecraft.murdermystery.Utils;

import ch.ksrminecraft.RankPointsAPI.PointsAPI;
import ch.ksrminecraft.murdermystery.MurderMystery;
import java.util.UUID;
import java.util.logging.Logger;

public class PointsManager {

    private final PointsAPI api;
    private final Logger logger;
    private final MurderMystery plugin;

    // Konstruktor
    public PointsManager(Logger logger, MurderMystery plugin) {
        this.logger = logger;
        this.plugin = plugin;

        // Daten aus Config laden
        String url = plugin.getConfig().getString("Rank-Points-API-url");
        String user = plugin.getConfig().getString("Rank-Points-API-user");
        String pass = plugin.getConfig().getString("Rank-Points-API-password");

        if (url == null || user == null || pass == null) {
            logger.severe("Fehlende Konfigurationswerte für RankPointsAPI! Bitte config.yml prüfen.");
            throw new IllegalStateException("Config-Werte für RankPointsAPI unvollständig!");
        }

        // PointsAPI initialisieren
        this.api = new PointsAPI(url, user, pass, logger, true);
    }

    // Spieler auswählen und beliebige Anzahl Punkte hinzufügen
    public void addPointsToPlayer(UUID uuid, int points) {
        boolean success = api.addPoints(uuid, points);
        int newPoints = api.getPoints(uuid);

        if (success) {
            logger.info("Punkte erfolgreich hinzugefügt. Neuer Punktestand von " + uuid + ": " + newPoints);
        } else {
            logger.warning("Fehler beim Hinzufügen der Punkte für Spieler: " + uuid);
        }
    }
}
