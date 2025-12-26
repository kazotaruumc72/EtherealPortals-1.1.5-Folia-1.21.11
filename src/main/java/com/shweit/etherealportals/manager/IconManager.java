package fr.kazotaruumc72.etherealportals.manager;

import fr.kazotaruumc72.etherealportals.model.PortalIcon;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages custom icons.
 */
public class IconManager {
  private final Map<String, PortalIcon> icons = new LinkedHashMap<>();

  /**
   * Adds a custom icon.
   *
   * @param name the icon name
   * @param base64 the base64-encoded texture
   * @return true if added, false if an icon with that name already exists
   */
  public boolean addIcon(String name, String base64) {
    String key = name.toLowerCase();
    if (icons.containsKey(key)) {
      return false;
    }
    icons.put(key, new PortalIcon(key, base64));
    return true;
  }

  /**
   * Removes a custom icon.
   *
   * @param name the icon name
   * @return true if removed, false if not found
   */
  public boolean removeIcon(String name) {
    return icons.remove(name.toLowerCase()) != null;
  }

  /**
   * Gets a custom icon by name.
   *
   * @param name the icon name
   * @return the icon, or null if not found
   */
  public PortalIcon getIcon(String name) {
    return icons.get(name.toLowerCase());
  }

  public Collection<PortalIcon> getIcons() {
    return Collections.unmodifiableCollection(icons.values());
  }
}
