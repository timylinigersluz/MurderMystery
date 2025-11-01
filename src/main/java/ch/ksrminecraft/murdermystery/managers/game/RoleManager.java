package ch.ksrminecraft.murdermystery.managers.game;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.model.Role;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class RoleManager {

    private final MurderMystery plugin;
    private final Map<UUID, Role> roles = new HashMap<>();

    public RoleManager(MurderMystery plugin) {
        this.plugin = plugin;
    }

    // Rollen verteilen
    public Map<UUID, Role> assignRoles(Set<UUID> players) {
        roles.clear();

        plugin.debug("[RoleManager] Starte Rollenzuweisung für " + players.size() + " Spieler.");

        List<UUID> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);

        if (shuffled.size() < 3) {
            plugin.debug("[RoleManager] Rollen konnten nicht verteilt werden – zu wenige Spieler (" + shuffled.size() + ").");
            return Collections.unmodifiableMap(roles);
        }

        UUID murderer = shuffled.remove(0);
        UUID detective = shuffled.remove(0);

        roles.put(murderer, Role.MURDERER);
        roles.put(detective, Role.DETECTIVE);

        for (UUID uuid : shuffled) {
            roles.put(uuid, Role.BYSTANDER);
        }

        // Debug-Ausgabe aller Rollen
        plugin.debug("[RoleManager] Rollen wurden verteilt:");
        roles.forEach((uuid, role) -> {
            Player p = Bukkit.getPlayer(uuid);
            String name = (p != null ? p.getName() : uuid.toString());
            plugin.debug(" - Spieler " + name + " hat Rolle " + role);
        });

        return Collections.unmodifiableMap(roles);
    }

    public Role getRole(UUID uuid) {
        Role role = roles.get(uuid);
        plugin.debug("[RoleManager] Abfrage Rolle für UUID=" + uuid + " → " + role);
        return role;
    }

    public void setRole(UUID uuid, Role role) {
        roles.put(uuid, role);
        Player p = Bukkit.getPlayer(uuid);
        String name = (p != null ? p.getName() : uuid.toString());
        plugin.debug("[RoleManager] Rolle von " + name + " geändert zu " + role);
    }

    public Map<UUID, Role> getAllRoles() {
        return new HashMap<>(roles);
    }

    public void removePlayer(UUID uuid) {
        roles.remove(uuid);
        plugin.debug("[RoleManager] Spieler " + uuid + " wurde aus der Rollenliste entfernt.");
    }

    public Player getDetective() {
        Optional<UUID> detectiveUuid = roles.entrySet().stream()
                .filter(e -> e.getValue() == Role.DETECTIVE)
                .map(Map.Entry::getKey)
                .findFirst();

        Player detective = detectiveUuid.map(Bukkit::getPlayer).orElse(null);
        plugin.debug("[RoleManager] Detective-Abfrage → " + (detective != null ? detective.getName() : "Kein Detective gefunden"));
        return detective;
    }

    public Set<UUID> getBystanders() {
        Set<UUID> bystanders = new HashSet<>();
        for (Map.Entry<UUID, Role> e : roles.entrySet()) {
            if (e.getValue() == Role.BYSTANDER) {
                bystanders.add(e.getKey());
            }
        }
        plugin.debug("[RoleManager] Bystander-Abfrage → " + bystanders.size() + " Spieler gefunden.");
        return bystanders;
    }

    public void clearRoles() {
        roles.clear();
        plugin.debug("[RoleManager] Alle Rollen wurden zurückgesetzt.");
    }
}
