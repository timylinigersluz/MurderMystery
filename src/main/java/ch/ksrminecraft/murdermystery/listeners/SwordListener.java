package ch.ksrminecraft.murdermystery.listeners;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.effects.ItemManager;
import ch.ksrminecraft.murdermystery.managers.game.GameManagerRegistry;
import ch.ksrminecraft.murdermystery.managers.game.RoleManager;
import ch.ksrminecraft.murdermystery.model.ArenaGame;
import ch.ksrminecraft.murdermystery.model.Role;
import ch.ksrminecraft.murdermystery.utils.MessageLimiter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class SwordListener implements Listener {

    private final MurderMystery plugin;
    private final GameManagerRegistry registry;

    public SwordListener(MurderMystery plugin, GameManagerRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @EventHandler
    public void onSwordHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        // Nur Schwert relevant
        if (!ItemManager.isMurdererSword(attacker.getInventory().getItemInMainHand())) return;

        // Arena des Angreifers ermitteln
        ArenaGame manager = registry.findArenaOfPlayer(attacker);
        if (manager == null || !manager.isGameStarted()) {
            return; // kein Spiel aktiv → nichts tun
        }

        // Nur Mörder darf es nutzen
        if (RoleManager.getRole(attacker.getUniqueId()) != Role.MURDERER) {
            MessageLimiter.sendPlayerMessage(attacker, "sword-forbidden",
                    "§cNur der Mörder darf das Schwert benutzen!");
            event.setCancelled(true);
            return;
        }

        // Schaden verhindern → wir übernehmen alles
        event.setCancelled(true);

        // Meldung
        plugin.debug("Murderer " + attacker.getName() + " hat " + victim.getName() + " mit dem Schwert getötet.");

        // Spieler in dieser Arena eliminieren
        manager.eliminate(victim, attacker);
    }
}
