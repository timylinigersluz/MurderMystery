package ch.ksrminecraft.murdermystery.managers.effects;

import ch.ksrminecraft.murdermystery.MurderMystery;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemManager {

    // ------------------ Konstanten ------------------
    private static final String DETECTIVE_BOW_NAME = ChatColor.AQUA + "Detective-Bogen"; // §b
    private static final String MURDERER_SWORD_NAME = ChatColor.DARK_RED + "Murderer-Schwert"; // §4

    private static final Map<UUID, Long> bowCooldown = new HashMap<>();
    private static final long COOLDOWN_MILLIS = 3000; // 3 Sekunden

    // ------------------ Erstellung ------------------
    public static ItemStack createDetectiveBow() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(DETECTIVE_BOW_NAME);
            meta.addEnchant(Enchantment.INFINITY, 1, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            meta.setUnbreakable(true);
            bow.setItemMeta(meta);
        }
        return bow;
    }

    public static ItemStack createMurdererSword() {
        ItemStack sword = new ItemStack(Material.IRON_SWORD, 1);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MURDERER_SWORD_NAME);
            meta.setUnbreakable(true);
            sword.setItemMeta(meta);
        }
        return sword;
    }

    // ------------------ Erkennung ------------------
    public static boolean isDetectiveBow(ItemStack item) {
        if (item == null || item.getType() != Material.BOW || !item.hasItemMeta()) return false;
        return DETECTIVE_BOW_NAME.equals(item.getItemMeta().getDisplayName());
    }

    public static boolean isMurdererSword(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_SWORD || !item.hasItemMeta()) return false;
        return MURDERER_SWORD_NAME.equals(item.getItemMeta().getDisplayName());
    }

    // ------------------ Gameplay ------------------
    public static boolean canShoot(Player player) {
        long now = System.currentTimeMillis();
        UUID id = player.getUniqueId();
        if (!bowCooldown.containsKey(id) || now - bowCooldown.get(id) >= COOLDOWN_MILLIS) {
            bowCooldown.put(id, now);
            return true;
        }
        return false;
    }

    // ------------------ Cleanup ------------------
    /**
     * Entfernt alle Spezialitems (Bogen, Schwert, Pfeile) aus dem Inventar UND vom Boden in der Nähe.
     */
    public static void clearSpecialItems(Player p) {
        boolean removed = false;

        // --- Inventar prüfen ---
        for (ItemStack item : p.getInventory().getContents()) {
            if (isDetectiveBow(item) || isMurdererSword(item) ||
                    (item != null && item.getType() == Material.ARROW)) {
                p.getInventory().remove(item);
                removed = true;
            }
        }

        // OffHand prüfen
        ItemStack off = p.getInventory().getItemInOffHand();
        if (isDetectiveBow(off) || isMurdererSword(off) ||
                (off != null && off.getType() == Material.ARROW)) {
            p.getInventory().setItemInOffHand(null);
            removed = true;
        }

        // --- Dropped Items im Umkreis prüfen ---
        for (Entity entity : p.getNearbyEntities(10, 10, 10)) { // 10 Block Radius
            if (entity instanceof Item dropped) {
                ItemStack stack = dropped.getItemStack();
                if (isDetectiveBow(stack) || isMurdererSword(stack) ||
                        stack.getType() == Material.ARROW) {
                    dropped.remove();
                    removed = true;
                    MurderMystery.getInstance().debug("Cleanup: Dropped Spezialitem von " + p.getName() +
                            " entfernt (" + stack.getType() + ")");
                }
            }
        }

        if (removed) {
            MurderMystery.getInstance().debug("Cleanup: Spezialitems bei " + p.getName() + " entfernt.");
        }
        p.updateInventory();
    }
}
