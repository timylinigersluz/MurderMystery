package ch.ksrminecraft.murdermystery.utils;

import ch.ksrminecraft.murdermystery.MurderMystery;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class QuitTracker {
    // Alle Spieler, die während dem Spiel Minecraft verlassen, mit UUID abspeichern
    private static final Set<UUID> quitDuringGame = new HashSet<>();

    // Spieler markieren
    public static void mark(Player p) {
        quitDuringGame.add(p.getUniqueId());
        MurderMystery.getInstance().debug("Spieler " + p.getName() + " wurde im QuitTracker markiert.");
    }

    // Überprüfen, ob Spieler Minecraft während dem Spiel verlassen hat
    public static boolean hasQuit(Player p) {
        boolean result = quitDuringGame.contains(p.getUniqueId());
        MurderMystery.getInstance().debug("Abfrage QuitTracker: Spieler " + p.getName() + " → " + result);
        return result;
    }

    // Spieler aus QuitTracker entfernen
    public static void clear(Player p) {
        if (quitDuringGame.remove(p.getUniqueId())) {
            MurderMystery.getInstance().debug("Spieler " + p.getName() + " wurde aus dem QuitTracker entfernt.");
        } else {
            MurderMystery.getInstance().debug("Versuch Spieler " + p.getName() + " aus QuitTracker zu entfernen, war aber nicht markiert.");
        }
    }
}
