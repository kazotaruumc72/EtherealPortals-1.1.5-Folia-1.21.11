package fr.kazotaruumc72.etherealportals.model;

import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Represents a single portal inside a group.
 */
public class Portal {
  private final String name;
  private final Location baseLocation; // block base
  private String iconName; // optional icon reference
  private final boolean breakable; // whether this portal can be broken and dropped as item

  /**
   * Creates a new portal (defaults to non-breakable).
   *
   * @param name the portal name
   * @param baseLocation the base location of the portal
   * @param iconName the optional icon name
   */
  public Portal(String name, Location baseLocation, String iconName) {
    this(name, baseLocation, iconName, false);
  }

  /**
   * Creates a new portal.
   *
   * @param name the portal name
   * @param baseLocation the base location of the portal
   * @param iconName the optional icon name
   * @param breakable whether this portal can be broken and dropped as item
   */
  public Portal(String name, Location baseLocation, String iconName, boolean breakable) {
    this.name = name;
    this.baseLocation = baseLocation.clone();
    this.iconName = iconName;
    this.breakable = breakable;
  }

  public String getName() {
    return name;
  }

  public Location getBaseLocation() {
    return baseLocation.clone();
  }

  public Location getCenterLocation() {
    return baseLocation.clone().add(0.5, 0, 0.5);
  }

  public String getIconName() {
    return iconName;
  }

  public void setIconName(String iconName) {
    this.iconName = iconName;
  }

  public boolean isBreakable() {
    return breakable;
  }

  /**
   * Serializes this portal to a configuration section.
   *
   * @param section the configuration section to write to
   */
  public void serialize(ConfigurationSection section) {
    section.set("world", baseLocation.getWorld().getName());
    section.set("x", baseLocation.getX());
    section.set("y", baseLocation.getY());
    section.set("z", baseLocation.getZ());
    section.set("yaw", baseLocation.getYaw());
    section.set("pitch", baseLocation.getPitch());
    if (iconName != null) {
      section.set("icon", iconName);
    }
    section.set("breakable", breakable);
  }

  /**
   * Deserializes a portal from a configuration section.
   *
   * @param name the portal name
   * @param section the configuration section to read from
   * @param world the world the portal is in
   * @return the deserialized portal
   */
  public static Portal deserialize(String name, ConfigurationSection section, World world) {
    double x = section.getDouble("x");
    double y = section.getDouble("y");
    double z = section.getDouble("z");
    float yaw = (float) section.getDouble("yaw", 0.0);
    float pitch = (float) section.getDouble("pitch", 0.0);
    Location loc = new Location(world, x, y, z, yaw, pitch);
    String icon = section.getString("icon");
    boolean breakable = section.getBoolean("breakable", false);
    return new Portal(name, loc, icon, breakable);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Portal)) {
      return false;
    }
    Portal portal = (Portal) o;
    return name.equalsIgnoreCase(portal.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name.toLowerCase());
  }
}
