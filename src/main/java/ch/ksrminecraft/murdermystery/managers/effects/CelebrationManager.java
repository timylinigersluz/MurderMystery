package ch.ksrminecraft.murdermystery.managers.effects;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.game.RoundResultManager;
import ch.ksrminecraft.murdermystery.model.Role;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CelebrationManager {

    private final MurderMystery plugin;

    public CelebrationManager(MurderMystery plugin) {
        this.plugin = plugin;
    }

    /**
     * Startet die Celebration nach Spielende.
     *
     * @param players    Alle Spieler der Arena
     * @param condition  Endbedingung (Murderer, Detective, Timeout)
     * @param roles      Rollen-Zuordnung der Spieler
     */
    public void startCelebration(Set<UUID> players,
                                 RoundResultManager.EndCondition condition,
                                 Map<UUID, Role> roles) {

        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) continue;

            // Sonderfall: Zeit abgelaufen → Unentschieden
            if (condition == RoundResultManager.EndCondition.TIME_UP) {
                // neutraler Fall für alle
                p.sendTitle(ChatColor.YELLOW + "⏳ Zeit abgelaufen", ChatColor.GRAY + "Unentschieden!", 10, 60, 10);
                p.sendMessage(ChatColor.YELLOW + "⏳ Das Spiel endete unentschieden.");
                continue;
            }

            boolean isWinner = isWinner(roles.get(uuid), condition);

            if (isWinner) {
                launchFireworks(p);
                p.sendTitle(ChatColor.GREEN + "Sieg!", ChatColor.YELLOW + "Du hast gewonnen!", 10, 60, 10);
                p.sendMessage(ChatColor.GREEN + "Glückwunsch, du hast gewonnen!");
            } else {
                playLoserSound(p);
                p.sendTitle(ChatColor.RED + "Niederlage", ChatColor.GRAY + "Vielleicht nächstes Mal!", 10, 60, 10);
                p.sendMessage(ChatColor.RED + "Leider verloren.");
            }
        }
    }

    private boolean isWinner(Role role, RoundResultManager.EndCondition condition) {
        if (condition == RoundResultManager.EndCondition.TIME_UP) {
            // Bei Zeitablauf → niemand ist Gewinner oder Verlierer
            return false; // neutral
        }

        switch (condition) {
            case DETECTIVE_WIN -> {
                return role == Role.DETECTIVE || role == Role.BYSTANDER;
            }
            case MURDERER_WIN -> {
                return role == Role.MURDERER;
            }
            // ggf. weitere Fälle wie CO_WIN usw.
            default -> {
                return false;
            }
        }
    }

    /** Feuerwerk für Sieger */
    public void launchFireworks(Player player) {
        for (int i = 0; i < 3; i++) {
            int delay = i * 20;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location loc = player.getLocation().add(0, 1, 0);
                Firework firework = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);

                FireworkMeta meta = firework.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .withColor(Color.AQUA, Color.GREEN, Color.YELLOW)
                        .withFade(Color.WHITE)
                        .trail(true)
                        .flicker(true)
                        .build());
                meta.setPower(1);
                firework.setFireworkMeta(meta);
            }, delay);
        }
    }

    /** Sound für Verlierer */
    public void playLoserSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }
}
