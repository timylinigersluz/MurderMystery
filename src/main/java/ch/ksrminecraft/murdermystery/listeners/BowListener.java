package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.utils.ItemManager;
import ch.ksrminecraft.murdermystery.utils.Role;
import ch.ksrminecraft.murdermystery.utils.RoleManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class BowListener implements Listener {

    private final MurderMystery plugin;

    public BowListener(MurderMystery plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player shooter)) return;
        if (!ItemManager.isDetectiveBow(event.getBow())) return;

        if (!ItemManager.canShoot(shooter)) {
            shooter.sendMessage(ChatColor.GOLD + "Du musst 3 Sekunden warten, bevor du wieder schießen kannst!");
            event.setCancelled(true);
            plugin.debug("Spieler " + shooter.getName() + " wollte schießen, aber Cooldown aktiv.");
        } else {
            shooter.playSound(shooter.getLocation(), Sound.BLOCK_BIG_DRIPLEAF_TILT_DOWN, 1.0f, 1.0f);
            plugin.debug("Spieler " + shooter.getName() + " hat einen Pfeil mit dem Detective-Bogen geschossen.");
        }
    }

    @EventHandler
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;

        if (!ItemManager.isDetectiveBow(shooter.getInventory().getItemInMainHand())) return;

        event.setDamage(0);
        Role victimRole = RoleManager.getRole(victim.getUniqueId());

        if (victimRole == Role.MURDERER) {
            victim.setHealth(0);
            plugin.setMurdererKilledByBow(true);

            broadcastKill("🏹 Detective", shooter.getName(), victim.getName(), ChatColor.BLUE);
            plugin.debug("Detective " + shooter.getName() + " hat den Murderer " + victim.getName() + " getötet.");
        } else {
            victim.setHealth(0);
            shooter.setHealth(0);

            // Classic: sofort Game Over
            // Bow-Fallback: Bogen droppt
            RoleManager.setRole(shooter.getUniqueId(), Role.BYSTANDER);
            shooter.getWorld().dropItemNaturally(shooter.getLocation(), ItemManager.createDetectiveBow());

            broadcastKill("🏹 Detective", shooter.getName(), victim.getName(), ChatColor.RED);
            plugin.debug("Detective " + shooter.getName() + " hat " + victim.getName() + " (kein Murderer) getroffen → Strafe folgt.");

            int penalty = plugin.getConfig().getInt("punkte-strafe-detective", 5);
            plugin.getPointsManager().applyPenalty(shooter.getUniqueId(), penalty, "Detective hat Innocent getötet");
            shooter.sendMessage(ChatColor.RED + "Du hast einen Unschuldigen getötet! -" + penalty + " Punkte.");
        }
    }

    @EventHandler
    public void onBowPickup(PlayerPickupItemEvent event) {
        Player p = event.getPlayer();
        if (ItemManager.isDetectiveBow(event.getItem().getItemStack())) {
            if (RoleManager.getRole(p.getUniqueId()) == Role.BYSTANDER) {
                RoleManager.setRole(p.getUniqueId(), Role.DETECTIVE);
                Bukkit.broadcastMessage(ChatColor.AQUA + p.getName() + " ist der neue Detective!");
                p.sendTitle(ChatColor.AQUA + "🏹 Neuer Detective!", ChatColor.GREEN + "Du hast den Bogen aufgenommen.", 20, 60, 20);
            }
        }
    }

    private void broadcastKill(String role, String killer, String victim, ChatColor color) {
        String title = color + role + " " + killer;
        String subtitle = ChatColor.GRAY + "hat " + ChatColor.RED + victim + ChatColor.GRAY + " getötet!";
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(title, subtitle, 10, 60, 10);
            p.sendMessage(title + ChatColor.GRAY + " hat " + ChatColor.RED + victim + ChatColor.GRAY + " getötet!");
        }
    }
}
