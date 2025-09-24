package ch.ksrminecraft.murdermystery.Listener;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.Utils.ItemManager;
import ch.ksrminecraft.murdermystery.Utils.Role;
import ch.ksrminecraft.murdermystery.Utils.RoleManager;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class WeaponListener implements Listener {

    private final MurderMystery plugin;
    public WeaponListener(MurderMystery plugin) {
        this.plugin = plugin;
    }

    // Cooldown nach Bogenschuss des Detective-Bogens
    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player shooter)) return;

        if (ItemManager.isDetectiveBow(event.getBow())) {
            if (!ItemManager.canShoot(shooter)) {
                shooter.sendMessage(ChatColor.GOLD + "Du musst 3 Sekunden warten, bevor du wieder schießen kannst!");
                event.setCancelled(true);
            } else {
                // Sound abspielen
                shooter.playSound(shooter.getLocation(), Sound.BLOCK_BIG_DRIPLEAF_TILT_DOWN, 1.0f, 1.0f);
            }
        }
    }

    // Detective Bogen trifft Spieler
    @EventHandler
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = null;

        // Spieler der schiesst wird zum Attacker
        if (event.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof Player shooter) {
            attacker = shooter;

        // Kein Pfeil hat Schaden verursacht
        } else if (event.getDamager() instanceof Player p) {
            attacker = p;
        }

        if (attacker == null) return;

        // Detective-Bogen
        if (ItemManager.isDetectiveBow(attacker.getInventory().getItemInMainHand())) {
            event.setDamage(0);

            Role victimRole = RoleManager.getRole(victim.getUniqueId());

            // Murderer wurde getroffen
            if (victimRole == Role.MURDERER) {
                victim.setHealth(0);
                plugin.setMurdererKilledByBow(true);

            // Detective hat Bystander getroffen; stirbt selber auch
            } else {
                victim.setHealth(0);
                attacker.setHealth(0);
                // Detective wird zum Innocent
                RoleManager.setRole(attacker.getUniqueId(), Role.BYSTANDER);
                attacker.getWorld().dropItemNaturally(attacker.getLocation(), ItemManager.createDetectiveBow());
            }
        }

        // Murderer-Schwert
        if (ItemManager.isMurdererSword(attacker.getInventory().getItemInMainHand())) {
            event.setDamage(0);
            victim.setHealth(0);
        }
    }


    // Bogen & Waffe können nicht fallen gelassen werden
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (ItemManager.isDetectiveBow(item) || ItemManager.isMurdererSword(item)) {
            event.setCancelled(true); // Manuelles Droppen verhindern
        }
    }

    // Bogen kann von Bystander aufgehoben werden, dieser wird neuer Detective
    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack item = event.getItem().getItemStack();
        if (ItemManager.isDetectiveBow(item)) {
            Role role = RoleManager.getRole(player.getUniqueId());
            if (role == Role.MURDERER) {
                // Murderer darf Bogen nicht aufnehmen
                event.setCancelled(true);
            } else {
                RoleManager.setRole(player.getUniqueId(), Role.DETECTIVE);
                player.sendMessage(ChatColor.AQUA + "Du bist jetzt der Detective!");
            }
        }
    }
}
