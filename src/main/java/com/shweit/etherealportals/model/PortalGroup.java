package fr.kazotaruumc72.etherealportals.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a logical grouping of portals. Teleportation only allowed within group.
 */
public class PortalGroup {
  private final String name;
  private final Map<String, Portal> portals = new LinkedHashMap<>();

  /**
   * Creates a new portal group.
   *
   * @param name the group name
   */
  public PortalGroup(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public Collection<Portal> getPortals() {
    return Collections.unmodifiableCollection(portals.values());
  }

  /**
   * Gets a portal by name (case-insensitive).
   *
   * @param portalName the portal name
   * @return the portal, or null if not found
   */
  public Portal getPortal(String portalName) {
    return portals.get(portalName.toLowerCase());
  }

  /**
   * Adds a portal to this group.
   *
   * @param portal the portal to add
   * @return true if added, false if a portal with that name already exists
   */
  public boolean addPortal(Portal portal) {
    String key = portal.getName().toLowerCase();
    if (portals.containsKey(key)) {
      return false;
    }
    portals.put(key, portal);
    return true;
  }

  /**
   * Removes a portal from this group.
   *
   * @param portalName the portal name
   * @return true if removed, false if not found
   */
  public boolean removePortal(String portalName) {
    return portals.remove(portalName.toLowerCase()) != null;
  }

  public boolean isEmpty() {
    return portals.isEmpty();
  }
}
