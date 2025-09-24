package ch.ksrminecraft.murdermystery.Utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.*;

public class RoleManager {

    // Speichert Rollen + Spieler UUID dauerhaft
    private static final Map<UUID, Role> roles = new HashMap<>();

    // UUID aller angegebenen Spieler wird zugeteilt
    public static Map<UUID, Role> assignRoles(Set<UUID> players) {
        // Mögliche Rollenverteilung aus vorheriger Runde beseitigen
        roles.clear();

        List<UUID> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);

        // Mindestens 3 Spieler, sonst leer zurückgeben
        if (shuffled.size() < 3) return roles;

        UUID murderer = shuffled.remove(0);
        UUID detective = shuffled.remove(0);

        // Rollenzuweisung mit Enum
        roles.put(murderer, Role.MURDERER);
        roles.put(detective, Role.DETECTIVE);

        for (UUID uuid : shuffled) {
            roles.put(uuid, Role.BYSTANDER);
        }

        return Collections.unmodifiableMap(roles);
    }

    // Rolle eines Spielers mit der UUID abfragen
    public static Role getRole(UUID uuid) {
        return roles.get(uuid);
    }

    // Rolle eines Spielers ändern mit der UUID
    public static void setRole(UUID uuid, Role role) {
        roles.put(uuid, role);
    }

    public static Map<UUID, Role> getAllRoles() {
        return new HashMap<>(roles);
    }

    public static void removePlayer(UUID uuid) {
        roles.remove(uuid);
    }

    // Methode zum Holen des Detectives
    public static Player getDetective() {
        Optional<UUID> detectiveUuid = roles.entrySet().stream()
                .filter(e -> e.getValue() == Role.DETECTIVE)
                .map(Map.Entry::getKey)
                .findFirst();

        return detectiveUuid.map(Bukkit::getPlayer).orElse(null);
    }

    // Methode zum Holen aller Bystander (liefert UUIDs zurück)
    public static Set<UUID> getBystanders() {
        Set<UUID> bystanders = new HashSet<>();
        for (Map.Entry<UUID, Role> e : roles.entrySet()) {
            if (e.getValue() == Role.BYSTANDER) {
                bystanders.add(e.getKey());
            }
        }
        return bystanders;
    }
}
