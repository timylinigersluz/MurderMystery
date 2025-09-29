package ch.ksrminecraft.murdermystery.managers.game;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.model.Role;
import ch.ksrminecraft.murdermystery.model.RoundStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Punkte-Manager, lädt RankPointsAPI dynamisch via Reflection aus dem RankPointsAPI-Plugin.
 */
public class PointsManager {

    private final Logger logger;
    private final MurderMystery plugin;

    private Object apiInstance;
    private Method mAddPoints;
    private Method mSetPoints;
    private Method mGetPoints;

    public PointsManager(Logger logger, MurderMystery plugin) {
        this.logger = logger;
        this.plugin = plugin;

        String url = plugin.getConfig().getString("Rank-Points-API-url");
        String user = plugin.getConfig().getString("Rank-Points-API-user");
        String pass = plugin.getConfig().getString("Rank-Points-API-password");

        boolean debug = plugin.getConfig().getBoolean("rankpoints.debug", false);
        boolean excludeStaff = plugin.getConfig().getBoolean("rankpoints.exclude-staff", true);

        if (url == null || user == null || pass == null) {
            logger.severe("Fehlende Konfigurationswerte für RankPointsAPI! Bitte config.yml prüfen.");
            throw new IllegalStateException("Config-Werte für RankPointsAPI unvollständig!");
        }

        Plugin rp = Bukkit.getPluginManager().getPlugin("RankPointsAPI");
        if (rp == null || !rp.isEnabled()) {
            throw new IllegalStateException("RankPointsAPI ist nicht geladen oder deaktiviert!");
        }

        try {
            ClassLoader rpCl = rp.getClass().getClassLoader();
            Class<?> apiClass = rpCl.loadClass("ch.ksrminecraft.RankPointsAPI.PointsAPI");

            Constructor<?> ctor = apiClass.getConstructor(
                    String.class, String.class, String.class,
                    Logger.class, boolean.class, boolean.class
            );
            apiInstance = ctor.newInstance(url, user, pass, logger, debug, excludeStaff);

            mAddPoints = apiClass.getMethod("addPoints", UUID.class, int.class);
            mSetPoints = apiClass.getMethod("setPoints", UUID.class, int.class);
            mGetPoints = apiClass.getMethod("getPoints", UUID.class);

            plugin.debug("RankPointsAPI erfolgreich initialisiert (Reflection).");
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Fehler beim Initialisieren der RankPointsAPI via Reflection", t);
            throw new IllegalStateException("RankPointsAPI konnte nicht geladen werden.", t);
        }
    }

    // --- Rundenpunkte ---
    public void distributeRoundPoints(RoundStats stats, Map<UUID, Role> roles) {
        Map<UUID, String> nameCache = new HashMap<>();

        for (UUID uuid : stats.getAllPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                nameCache.put(uuid, p.getName());
            } else {
                String lastName = Bukkit.getOfflinePlayer(uuid).getName();
                nameCache.put(uuid, lastName != null ? lastName : uuid.toString().substring(0, 8));
            }
        }

        String summary = stats.formatSummary(nameCache, roles);
        Bukkit.broadcastMessage(summary);

        for (UUID uuid : stats.getAllPlayers()) {
            int points = Math.max(0, stats.getPoints(uuid));
            addPointsInternal(uuid, points);

            String name = nameCache.getOrDefault(uuid, "Unbekannt");
            int newTotal = getPoints(uuid);

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

    // --- API Methoden ---
    public void addPointsToPlayer(UUID uuid, int points) {
        addPointsInternal(uuid, Math.max(0, points));
        plugin.debug("addPointsToPlayer → " + uuid);
    }

    public void setPoints(UUID uuid, int points) {
        try {
            mSetPoints.invoke(apiInstance, uuid, Math.max(0, points));
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "setPoints fehlgeschlagen für " + uuid, t);
        }
    }

    public void applyPenalty(UUID uuid, int penaltyPoints, String reason) {
        int applied = Math.max(0, penaltyPoints);
        addPointsInternal(uuid, -applied);
        plugin.debug("Penalty -" + applied + " für " + uuid + " (Grund: " + reason + ")");
    }

    public int getPoints(UUID uuid) {
        try {
            Object res = mGetPoints.invoke(apiInstance, uuid);
            return (res instanceof Integer i) ? i : 0;
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "getPoints fehlgeschlagen für " + uuid, t);
            return 0;
        }
    }

    // --- Intern ---
    private void addPointsInternal(UUID uuid, int delta) {
        try {
            mAddPoints.invoke(apiInstance, uuid, delta);
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "addPoints fehlgeschlagen für " + uuid + " (Δ=" + delta + ")", t);
        }
    }
}
