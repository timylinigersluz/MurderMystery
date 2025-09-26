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
                        if (!p.getInventory().contains(ItemManager.createDetectiveBow())) {
                            p.getInventory().addItem(ItemManager.createDetectiveBow());
                            p.sendMessage(ChatColor.YELLOW + "Dein Detective-Bogen wurde wiederhergestellt!");
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
