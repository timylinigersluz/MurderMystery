package ch.ksrminecraft.murdermystery.managers.effects;

import ch.ksrminecraft.murdermystery.MurderMystery;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemManager {

    private static final String DETECTIVE_BOW_NAME = ChatColor.AQUA + "Detective-Bogen";
    private static final String MURDERER_SWORD_NAME = ChatColor.DARK_RED + "Murderer-Schwert";

    private static final Map<UUID, Long> bowCooldown = new HashMap<>();
    private static final long COOLDOWN_MILLIS = 3000; // 3 Sekunden

    public static ItemStack createDetectiveBow() {
        ItemStack bow = new ItemStack(Material.BOW, 1);
        ItemMeta meta = bow.getItemMeta();
        meta.setDisplayName(DETECTIVE_BOW_NAME);
        meta.addEnchant(Enchantment.INFINITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        meta.setUnbreakable(true);
        bow.setItemMeta(meta);
        return bow;
    }

    public static ItemStack createMurdererSword() {
        ItemStack sword = new ItemStack(Material.IRON_SWORD, 1);
        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName(MURDERER_SWORD_NAME);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        meta.setUnbreakable(true);
        sword.setItemMeta(meta);
        return sword;
    }

    public static boolean isDetectiveBow(ItemStack item) {
        if (item == null || item.getType() != Material.BOW || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return DETECTIVE_BOW_NAME.equals(meta.getDisplayName());
    }

    public static boolean isMurdererSword(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_SWORD || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return MURDERER_SWORD_NAME.equals(meta.getDisplayName());
    }

    public static boolean canShoot(Player player) {
        long now = System.currentTimeMillis();
        UUID id = player.getUniqueId();
        if (!bowCooldown.containsKey(id) || now - bowCooldown.get(id) >= COOLDOWN_MILLIS) {
            bowCooldown.put(id, now);
            return true;
        }
        return false;
    }
}
