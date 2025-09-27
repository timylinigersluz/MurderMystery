package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.effects.ItemManager;
import ch.ksrminecraft.murdermystery.managers.game.RoleManager;
import ch.ksrminecraft.murdermystery.model.Role;
import ch.ksrminecraft.murdermystery.utils.MessageLimiter;
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
                MessageLimiter.sendPlayerMessage(attacker, "sword-forbidden",
                        "§cNur der Mörder darf das Schwert benutzen!");
                event.setCancelled(true);
                return;
            }

            // Kein echter Schaden → nur eliminate triggern
            event.setDamage(0);

            // zentrale Logik im PlayerManager
            plugin.getGameManager().eliminate(victim, attacker);

            // Broadcast + Debug
            MessageLimiter.sendBroadcast("murderer-kill",
                    ChatColor.DARK_RED + "🔪 Der Mörder hat einen Spieler getötet!");
            plugin.debug("Murderer " + attacker.getName() + " hat " + victim.getName() + " mit dem Schwert getötet.");
        }
    }
}
