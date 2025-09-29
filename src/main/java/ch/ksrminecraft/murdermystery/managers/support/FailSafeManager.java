package ch.ksrminecraft.murdermystery.managers.support;

import ch.ksrminecraft.murdermystery.MurderMystery;
import ch.ksrminecraft.murdermystery.managers.game.GameManager;
import ch.ksrminecraft.murdermystery.managers.effects.ItemManager;
import ch.ksrminecraft.murdermystery.model.Role;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class FailSafeManager {

    private final MurderMystery plugin;
    private final GameManager gameManager;
    private int taskId = -1;

    public FailSafeManager(GameManager gameManager, MurderMystery plugin) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    public void start() {
        stop(); // Safety
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!gameManager.isGameStarted()) return;

            for (UUID uuid : gameManager.getPlayers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) continue;

                Role role = gameManager.getRoles().get(uuid);
                if (role == null) continue;

                switch (role) {
                    case DETECTIVE -> {
                        // Bogen wiederherstellen
                        if (!p.getInventory().contains(ItemManager.createDetectiveBow())) {
                            p.getInventory().addItem(ItemManager.createDetectiveBow());
                            p.sendMessage(ChatColor.YELLOW + "Dein Detective-Bogen wurde wiederhergestellt!");
                        }

                        // Pfeil wiederherstellen (immer genau 1)
                        long arrowCount = p.getInventory().all(org.bukkit.Material.ARROW)
                                .values()
                                .stream()
                                .mapToInt(stack -> stack != null ? stack.getAmount() : 0)
                                .sum();

                        if (arrowCount == 0) {
                            p.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.ARROW, 1));
                            p.sendMessage(ChatColor.YELLOW + "Dein Detective-Pfeil wurde wiederhergestellt!");
                        } else if (arrowCount > 1) {
                            // Sicherheit: auf 1 Pfeil reduzieren
                            p.getInventory().remove(org.bukkit.Material.ARROW);
                            p.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.ARROW, 1));
                            plugin.debug("Detective " + p.getName() + " hatte mehr als 1 Pfeil → korrigiert auf genau 1.");
                        }
                    }
                    case MURDERER -> {
                        if (!p.getInventory().contains(ItemManager.createMurdererSword())) {
                            p.getInventory().addItem(ItemManager.createMurdererSword());
                            p.sendMessage(ChatColor.YELLOW + "Dein Murderer-Schwert wurde wiederhergestellt!");
                        }
                    }
                }
            }
        }, 0L, 200L);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }
}
