package ch.ksrminecraft.murdermystery.Utils;

import org.bukkit.entity.Player;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class QuitTracker {
    // Alle Spieler, die während dem Spiel Minecraft verlassen mit UUID abspeichern
    private static final Set<UUID> quitDuringGame = new HashSet<>();

    // Spieler markieren
    public static void mark(Player p) {
        quitDuringGame.add(p.getUniqueId());
    }

    // Überprüfen, ob Spieler Minecraft während dem Spiel verlassen hat
    public static boolean hasQuit(Player p) {
        return quitDuringGame.contains(p.getUniqueId());
    }

    // Spieler aus QuitTracker entfernen
    public static void clear(Player p) {
        quitDuringGame.remove(p.getUniqueId());
    }
}
