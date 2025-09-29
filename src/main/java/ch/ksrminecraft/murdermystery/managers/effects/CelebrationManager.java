package ch.ksrminecraft.murdermystery.managers.effects;

import ch.ksrminecraft.murdermystery.MurderMystery;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

public class CelebrationManager {

    private final MurderMystery plugin;

    public CelebrationManager(MurderMystery plugin) {
        this.plugin = plugin;
    }

    public void launchFireworks(Player player) {
        if (player == null || !player.isOnline()) return;

        for (int i = 0; i < 3; i++) {
            int delay = i * 20; // jede Rakete um 1 Sekunde versetzt
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location loc = player.getLocation().add(0, 1, 0);
                Firework firework = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);

                FireworkMeta meta = firework.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .withColor(Color.AQUA, Color.GREEN, Color.YELLOW)
                        .withFade(Color.WHITE)
                        .trail(true)
                        .flicker(true)
                        .build());
                meta.setPower(1); // kurze Flugzeit
                firework.setFireworkMeta(meta);
            }, delay);
        }
    }
}
