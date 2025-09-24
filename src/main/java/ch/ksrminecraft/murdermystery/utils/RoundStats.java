package ch.ksrminecraft.murdermystery.utils;

import java.util.*;

public class RoundStats {

    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Set<UUID> survived = new HashSet<>();
    private final Set<UUID> quitters = new HashSet<>();
    private final Map<UUID, Integer> roundPoints = new HashMap<>();

    public void addKill(UUID player) {
        kills.put(player, kills.getOrDefault(player, 0) + 1);
    }

    public int getKills(UUID player) {
        return kills.getOrDefault(player, 0);
    }

    public void markSurvived(UUID player) {
        survived.add(player);
    }

    public boolean hasSurvived(UUID player) {
        return survived.contains(player);
    }

    public void markQuitter(UUID player) {
        quitters.add(player);
    }

    public boolean isQuitter(UUID player) {
        return quitters.contains(player);
    }

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

    public Set<UUID> getAllPlayers() {
        Set<UUID> all = new HashSet<>();
        all.addAll(kills.keySet());
        all.addAll(survived);
        all.addAll(quitters);
        all.addAll(roundPoints.keySet());
        return all;
    }

    @Override
    public String toString() {
        return "RoundStats{" +
                "kills=" + kills +
                ", survived=" + survived +
                ", quitters=" + quitters +
                ", roundPoints=" + roundPoints +
                '}';
    }
}
