package fr.kazotaruumc72.etherealportals.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

/** Utility to create a custom textured player head from base64 texture value. */
public final class SkullUtils {
  private SkullUtils() {}

  /**
   * Creates a custom player head with a base64-encoded texture.
   *
   * @param base64 the base64-encoded texture value
   * @param displayName the display name for the head
   * @return ItemStack with the custom texture
   */
  public static ItemStack createHead(String base64, String displayName) {
    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
    SkullMeta meta = (SkullMeta) head.getItemMeta();

    if (displayName != null) {
      meta.setDisplayName(displayName);
    }

    if (base64 != null && !base64.isEmpty()) {
      try {
        // Create a deterministic UUID based on the texture so items with the same texture stack
        UUID textureUuid = UUID.nameUUIDFromBytes(base64.getBytes(StandardCharsets.UTF_8));
        PlayerProfile profile = Bukkit.createPlayerProfile(textureUuid, "");
        PlayerTextures textures = profile.getTextures();

        // Decode base64 to get the texture URL
        String textureUrl = getTextureUrlFromBase64(base64);
        if (textureUrl != null) {
          textures.setSkin(new URL(textureUrl));
          profile.setTextures(textures);
          meta.setOwnerProfile(profile);
        }
      } catch (MalformedURLException e) {
        System.err.println("Invalid texture URL: " + e.getMessage());
      }
    }

    if (!head.setItemMeta(meta)) {
      // This should never happen for valid SkullMeta, but handle return value for SpotBugs
      System.err.println("Failed to set skull meta");
    }
    return head;
  }

  /**
   * Extracts the texture URL from a base64-encoded texture value.
   * The base64 typically contains a JSON with the texture URL.
   *
   * @param base64 the base64-encoded texture value
   * @return the extracted texture URL, or null if parsing fails
   */
  private static String getTextureUrlFromBase64(String base64) {
    try {
      // Decode the base64 string
      String decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);

      // Extract URL from JSON (simple parsing)
      // Format: {"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/..."}}}
      int urlStart = decoded.indexOf("\"url\":\"") + 7;
      int urlEnd = decoded.indexOf("\"", urlStart);

      if (urlStart > 6 && urlEnd > urlStart) {
        return decoded.substring(urlStart, urlEnd);
      }
    } catch (Exception e) {
      System.err.println("Failed to parse base64 texture: " + e.getMessage());
    }
    return null;
  }
}
