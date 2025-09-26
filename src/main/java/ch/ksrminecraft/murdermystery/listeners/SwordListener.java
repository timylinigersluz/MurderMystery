package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.effects.ItemManager;
import ch.ksrminecraft.murdermystery.managers.game.RoleManager;
import ch.ksrminecraft.murdermystery.model.Role;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class SwordListener implements Listener {

    private final MurderMystery plugin;

    public SwordListener(MurderMystery plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSwordHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        if (ItemManager.isMurdererSword(attacker.getInventory().getItemInMainHand())) {
            if (RoleManager.getRole(attacker.getUniqueId()) != Role.MURDERER) {
                event.setCancelled(true);
                attacker.sendMessage("§cNur der Mörder darf das Schwert benutzen!");
                return;
            }

            event.setDamage(0);
            victim.setHealth(0);

            broadcastKill("🔪 Murderer", attacker.getName(), victim.getName(), ChatColor.DARK_RED);
            plugin.debug("Murderer " + attacker.getName() + " hat " + victim.getName() + " mit dem Schwert getötet.");
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
