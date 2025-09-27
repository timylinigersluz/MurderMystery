package ch.ksrminecraft.murdermystery.utils;

import ch.ksrminecraft.murdermystery.MurderMystery;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MessageLimiter {

    private static final Map<String, Long> cooldowns = new HashMap<>();
    private static final Map<UUID, Map<String, Long>> playerCooldowns = new HashMap<>();

    private static long globalCooldown;
    private static long playerCooldown;

    // Muss beim Plugin-Start initialisiert werden
    public static void init(MurderMystery plugin) {
        globalCooldown = plugin.getConfig().getLong("message-cooldown.global", 3000);
        playerCooldown = plugin.getConfig().getLong("message-cooldown.player", 2000);
        Bukkit.getLogger().info("[MurderMystery] MessageLimiter initialisiert → global="
                + globalCooldown + "ms, player=" + playerCooldown + "ms");
    }

    // --- Broadcast-Nachrichten ---
    public static void sendBroadcast(String key, String message) {
        if (isOnCooldown("broadcast:" + key, globalCooldown)) return;
        Bukkit.broadcastMessage(message);
        setCooldown("broadcast:" + key);
    }

    // --- Konsolen-Logs ---
    public static void logConsole(String key, String message) {
        if (isOnCooldown("console:" + key, globalCooldown)) return;
        Bukkit.getLogger().info(message);
        setCooldown("console:" + key);
    }

    // --- Spieler-Nachrichten ---
    public static void sendPlayerMessage(Player player, String key, String message) {
        UUID uuid = player.getUniqueId();
        playerCooldowns.putIfAbsent(uuid, new HashMap<>());
        Map<String, Long> map = playerCooldowns.get(uuid);

        if (map.containsKey(key) && System.currentTimeMillis() - map.get(key) < playerCooldown) {
            return; // noch auf Cooldown
        }

        player.sendMessage(message);
        map.put(key, System.currentTimeMillis());
    }

    private static boolean isOnCooldown(String key, long cd) {
        return cooldowns.containsKey(key) && System.currentTimeMillis() - cooldowns.get(key) < cd;
    }

    private static void setCooldown(String key) {
        cooldowns.put(key, System.currentTimeMillis());
    }
}
