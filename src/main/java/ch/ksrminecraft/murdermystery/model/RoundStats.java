package ch.ksrminecraft.murdermystery.model;

import java.util.*;

public class RoundStats {

    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, List<UUID>> killDetails = new HashMap<>();
    private final Set<UUID> survived = new HashSet<>();
    private final Set<UUID> quitters = new HashSet<>();
    private final Map<UUID, Integer> roundPoints = new HashMap<>();

    // Anzahl Fehlabschüsse pro Detective
    private final Map<UUID, Integer> detectiveInnocentKills = new HashMap<>();

    // --- Kills + Opferliste ---
    public void addKill(UUID killerId, UUID victimId) {
        kills.put(killerId, kills.getOrDefault(killerId, 0) + 1);
        killDetails.computeIfAbsent(killerId, k -> new ArrayList<>()).add(victimId);
    }

    public int getKills(UUID player) {
        return kills.getOrDefault(player, 0);
    }

    public Map<UUID, Integer> getKillsMap() {
        return new HashMap<>(kills);
    }

    public List<UUID> getKillVictims(UUID killer) {
        return new ArrayList<>(killDetails.getOrDefault(killer, Collections.emptyList()));
    }

    public Map<UUID, List<UUID>> getKillDetailsMap() {
        Map<UUID, List<UUID>> copy = new HashMap<>();
        for (Map.Entry<UUID, List<UUID>> e : killDetails.entrySet()) {
            copy.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        return copy;
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
    public void addDetectiveInnocentKill(UUID detective) {
        detectiveInnocentKills.put(detective,
                detectiveInnocentKills.getOrDefault(detective, 0) + 1);
    }

    public int getDetectiveInnocentKills(UUID detective) {
        return detectiveInnocentKills.getOrDefault(detective, 0);
    }

    // --- Alle Spieler dieser Runde ---
    public Set<UUID> getAllPlayers() {
        Set<UUID> all = new HashSet<>();
        all.addAll(kills.keySet());
        all.addAll(killDetails.keySet());
        all.addAll(survived);
        all.addAll(quitters);
        all.addAll(roundPoints.keySet());
        all.addAll(detectiveInnocentKills.keySet());
        return all;
    }

    // --- Debug-Ausgabe ---
    @Override
    public String toString() {
        return "RoundStats{" +
                "kills=" + kills +
                ", killDetails=" + killDetails +
                ", survived=" + survived +
                ", quitters=" + quitters +
                ", roundPoints=" + roundPoints +
                ", detectiveInnocentKills=" + detectiveInnocentKills +
                '}';
    }

    // --- Formatierte Übersicht ---
    public String formatSummary(Map<UUID, String> nameCache, Map<UUID, Role> roles) {
        StringBuilder sb = new StringBuilder();
        sb.append("§6===== §eRundenstatistik §6=====\n");

        for (UUID uuid : getAllPlayers()) {
            String name = nameCache.getOrDefault(uuid, uuid.toString().substring(0, 8));
            int killsCount = getKills(uuid);
            int points = getPoints(uuid);

            sb.append("§7• ").append(name);

            if (killsCount > 0) {
                sb.append(" §8| §cKills: ").append(killsCount);

                List<UUID> victims = getKillVictims(uuid);
                if (!victims.isEmpty()) {
                    sb.append(" §8(");
                    for (UUID v : victims) {
                        String vName = nameCache.getOrDefault(v, v.toString().substring(0, 8));
                        sb.append(vName).append(", ");
                    }
                    sb.setLength(sb.length() - 2); // letztes Komma entfernen
                    sb.append(")");
                }
            }

            if (hasSurvived(uuid)) sb.append(" §8| §aÜberlebt");
            if (isQuitter(uuid)) sb.append(" §8| §eQuit");

            Role role = roles.get(uuid);
            if (role == Role.DETECTIVE) {
                int fails = getDetectiveInnocentKills(uuid);
                if (fails > 0) {
                    sb.append(" §8| §cFehlabschüsse: ").append(fails);
                }
            }

            if (points != 0) sb.append(" §8| §bPunkte: ").append(points);

            sb.append("\n");
        }

        sb.append("§6=========================");
        return sb.toString();
    }
}
