package ch.ksrminecraft.murdermystery.managers.effects;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class Broadcaster {

    public static void broadcastMessage(Set<UUID> players, String msg) {
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(msg);
            }
        }
    }

    public static void sendTitle(Player p, String title, String subtitle) {
        if (p != null && p.isOnline()) {
            p.sendTitle(title, subtitle, 20, 80, 20);
        }
    }

    public static void playSound(Player p, Sound sound) {
        if (p != null && p.isOnline()) {
            p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
        }
    }
}
