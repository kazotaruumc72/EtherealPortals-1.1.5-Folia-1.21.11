package fr.kazotaruumc72.etherealportals.util;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Utility for formatted messages. */
public final class MessageUtils {
  private static final String PREFIX = ChatColor.DARK_PURPLE + "["
      + ChatColor.LIGHT_PURPLE + "Ethereal Portals"
      + ChatColor.DARK_PURPLE + "]" + ChatColor.RESET + " ";

  private MessageUtils() {}

  /**
   * Sends a message with the plugin prefix.
   *
   * @param sender the command sender
   * @param msg the message (supports color codes with &)
   */
  public static void send(CommandSender sender, String msg) {
    sender.sendMessage(PREFIX + ChatColor.translateAlternateColorCodes('&', msg));
  }

  /**
   * Sends a success message (green).
   *
   * @param sender the command sender
   * @param msg the message (supports color codes with &)
   */
  public static void success(CommandSender sender, String msg) {
    sender.sendMessage(PREFIX + ChatColor.GREEN
        + ChatColor.translateAlternateColorCodes('&', msg));
  }

  /**
   * Sends an error message (red).
   *
   * @param sender the command sender
   * @param msg the message (supports color codes with &)
   */
  public static void error(CommandSender sender, String msg) {
    sender.sendMessage(PREFIX + ChatColor.RED
        + ChatColor.translateAlternateColorCodes('&', msg));
  }

  /**
   * Sends an info message (gray).
   *
   * @param sender the command sender
   * @param msg the message (supports color codes with &)
   */
  public static void info(CommandSender sender, String msg) {
    sender.sendMessage(PREFIX + ChatColor.GRAY
        + ChatColor.translateAlternateColorCodes('&', msg));
  }

  /**
   * Sends a warning message (yellow).
   *
   * @param sender the command sender
   * @param msg the message (supports color codes with &)
   */
  public static void warning(CommandSender sender, String msg) {
    sender.sendMessage(PREFIX + ChatColor.YELLOW
        + ChatColor.translateAlternateColorCodes('&', msg));
  }

  /**
   * Sends a teleportation message.
   *
   * @param sender the command sender
   * @param portalName the portal name
   */
  public static void teleport(CommandSender sender, String portalName) {
    sender.sendMessage(PREFIX + ChatColor.GRAY + "Teleporting to "
        + ChatColor.LIGHT_PURPLE + portalName + ChatColor.GRAY + "...");
  }

  /**
   * Sends a cooldown message.
   *
   * @param sender the command sender
   * @param seconds remaining cooldown seconds
   */
  public static void cooldown(CommandSender sender, long seconds) {
    sender.sendMessage(PREFIX + ChatColor.YELLOW + "Please wait "
        + ChatColor.GOLD + seconds + "s"
        + ChatColor.YELLOW + " before teleporting again.");
  }

  /**
   * Formats location coordinates.
   *
   * @param loc the location
   * @return formatted coordinate string
   */
  public static String formatCoords(Location loc) {
    return String.format("x=%.1f y=%.1f z=%.1f", loc.getX(), loc.getY(), loc.getZ());
  }

  /**
   * Parses a relative coordinate token.
   *
   * @param player the player for relative coordinates
   * @param token the coordinate token (~ for relative)
   * @param base the base coordinate
   * @return the parsed coordinate value
   */
  public static double parseRelative(Player player, String token, double base) {
    if (token.startsWith("~")) {
      if (token.length() == 1) {
        return base;
      }
      try {
        return base + Double.parseDouble(token.substring(1));
      } catch (NumberFormatException e) {
        return base;
      }
    }
    try {
      return Double.parseDouble(token);
    } catch (NumberFormatException e) {
      return base;
    }
  }
}
