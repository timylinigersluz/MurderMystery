package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.effects.ItemManager;
import ch.ksrminecraft.murdermystery.managers.game.GameManagerRegistry;
import ch.ksrminecraft.murdermystery.managers.game.RoleManager;
import ch.ksrminecraft.murdermystery.model.ArenaGame;
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
import org.bukkit.inventory.ItemStack;

public class BowListener implements Listener {

    private final MurderMystery plugin;
    private final GameManagerRegistry registry;

    public BowListener(MurderMystery plugin, GameManagerRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player shooter)) return;

        // Arena des Schützen finden
        ArenaGame manager = registry.findArenaOfPlayer(shooter);
        if (manager == null || !manager.isGameStarted()) return;

        ItemStack bow = event.getBow();
        if (!ItemManager.isDetectiveBow(bow)) return;

        if (!ItemManager.canShoot(shooter)) {
            MessageLimiter.sendPlayerMessage(
                    shooter,
                    "bow-cooldown",
                    ChatColor.GOLD + "Du musst 3 Sekunden warten, bevor du wieder schießen kannst!"
            );
            event.setCancelled(true);
        } else {
            shooter.playSound(shooter.getLocation(), Sound.BLOCK_BIG_DRIPLEAF_TILT_DOWN, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;

        // Arena-Kontext bestimmen (vom Schützen)
        ArenaGame shooterMgr = registry.findArenaOfPlayer(shooter);
        if (shooterMgr == null || !shooterMgr.isGameStarted()) return;

        // Optional: sicherstellen, dass Opfer in derselben Arena ist
        ArenaGame victimMgr = registry.findArenaOfPlayer(victim);
        if (victimMgr == null || victimMgr != shooterMgr) return;

        if (!ItemManager.isDetectiveBow(shooter.getInventory().getItemInMainHand())) return;

        // Standard-Schaden komplett verhindern – wir handeln alles selbst
        event.setCancelled(true);

        Role victimRole = RoleManager.getRole(victim.getUniqueId());

        if (victimRole == Role.MURDERER) {
            // Meldung vor eliminate()
            MessageLimiter.sendBroadcast(
                    "murderer-killed",
                    ChatColor.BLUE + shooter.getName() + " hat den Mörder " + victim.getName() + " eliminiert!"
            );
            plugin.debug("Detective " + shooter.getName() + " hat den Mörder " + victim.getName()
                    + " eliminiert (Arena=" + shooterMgr.getArena().getName() + ").");

            shooterMgr.eliminate(victim, shooter);
            shooterMgr.checkWinConditions();

        } else {
            int penalty = plugin.getConfigManager().getPointsKillInnocent();

            MessageLimiter.sendBroadcast(
                    "innocent-killed",
                    ChatColor.RED + "Der Detective hat einen Unschuldigen eliminiert und erhält eine Strafe ("
                            + penalty + " Pt)."
            );
            plugin.debug("Detective " + shooter.getName() + " hat " + victim.getName()
                    + " (unschuldig) eliminiert (Arena=" + shooterMgr.getArena().getName() + ").");

            shooterMgr.eliminate(victim, shooter);
            shooterMgr.getPointsManager().applyPenalty(
                    shooter.getUniqueId(),
                    Math.abs(penalty),
                    "Unschuldigen eliminiert"
            );
        }
    }
}
