package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.effects.ItemManager;
import ch.ksrminecraft.murdermystery.managers.game.RoleManager;
import ch.ksrminecraft.murdermystery.model.Role;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

public class BowListener implements Listener {

    private final MurderMystery plugin;

    public BowListener(MurderMystery plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!plugin.getGameManager().isGameStarted()) return;
        if (!(event.getEntity() instanceof Player shooter)) return;
        if (!ItemManager.isDetectiveBow(event.getBow())) return;

        if (!ItemManager.canShoot(shooter)) {
            shooter.sendMessage(ChatColor.GOLD + "⏳ Du musst 3 Sekunden warten, bevor du wieder schiessen kannst!");
            event.setCancelled(true);
        } else {
            shooter.playSound(shooter.getLocation(), Sound.BLOCK_BIG_DRIPLEAF_TILT_DOWN, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!plugin.getGameManager().isGameStarted()) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;
        if (!ItemManager.isDetectiveBow(shooter.getInventory().getItemInMainHand())) return;

        // Standard-Schaden verhindern
        event.setDamage(0);

        Role victimRole = RoleManager.getRole(victim.getUniqueId());

        // === Fall 1: Mörder getroffen ===
        if (victimRole == Role.MURDERER) {
            victim.setHealth(0);
            Bukkit.broadcastMessage(ChatColor.BLUE + "⚔ Der Mörder wurde getötet!");
            plugin.debug("Detective " + shooter.getName() + " hat den Mörder " + victim.getName() + " getötet.");

            // Spielende prüfen
            plugin.getGameManager().checkWinConditions();
            return;
        }

        // === Fall 2: Unschuldiger getroffen ===
        victim.setHealth(0);

        // Strafe für Detective (nur über API, nicht in Runde addieren)
        int penalty = plugin.getConfigManager().getPointsKillInnocent();
        plugin.getPointsManager().applyPenalty(
                shooter.getUniqueId(),
                Math.abs(penalty),
                "Unschuldigen getötet"
        );

        // ➕ In RoundStats eintragen
        if (plugin.getGameManager().getRoundStats() != null) {
            plugin.getGameManager().getRoundStats().markDetectiveKilledInnocent(shooter.getUniqueId());
        }

        // Nachricht an alle
        Bukkit.broadcastMessage(ChatColor.RED + "❌ Der Detective hat einen Unschuldigen getötet und erhält eine Strafe (" + penalty + " Pt).");
        plugin.debug("Detective " + shooter.getName() + " hat " + victim.getName() + " (unschuldig) getötet. Strafe: " + penalty + " Punkte.");
    }
}
