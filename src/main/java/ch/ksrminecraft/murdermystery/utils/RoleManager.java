package ch.ksrminecraft.murdermystery.utils;

import ch.ksrminecraft.murdermystery.MurderMystery;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class RoleManager {

    // Speichert Rollen + Spieler UUID dauerhaft
    private static final Map<UUID, Role> roles = new HashMap<>();

    // UUID aller angegebenen Spieler wird zugeteilt
    public static Map<UUID, Role> assignRoles(Set<UUID> players) {
        roles.clear();

        List<UUID> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);

        // Mindestens 3 Spieler, sonst leer zurückgeben
        if (shuffled.size() < 3) {
            MurderMystery.getInstance().debug("Rollen konnten nicht verteilt werden – zu wenige Spieler (" + shuffled.size() + ").");
            return roles;
        }

        UUID murderer = shuffled.remove(0);
        UUID detective = shuffled.remove(0);

        roles.put(murderer, Role.MURDERER);
        roles.put(detective, Role.DETECTIVE);

        for (UUID uuid : shuffled) {
            roles.put(uuid, Role.BYSTANDER);
        }

        // Debug-Ausgabe aller Rollen
        MurderMystery.getInstance().debug("Rollen wurden verteilt:");
        roles.forEach((uuid, role) -> {
            Player p = Bukkit.getPlayer(uuid);
            String name = (p != null ? p.getName() : uuid.toString());
            MurderMystery.getInstance().debug(" - " + name + " ist " + role);
        });

        return Collections.unmodifiableMap(roles);
    }

    // Rolle eines Spielers mit der UUID abfragen
    public static Role getRole(UUID uuid) {
        Role role = roles.get(uuid);
        MurderMystery.getInstance().debug("Abfrage Rolle: " + uuid + " → " + role);
        return role;
    }

    // Rolle eines Spielers ändern mit der UUID
    public static void setRole(UUID uuid, Role role) {
        roles.put(uuid, role);
        Player p = Bukkit.getPlayer(uuid);
        String name = (p != null ? p.getName() : uuid.toString());
        MurderMystery.getInstance().debug("Rolle von " + name + " wurde geändert zu " + role);
    }

    public static Map<UUID, Role> getAllRoles() {
        return new HashMap<>(roles);
    }

    public static void removePlayer(UUID uuid) {
        roles.remove(uuid);
        MurderMystery.getInstance().debug("Spieler " + uuid + " wurde aus der Rollenliste entfernt.");
    }

    // Methode zum Holen des Detectives
    public static Player getDetective() {
        Optional<UUID> detectiveUuid = roles.entrySet().stream()
                .filter(e -> e.getValue() == Role.DETECTIVE)
                .map(Map.Entry::getKey)
                .findFirst();

        Player detective = detectiveUuid.map(Bukkit::getPlayer).orElse(null);
        MurderMystery.getInstance().debug("Detective-Abfrage → " + (detective != null ? detective.getName() : "Kein Detective gefunden"));
        return detective;
    }

    // Methode zum Holen aller Bystander (liefert UUIDs zurück)
    public static Set<UUID> getBystanders() {
        Set<UUID> bystanders = new HashSet<>();
        for (Map.Entry<UUID, Role> e : roles.entrySet()) {
            if (e.getValue() == Role.BYSTANDER) {
                bystanders.add(e.getKey());
            }
        }
        MurderMystery.getInstance().debug("Bystander-Abfrage → " + bystanders.size() + " Spieler gefunden.");
        return bystanders;
    }
}
