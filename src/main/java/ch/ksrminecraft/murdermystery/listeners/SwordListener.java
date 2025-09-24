package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.utils.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class SwordListener implements Listener {

    @EventHandler
    public void onSwordHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        if (ItemManager.isMurdererSword(attacker.getInventory().getItemInMainHand())) {
            event.setDamage(0);
            victim.setHealth(0);

            broadcastKill("🔪 Murderer", attacker.getName(), victim.getName(), ChatColor.DARK_RED);
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
