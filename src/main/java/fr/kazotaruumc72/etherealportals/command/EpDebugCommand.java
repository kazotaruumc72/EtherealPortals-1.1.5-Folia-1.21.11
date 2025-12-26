package fr.kazotaruumc72.etherealportals.command;

import fr.kazotaruumc72.etherealportals.EtherealPortals;
import fr.kazotaruumc72.etherealportals.util.MessageUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

/**
 * Debug commands for monitoring and managing portal TextDisplay entities.
 */
public class EpDebugCommand implements CommandExecutor, TabCompleter {
  private final EtherealPortals plugin;

  /**
   * Creates a new EpDebug command executor.
   *
   * @param plugin the plugin instance
   */
  public EpDebugCommand(EtherealPortals plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command,
      String label, String[] args) {
    if (!sender.hasPermission("portal.admin")) {
      MessageUtils.error(sender, "You don't have permission to use this command.");
      return true;
    }

    if (args.length == 0) {
      MessageUtils.info(sender, "Usage: &d/epdebug &7<&bcount&7|&blist&7|&bcleanup&7|&bcheck&7>");
      return true;
    }

    String sub = args[0].toLowerCase(Locale.ROOT);
    switch (sub) {
      case "count":
        handleCount(sender);
        return true;
      case "list":
        handleList(sender);
        return true;
      case "cleanup":
        handleCleanup(sender);
        return true;
      case "check":
        handleCheck(sender);
        return true;
      default:
        MessageUtils.error(sender,
            "Unknown subcommand! Use &d/epdebug count&c, &dlist&c, &dcleanup&c, or &dcheck&c.");
        return true;
    }
  }

  private void handleCount(CommandSender sender) {
    int total = 0;
    Map<String, Integer> perWorld = new HashMap<>();

    for (World world : Bukkit.getWorlds()) {
      int count = 0;
      for (TextDisplay td : world.getEntitiesByClass(TextDisplay.class)) {
        if (td.getScoreboardTags().stream().anyMatch(tag -> tag.startsWith("ep_portal:"))) {
          count++;
          total++;
        }
      }
      if (count > 0) {
        perWorld.put(world.getName(), count);
      }
    }

    MessageUtils.info(sender, "Portal TextDisplay entities: &d" + total);
    if (!perWorld.isEmpty()) {
      sender.sendMessage("");
      MessageUtils.info(sender, "Per world:");
      perWorld.forEach((worldName, count) ->
          MessageUtils.info(sender, "  &7- &b" + worldName + "&7: &d" + count));
    }

    if (total == 0) {
      MessageUtils.success(sender, "No portal TextDisplays found - all clean!");
    }
  }

  private void handleList(CommandSender sender) {
    Map<String, Integer> portalCounts = new HashMap<>();

    for (World world : Bukkit.getWorlds()) {
      for (TextDisplay td : world.getEntitiesByClass(TextDisplay.class)) {
        for (String tag : td.getScoreboardTags()) {
          if (tag.startsWith("ep_portal:")) {
            portalCounts.merge(tag, 1, Integer::sum);
          }
        }
      }
    }

    if (portalCounts.isEmpty()) {
      MessageUtils.success(sender, "No portal TextDisplays found - all clean!");
      return;
    }

    MessageUtils.info(sender, "Portal TextDisplays by tag:");
    sender.sendMessage("");
    portalCounts.forEach((tag, count) -> {
      String color = count > 1 ? "&c" : "&a";
      String warning = count > 1 ? " &c⚠ DUPLICATE!" : "";
      MessageUtils.info(sender, "  " + color + count + "x &7" + tag + warning);
    });

    long duplicates = portalCounts.values().stream().filter(c -> c > 1).count();
    if (duplicates > 0) {
      sender.sendMessage("");
      MessageUtils.warning(sender,
          "Found &c" + duplicates + "&e duplicate(s)! Use &d/epdebug cleanup &eto remove them.");
    }
  }

  private void handleCleanup(CommandSender sender) {
    int removed = 0;

    for (World world : Bukkit.getWorlds()) {
      for (TextDisplay td : world.getEntitiesByClass(TextDisplay.class)) {
        if (td.getScoreboardTags().stream().anyMatch(tag -> tag.startsWith("ep_portal:"))) {
          td.remove();
          removed++;
        }
      }
    }

    MessageUtils.success(sender,
        "Removed &d" + removed + "&a portal TextDisplay(s). They will respawn automatically.");
  }

  private void handleCheck(CommandSender sender) {
    if (!(sender instanceof Player)) {
      MessageUtils.error(sender, "This command can only be used by players.");
      return;
    }

    Player player = (Player) sender;
    Location playerLoc = player.getLocation();
    double radius = 50.0;

    MessageUtils.info(sender, "Checking for portal TextDisplays within &d" + radius + "&7 blocks:");
    sender.sendMessage("");

    int found = 0;
    for (TextDisplay td : player.getWorld().getEntitiesByClass(TextDisplay.class)) {
      if (td.getScoreboardTags().stream().anyMatch(tag -> tag.startsWith("ep_portal:"))) {
        if (td.getLocation().distance(playerLoc) <= radius) {
          found++;
          String tags = String.join(", ", td.getScoreboardTags());
          Location loc = td.getLocation();
          MessageUtils.info(sender,
              "  &d#" + found + " &7at &b" + String.format("%.1f, %.1f, %.1f",
                  loc.getX(), loc.getY(), loc.getZ()));
          MessageUtils.info(sender, "      &7Tags: &d" + tags);
          MessageUtils.info(sender,
              "      &7Distance: &b" + String.format("%.1f", loc.distance(playerLoc))
                  + "&7 blocks");
          sender.sendMessage("");
        }
      }
    }

    if (found == 0) {
      MessageUtils.warning(sender, "No portal TextDisplays found nearby.");
    } else {
      MessageUtils.success(sender, "Found &d" + found + "&a TextDisplay(s) nearby.");
    }
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command,
      String alias, String[] args) {
    List<String> completions = new ArrayList<>();

    if (args.length == 1) {
      List<String> subcommands = List.of("count", "list", "cleanup", "check");
      String input = args[0].toLowerCase();
      for (String sub : subcommands) {
        if (sub.startsWith(input)) {
          completions.add(sub);
        }
      }
    }

    return completions;
  }
}
