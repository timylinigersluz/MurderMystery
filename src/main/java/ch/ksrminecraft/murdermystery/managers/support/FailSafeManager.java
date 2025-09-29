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
                        // Detective-Bogen sicherstellen (Infinity/Unbreaking)
                        boolean hasBow = p.getInventory().containsAtLeast(ItemManager.createDetectiveBow(), 1);
                        if (!hasBow) {
                            p.getInventory().addItem(ItemManager.createDetectiveBow());
                            plugin.debug("FailSafe: Bogen bei " + p.getName() + " wiederhergestellt.");
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
                            plugin.debug("FailSafe: Kein Pfeil bei " + p.getName() + " → wiederhergestellt.");
                        } else if (arrowCount > 1) {
                            p.getInventory().remove(Material.ARROW);
                            p.getInventory().addItem(new ItemStack(Material.ARROW, 1));
                            plugin.debug("FailSafe: Pfeile bei " + p.getName() + " still auf genau 1 reduziert (hatte " + arrowCount + ").");
                        }
                    }
                    case MURDERER -> {
                        if (!p.getInventory().contains(ItemManager.createMurdererSword())) {
                            p.getInventory().addItem(ItemManager.createMurdererSword());
                            p.sendMessage(ChatColor.YELLOW + "Dein Murderer-Schwert wurde wiederhergestellt!");
                            plugin.debug("FailSafe: Schwert bei " + p.getName() + " wiederhergestellt.");
                        }
                    }
                }
            }
        }, 0L, 200L); // alle 10 Sekunden
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }
}
