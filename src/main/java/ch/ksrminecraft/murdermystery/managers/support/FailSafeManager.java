package ch.ksrminecraft.murdermystery.managers.support;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.effects.ItemManager;
import ch.ksrminecraft.murdermystery.managers.game.GameManager;
import ch.ksrminecraft.murdermystery.model.Role;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class FailSafeManager {

    private final MurderMystery plugin;
    private final GameManager gameManager;
    private int taskId = -1;

    public FailSafeManager(GameManager gameManager, MurderMystery plugin) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    /**
     * Startet die FailSafe-Überprüfung in einem wiederkehrenden Task.
     */
    public void start() {
        stop(); // Safety
        plugin.debug("[FailSafeManager] Task gestartet (alle 10 Sekunden).");

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!gameManager.isGameStarted()) {
                return; // kein Spam → nur laufen, wenn Spiel aktiv
            }

            for (UUID uuid : gameManager.getPlayers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) continue;

                Role role = gameManager.getRoles().get(uuid);
                if (role == null) continue;

                switch (role) {
                    case DETECTIVE -> {
                        // Detective-Bogen sicherstellen (Infinity/Unbreaking)
                        boolean hasBow = p.getInventory().containsAtLeast(ItemManager.createDetectiveBow(), 1);
                        if (!hasBow) {
                            p.getInventory().addItem(ItemManager.createDetectiveBow());
                            plugin.debug("[FailSafeManager] Detective-Bogen bei " + p.getName() + " wiederhergestellt.");
                        }

                        // Pfeile prüfen → genau 1
                        int arrowCount = p.getInventory().all(Material.ARROW)
                                .values()
                                .stream()
                                .mapToInt(stack -> stack != null ? stack.getAmount() : 0)
                                .sum();

                        if (arrowCount == 0) {
                            p.getInventory().addItem(new ItemStack(Material.ARROW, 1));
                            p.sendMessage(ChatColor.YELLOW + "Dein Detective-Pfeil wurde wiederhergestellt!");
                            plugin.debug("[FailSafeManager] Kein Pfeil bei " + p.getName() + " → 1 Pfeil hinzugefügt.");
                        } else if (arrowCount > 1) {
                            p.getInventory().remove(Material.ARROW);
                            p.getInventory().addItem(new ItemStack(Material.ARROW, 1));
                            plugin.debug("[FailSafeManager] Pfeile bei " + p.getName() +
                                    " korrigiert → hatte " + arrowCount + ", jetzt = 1.");
                        }
                        // ✅ keine Debug-Ausgabe mehr, wenn alles in Ordnung
                    }
                    case MURDERER -> {
                        if (!p.getInventory().contains(ItemManager.createMurdererSword())) {
                            p.getInventory().addItem(ItemManager.createMurdererSword());
                            p.sendMessage(ChatColor.YELLOW + "Dein Murderer-Schwert wurde wiederhergestellt!");
                            plugin.debug("[FailSafeManager] Murderer-Schwert bei " + p.getName() + " wiederhergestellt.");
                        }
                        // ✅ keine Debug-Ausgabe mehr, wenn alles in Ordnung
                    }
                    case BYSTANDER -> {
                        // keine Debug-Ausgabe nötig
                    }
                }
            }
        }, 0L, 200L); // alle 10 Sekunden
    }

    /**
     * Stoppt den FailSafe-Task.
     */
    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            plugin.debug("[FailSafeManager] Task gestoppt.");
            taskId = -1;
        }
    }
}
