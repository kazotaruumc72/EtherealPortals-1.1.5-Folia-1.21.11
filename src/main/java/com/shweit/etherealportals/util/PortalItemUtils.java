package fr.kazotaruumc72.etherealportals.util;

import java.util.List;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Utility class for creating and validating Portal Items.
 */
public final class PortalItemUtils {
  private PortalItemUtils() {}

  /**
   * Creates a portal item with custom texture.
   *
   * @param plugin the plugin instance
   * @param texture the base64-encoded texture
   * @param displayName the display name for the item
   * @param lore the lore lines for the item
   * @return the created portal item
   */
  public static ItemStack createPortalItem(
      Plugin plugin, String texture, String displayName, List<String> lore) {
    // Create player head with custom texture using SkullUtils
    ItemStack item = SkullUtils.createHead(texture, displayName);
    ItemMeta meta = item.getItemMeta();

    // Add lore if provided
    if (lore != null && !lore.isEmpty()) {
      meta.setLore(lore);
    }

    // Add persistent data to mark this as a portal item
    NamespacedKey key = new NamespacedKey(plugin, "portal_item");
    meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "true");

    if (!item.setItemMeta(meta)) {
      System.err.println("Failed to set portal item meta");
    }

    return item;
  }

  /**
   * Checks if an item is a portal item.
   *
   * @param plugin the plugin instance
   * @param item the item to check
   * @return true if the item is a portal item
   */
  public static boolean isPortalItem(Plugin plugin, ItemStack item) {
    if (item == null || !item.hasItemMeta()) {
      return false;
    }

    ItemMeta meta = item.getItemMeta();
    NamespacedKey key = new NamespacedKey(plugin, "portal_item");
    return meta.getPersistentDataContainer().has(key, PersistentDataType.STRING);
  }
}
