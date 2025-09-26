package ch.ksrminecraft.murdermystery.model;

import java.util.*;

public class RoundStats {

    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Set<UUID> survived = new HashSet<>();
    private final Set<UUID> quitters = new HashSet<>();
    private final Map<UUID, Integer> roundPoints = new HashMap<>();
    private final Set<UUID> detectiveKilledInnocent = new HashSet<>();

    // --- Kills ---
    public void addKill(UUID player) {
        kills.put(player, kills.getOrDefault(player, 0) + 1);
    }

    public int getKills(UUID player) {
        return kills.getOrDefault(player, 0);
    }

    public Map<UUID, Integer> getKillsMap() {
        return new HashMap<>(kills);
    }

    // --- Überleben ---
    public void markSurvived(UUID player) {
        survived.add(player);
    }

    public boolean hasSurvived(UUID player) {
        return survived.contains(player);
    }

    public Set<UUID> getSurvivors() {
        return new HashSet<>(survived);
    }

    // --- Quitter ---
    public void markQuitter(UUID player) {
        quitters.add(player);
    }

    public boolean isQuitter(UUID player) {
        return quitters.contains(player);
    }

    public Set<UUID> getQuitters() {
        return new HashSet<>(quitters);
    }

    // --- Punkte ---
    public void setPoints(UUID player, int points) {
        roundPoints.put(player, Math.max(0, points));
    }

    public void addPoints(UUID player, int points) {
        int current = roundPoints.getOrDefault(player, 0);
        roundPoints.put(player, Math.max(0, current + points));
    }

    public int getPoints(UUID player) {
        return roundPoints.getOrDefault(player, 0);
    }

    public Map<UUID, Integer> getAllPoints() {
        return new HashMap<>(roundPoints);
    }

    // --- Detective-Kills (Fehlabschüsse) ---
    public void markDetectiveKilledInnocent(UUID detective) {
        detectiveKilledInnocent.add(detective);
    }

    public boolean didDetectiveKillInnocent(UUID detective) {
        return detectiveKilledInnocent.contains(detective);
    }

    // --- Alle Spieler dieser Runde ---
    public Set<UUID> getAllPlayers() {
        Set<UUID> all = new HashSet<>();
        all.addAll(kills.keySet());
        all.addAll(survived);
        all.addAll(quitters);
        all.addAll(roundPoints.keySet());
        all.addAll(detectiveKilledInnocent);
        return all;
    }

    // --- Debug-Ausgabe ---
    @Override
    public String toString() {
        return "RoundStats{" +
                "kills=" + kills +
                ", survived=" + survived +
                ", quitters=" + quitters +
                ", roundPoints=" + roundPoints +
                ", detectiveKilledInnocent=" + detectiveKilledInnocent +
                '}';
    }

    // --- Formatierte Übersicht ---
    public String formatSummary(Map<UUID, String> nameCache) {
        StringBuilder sb = new StringBuilder();
        sb.append("§6===== §eRundenstatistik §6=====\n");

        for (UUID uuid : getAllPlayers()) {
            String name = nameCache.getOrDefault(uuid, uuid.toString().substring(0, 8));
            int killsCount = getKills(uuid);
            int points = getPoints(uuid);

            sb.append("§7• ").append(name);

            if (killsCount > 0) sb.append(" §8| §cKills: ").append(killsCount);
            if (hasSurvived(uuid)) sb.append(" §8| §aÜberlebt");
            if (isQuitter(uuid)) sb.append(" §8| §eQuit");
            if (didDetectiveKillInnocent(uuid)) sb.append(" §8| §cFehlabschuss");
            if (points != 0) sb.append(" §8| §bPunkte: ").append(points);

            sb.append("\n");
        }

        sb.append("§6=========================");
        return sb.toString();
    }
}
