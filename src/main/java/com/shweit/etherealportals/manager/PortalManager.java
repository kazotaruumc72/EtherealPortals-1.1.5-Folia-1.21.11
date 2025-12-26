package fr.kazotaruumc72.etherealportals.manager;

import fr.kazotaruumc72.etherealportals.model.Portal;
import fr.kazotaruumc72.etherealportals.model.PortalGroup;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Manages portal groups and portals in memory.
 */
public class PortalManager {
  private final Map<String, PortalGroup> groups = new LinkedHashMap<>();

  /**
   * Result class that contains both a portal and its parent group.
   */
  public static class PortalResult {
    private final Portal portal;
    private final PortalGroup group;

    /**
     * Creates a new portal result.
     *
     * @param portal the portal
     * @param group the portal's parent group
     */
    public PortalResult(Portal portal, PortalGroup group) {
      this.portal = portal;
      this.group = group;
    }

    public Portal getPortal() {
      return portal;
    }

    public PortalGroup getGroup() {
      return group;
    }
  }

  public Collection<PortalGroup> getGroups() {
    return Collections.unmodifiableCollection(groups.values());
  }

  /**
   * Gets a portal group by name (case-insensitive).
   *
   * @param name the group name
   * @return the group, or null if not found
   */
  public PortalGroup getGroup(String name) {
    return groups.get(name.toLowerCase());
  }

  /**
   * Creates a portal group if it doesn't exist.
   *
   * @param name the group name
   * @return the existing or newly created group
   */
  public PortalGroup createGroupIfAbsent(String name) {
    return groups.computeIfAbsent(name.toLowerCase(), k -> new PortalGroup(name));
  }

  /**
   * Deletes a portal group.
   *
   * @param name the group name
   * @return true if deleted, false if not found
   */
  public boolean deleteGroup(String name) {
    return groups.remove(name.toLowerCase()) != null;
  }

  /**
   * Adds a portal to a group (defaults to non-breakable).
   *
   * @param groupName the group name
   * @param portalName the portal name
   * @param loc the portal location
   * @param icon the optional icon name
   * @return true if added, false if a portal with that name already exists
   */
  public boolean addPortal(String groupName, String portalName, Location loc, String icon) {
    return addPortal(groupName, portalName, loc, icon, false);
  }

  /**
   * Adds a portal to a group.
   *
   * @param groupName the group name
   * @param portalName the portal name
   * @param loc the portal location
   * @param icon the optional icon name
   * @param breakable whether the portal can be broken
   * @return true if added, false if a portal with that name already exists
   */
  public boolean addPortal(String groupName, String portalName, Location loc,
      String icon, boolean breakable) {
    PortalGroup group = createGroupIfAbsent(groupName);
    return group.addPortal(new Portal(portalName, loc, icon, breakable));
  }

  /**
   * Removes a portal from a group.
   *
   * @param groupName the group name
   * @param portalName the portal name
   * @return true if removed, false if not found
   */
  public boolean removePortal(String groupName, String portalName) {
    PortalGroup group = getGroup(groupName);
    if (group == null) {
      return false;
    }
    boolean removed = group.removePortal(portalName);
    if (removed && group.isEmpty()) {
      // keep group unless explicitly deleted
    }
    return removed;
  }

  /**
   * Finds a portal at the given location.
   *
   * @param playerLoc the player location
   * @param width the portal width
   * @param depth the portal depth
   * @param height the portal height
   * @return the portal result, or null if no portal found
   */
  public PortalResult findPortalAt(Location playerLoc, double width,
      double depth, double height) {
    World world = playerLoc.getWorld();
    for (PortalGroup group : groups.values()) {
      for (Portal portal : group.getPortals()) {
        Location base = portal.getBaseLocation();
        if (!Objects.equals(world, base.getWorld())) {
          continue;
        }
        double minX = base.getX() - (width - 1) / 2.0;
        double maxX = base.getX() + (width + 1) / 2.0;
        double minY = base.getY();
        double maxY = base.getY() + height;
        double minZ = base.getZ() - (depth - 1) / 2.0;
        double maxZ = base.getZ() + (depth + 1) / 2.0;
        double x = playerLoc.getX();
        double y = playerLoc.getY();
        double z = playerLoc.getZ();
        if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ) {
          return new PortalResult(portal, group);
        }
      }
    }
    return null;
  }
}
