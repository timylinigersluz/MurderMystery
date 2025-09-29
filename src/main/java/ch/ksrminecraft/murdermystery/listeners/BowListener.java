package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.effects.ItemManager;
import ch.ksrminecraft.murdermystery.managers.game.RoleManager;
import ch.ksrminecraft.murdermystery.model.Role;
import ch.ksrminecraft.murdermystery.utils.MessageLimiter;
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
            MessageLimiter.sendPlayerMessage(shooter, "bow-cooldown",
                    ChatColor.GOLD + "⏳ Du musst 3 Sekunden warten, bevor du wieder schießen kannst!");
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

        // Standard-Schaden komplett verhindern
        event.setCancelled(true);

        Role victimRole = RoleManager.getRole(victim.getUniqueId());

        if (victimRole == Role.MURDERER) {
            plugin.getGameManager().eliminate(victim, shooter);

            MessageLimiter.sendBroadcast("murderer-killed",
                    ChatColor.BLUE + "⚔ Der Mörder wurde eliminiert!");
            plugin.debug("Detective " + shooter.getName() + " hat den Mörder " + victim.getName() + " eliminiert.");
            plugin.getGameManager().checkWinConditions();

        } else {
            plugin.getGameManager().eliminate(victim, shooter);

            int penalty = plugin.getConfigManager().getPointsKillInnocent();
            plugin.getPointsManager().applyPenalty(shooter.getUniqueId(),
                    Math.abs(penalty), "Unschuldigen eliminiert");

            MessageLimiter.sendBroadcast("innocent-killed",
                    ChatColor.RED + "Der Detective hat einen Unschuldigen eliminiert und erhält eine Strafe (" + penalty + " Pt).");
            plugin.debug("Detective " + shooter.getName() + " hat " + victim.getName() + " (unschuldig) eliminiert.");
        }
    }
}
