package fr.kazotaruumc72.etherealportals.command;

import fr.kazotaruumc72.etherealportals.EtherealPortals;
import fr.kazotaruumc72.etherealportals.manager.IconManager;
import fr.kazotaruumc72.etherealportals.manager.PortalManager;
import fr.kazotaruumc72.etherealportals.model.Portal;
import fr.kazotaruumc72.etherealportals.model.PortalGroup;
import fr.kazotaruumc72.etherealportals.model.PortalIcon;
import fr.kazotaruumc72.etherealportals.util.MessageUtils;
import fr.kazotaruumc72.etherealportals.util.PortalItemUtils;
import fr.kazotaruumc72.etherealportals.util.SkullUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Handles /portal group & icon subcommands plus GUI opening. */
public class PortalCommand implements CommandExecutor, TabCompleter {
  private final EtherealPortals plugin;

  /**
   * Creates a new portal command executor.
   *
   * @param plugin the plugin instance
   */
  public PortalCommand(EtherealPortals plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command,
      String label, String[] args) {
    if (args.length == 0) {
      MessageUtils.info(sender,
          "Usage: &d/portal &7<&bgroup&7|&bicon&7> &7<subcommand>");
      return true;
    }
    switch (args[0].toLowerCase(Locale.ROOT)) {
      case "group":
        handleGroup(sender, args);
        return true;
      case "icon":
        handleIcon(sender, args);
        return true;
      case "give":
        handleGive(sender, args);
        return true;
      default:
        MessageUtils.error(sender,
            "Unknown category! Use &d/portal group&c, &d/portal icon&c, or &d/portal give&c.");
        return true;
    }
  }

  private void handleGroup(CommandSender sender, String[] args) {
    PortalManager pm = plugin.getPortalManager();
    if (args.length < 2) {
      MessageUtils.info(sender,
          "Available commands: &dcreate&7, &ddelete&7, &dadd&7, &dremove&7, &dlist");
      return;
    }
    String sub = args[1].toLowerCase();
    switch (sub) {
      case "create":
        {
        if (!sender.hasPermission("portal.group.create")) {
          noPerm(sender);
          return;
        }
        if (args.length < 3) {
          MessageUtils.info(sender, "Usage: &d/portal group create &b<name>");
          return;
        }
        pm.createGroupIfAbsent(args[2]);
        MessageUtils.success(sender,
            "Portal group &d" + args[2] + "&a has been created!");
        plugin.getDataManager().saveGroups();
        return;
        }
      case "delete":
        {
        if (!sender.hasPermission("portal.group.delete")) {
          noPerm(sender);
          return;
        }
        if (args.length < 3) {
          MessageUtils.info(sender, "Usage: &d/portal group delete &b<name>");
          return;
        }
        PortalGroup group = pm.getGroup(args[2]);
        boolean ok = pm.deleteGroup(args[2]);
        if (ok) {
          // Clean up text displays for all portals in the group
          if (group != null) {
            plugin.getVisualTask().removeGroupTextDisplays(group);
          }
          MessageUtils.success(sender,
              "Portal group &d" + args[2] + "&a has been deleted.");
        } else {
          MessageUtils.error(sender,
              "Portal group &d" + args[2] + "&c doesn't exist.");
        }
        plugin.getDataManager().saveGroups();
        return;
        }
      case "add":
        {
        if (!sender.hasPermission("portal.group.add")) {
          noPerm(sender);
          return;
        }
        if (!(sender instanceof Player)) {
          MessageUtils.error(sender, "This command can only be used by players.");
          return;
        }
        if (args.length < 4) {
          MessageUtils.info(sender,
              "Usage: &d/portal group add &b<group> <name> &7[x] [y] [z] [world] [icon]");
          MessageUtils.info(sender,
              "Omit coordinates to use your current location.");
          return;
        }
        Player p = (Player) sender;
        String group = args[2];
        String name = args[3];

        // Use player location if no coordinates provided
        double x;
        double y;
        double z;
        World world;
        String icon;

        if (args.length >= 7) {
          // Coordinates provided
          x = MessageUtils.parseRelative(p, args[4], p.getLocation().getX());
          y = MessageUtils.parseRelative(p, args[5], p.getLocation().getY());
          z = MessageUtils.parseRelative(p, args[6], p.getLocation().getZ());
          world = p.getWorld();
          if (args.length >= 8) {
            World w = Bukkit.getWorld(args[7]);
            if (w != null) {
              world = w;
            }
          }
          icon = args.length >= 9 ? args[8] : null;
        } else {
          // No coordinates - use player location
          Location loc = p.getLocation();
          x = loc.getX();
          y = loc.getY();
          z = loc.getZ();
          world = loc.getWorld();
          // Icon can be provided as 5th argument (args[4]) if no coordinates given
          icon = args.length >= 5 ? args[4] : null;
        }
        Location portalLoc = new Location(world, Math.floor(x), Math.floor(y), Math.floor(z));
        boolean added = pm.addPortal(group, name, portalLoc, icon);
        if (added) {
          MessageUtils.success(sender,
              "Portal &d" + name + "&a has been added to group &d" + group + "&a!");
          // Create text display for the new portal
          plugin.getVisualTask().createTextDisplay(
              portalLoc.clone().add(0.5, 3, 0.5), group, name);
        } else {
          MessageUtils.error(sender,
              "A portal with the name &d" + name
              + "&c already exists in this group.");
        }
        plugin.getDataManager().saveGroups();
        return;
        }
      case "remove":
        {
        if (!sender.hasPermission("portal.group.remove")) {
          noPerm(sender);
          return;
        }
        if (args.length < 4) {
          MessageUtils.info(sender,
              "Usage: &d/portal group remove &b<group> <name>");
          return;
        }
        String groupName = args[2];
        String portalName = args[3];
        // Get portal before removing to clean up visual entities
        PortalGroup group = pm.getGroup(groupName);
        if (group == null) {
          MessageUtils.error(sender,
              "Group &d" + groupName + "&c doesn't exist.");
          return;
        }
        Portal portal = group.getPortal(portalName);
        if (portal == null) {
          MessageUtils.error(sender,
              "Portal &d" + portalName + "&c doesn't exist in group &d"
              + groupName + "&c.");
          return;
        }
        // Clean up text display for this portal before removing
        plugin.getVisualTask().removeTextDisplay(groupName, portalName, portal);
        // Now remove the portal
        boolean removed = pm.removePortal(groupName, portalName);
        if (removed) {
          MessageUtils.success(sender,
              "Portal &d" + portalName + "&a has been removed from group &d"
              + groupName + "&a.");
        } else {
          MessageUtils.error(sender,
              "Failed to remove portal &d" + portalName + "&c from group &d"
              + groupName + "&c.");
        }
        plugin.getDataManager().saveGroups();
        return;
        }
      case "list":
        {
        if (!sender.hasPermission("portal.use")) {
          noPerm(sender);
          return;
        }
        if (args.length == 2) {
          if (pm.getGroups().isEmpty()) {
            MessageUtils.warning(sender, "No portal groups have been created yet.");
          } else {
            MessageUtils.info(sender,
                "Portal Groups &7(" + pm.getGroups().size() + ")&7: &d"
                + pm.getGroups().stream().map(PortalGroup::getName)
                    .collect(Collectors.joining("&7, &d")));
          }
        } else {
          PortalGroup g = pm.getGroup(args[2]);
          if (g == null) {
            MessageUtils.error(sender,
                "Portal group &d" + args[2] + "&c doesn't exist.");
            return;
          }
          if (g.getPortals().isEmpty()) {
            MessageUtils.warning(sender,
                "Group &d" + g.getName() + "&e doesn't have any portals yet.");
          } else {
            MessageUtils.info(sender,
                "Portals in &d" + g.getName() + " &7(" + g.getPortals().size()
                + ")&7: &d"
                + g.getPortals().stream().map(Portal::getName)
                    .collect(Collectors.joining("&7, &d")));
          }
        }
        return;
        }
      default:
        MessageUtils.error(sender,
            "Unknown subcommand! Use &d/portal group &cto see available commands.");
        return;
    }
  }

  private void handleIcon(CommandSender sender, String[] args) {
    IconManager im = plugin.getIconManager();
    if (args.length < 2) {
      MessageUtils.info(sender, "Available commands: &dadd&7, &dremove&7, &dlist");
      return;
    }
    String sub = args[1].toLowerCase();
    switch (sub) {
      case "add":
        {
        if (!sender.hasPermission("portal.icon.add")) {
          noPerm(sender);
          return;
        }
        if (args.length < 4) {
          MessageUtils.info(sender, "Usage: &d/portal icon add &b<name> <base64>");
          return;
        }
        boolean ok = im.addIcon(args[2], args[3]);
        if (ok) {
          MessageUtils.success(sender,
              "Custom icon &d" + args[2] + "&a has been added!");
        } else {
          MessageUtils.error(sender,
              "An icon with the name &d" + args[2] + "&c already exists.");
        }
        plugin.getDataManager().saveIcons();
        return;
        }
      case "remove":
        {
        if (!sender.hasPermission("portal.icon.remove")) {
          noPerm(sender);
          return;
        }
        if (args.length < 3) {
          MessageUtils.info(sender, "Usage: &d/portal icon remove &b<name>");
          return;
        }
        boolean ok = im.removeIcon(args[2]);
        if (ok) {
          MessageUtils.success(sender,
              "Custom icon &d" + args[2] + "&a has been removed.");
        } else {
          MessageUtils.error(sender, "Icon &d" + args[2] + "&c doesn't exist.");
        }
        plugin.getDataManager().saveIcons();
        return;
        }
      case "list":
        {
        if (!(sender instanceof Player)) {
          MessageUtils.error(sender, "This command can only be used by players.");
          return;
        }
        if (!sender.hasPermission("portal.icon.list")) {
          noPerm(sender);
          return;
        }
        Player p = (Player) sender;
        openIconList(p);
        return;
        }
      default:
        MessageUtils.error(sender,
            "Unknown subcommand! Use &d/portal icon &cto see available commands.");
        return;
    }
  }

  private void handleGive(CommandSender sender, String[] args) {
    // Check permission
    if (!sender.hasPermission("portal.item.give")) {
      noPerm(sender);
      return;
    }

    // Check arguments: /portal give <player> [name]
    if (args.length < 2) {
      MessageUtils.info(sender, "Usage: &d/portal give &b<player> &7[name]");
      return;
    }

    // Get target player
    Player target = Bukkit.getPlayer(args[1]);
    if (target == null) {
      MessageUtils.error(sender, "Player &d" + args[1] + "&c is not online.");
      return;
    }

    // Get custom name if provided, otherwise use default
    String itemName = plugin.getPortalItemName();
    if (args.length >= 3) {
      // Join remaining args as custom name
      StringBuilder nameBuilder = new StringBuilder();
      for (int i = 2; i < args.length; i++) {
        if (i > 2) {
          nameBuilder.append(" ");
        }
        nameBuilder.append(args[i]);
      }
      itemName = ChatColor.translateAlternateColorCodes('&', nameBuilder.toString());
    }

    // Create portal item
    ItemStack item = PortalItemUtils.createPortalItem(
        plugin,
        plugin.getDefaultPortalTexture(),
        itemName,
        plugin.getPortalItemLore()
    );

    // Give item to player
    target.getInventory().addItem(item);

    // Success messages
    MessageUtils.success(sender,
        "Gave portal item to &d" + target.getName() + "&a!");
    MessageUtils.success(target, "You received a portal item!");
  }

  private void openIconList(Player player) {
    int size = 9 * ((plugin.getIconManager().getIcons().size() / 9) + 1);
    if (size > 54) {
      size = 54;
    }
    Inventory inv = Bukkit.createInventory(player, size,
        ChatColor.DARK_PURPLE + "Icons");
    for (PortalGroup group : plugin.getPortalManager().getGroups()) {
      // no icons here, skip
    }
    plugin.getIconManager().getIcons().forEach(icon -> {
      ItemStack head = SkullUtils.createHead(icon.getBase64(),
          ChatColor.LIGHT_PURPLE + icon.getName());
      ItemMeta meta = head.getItemMeta();
      if (meta != null) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Custom Icon");
        meta.setLore(lore);
        if (!head.setItemMeta(meta)) {
          plugin.getLogger().warning("Failed to set item meta for icon: "
              + icon.getName());
        }
      }
      inv.addItem(head);
    });
    player.openInventory(inv);
  }

  private void noPerm(CommandSender sender) {
    MessageUtils.error(sender, "You don't have permission to use this command.");
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command,
      String alias, String[] args) {
    if (args.length == 1) {
      return partial(args[0], List.of("group", "icon", "give"));
    }

    // Group commands
    if (args[0].equalsIgnoreCase("group")) {
      if (args.length == 2) {
        return partial(args[1], List.of("create", "delete", "add", "remove", "list"));
      }

      String subCmd = args[1].toLowerCase();
      PortalManager pm = plugin.getPortalManager();

      // /portal group delete <group>
      if (subCmd.equals("delete") && args.length == 3) {
        return partial(args[2], getGroupNames());
      }

      // /portal group add <group> <name> [x] [y] [z] [world] [icon]
      if (subCmd.equals("add")) {
        if (args.length == 3) {
          return partial(args[2], getGroupNames());
        }
        if (args.length == 4) {
          return Collections.singletonList("<portal_name>");
        }
        if (args.length == 5 && sender instanceof Player) {
          // Can be either x coordinate or icon name (if using player location)
          Player p = (Player) sender;
          List<String> suggestions = new ArrayList<>();
          suggestions.add(String.valueOf((int) p.getLocation().getX()));
          suggestions.add("~");
          suggestions.addAll(getIconNames());
          return partial(args[4], suggestions);
        }
        if (args.length >= 6 && args.length <= 7 && sender instanceof Player) {
          Player p = (Player) sender;
          Location loc = p.getLocation();
          int index = args.length - 5; // 1=y, 2=z
          String coord;
          if (index == 1) {
            coord = String.valueOf((int) loc.getY());
          } else {
            coord = String.valueOf((int) loc.getZ());
          }
          return partial(args[args.length - 1], List.of(coord, "~"));
        }
        if (args.length == 8) {
          return partial(args[7], getWorldNames());
        }
        if (args.length == 9) {
          return partial(args[8], getIconNames());
        }
      }

      // /portal group remove <group> <portal>
      if (subCmd.equals("remove")) {
        if (args.length == 3) {
          return partial(args[2], getGroupNames());
        }
        if (args.length == 4) {
          PortalGroup group = pm.getGroup(args[2]);
          if (group != null) {
            return partial(args[3], group.getPortals().stream()
                .map(Portal::getName).collect(Collectors.toList()));
          }
        }
      }

      // /portal group list [group]
      if (subCmd.equals("list") && args.length == 3) {
        return partial(args[2], getGroupNames());
      }
    }

    // Icon commands
    if (args[0].equalsIgnoreCase("icon")) {
      if (args.length == 2) {
        return partial(args[1], List.of("add", "remove", "list"));
      }

      String subCmd = args[1].toLowerCase();

      // /portal icon remove <icon>
      if (subCmd.equals("remove") && args.length == 3) {
        return partial(args[2], getIconNames());
      }

      // /portal icon add <name> <base64>
      if (subCmd.equals("add")) {
        if (args.length == 3) {
          return Collections.singletonList("<icon_name>");
        }
        if (args.length == 4) {
          return Collections.singletonList("<base64_texture>");
        }
      }
    }

    // Give command: /portal give <player> [name]
    if (args[0].equalsIgnoreCase("give")) {
      if (args.length == 2) {
        // Tab-complete player names
        return partial(args[1], Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .collect(Collectors.toList()));
      }
      if (args.length == 3) {
        return Collections.singletonList("[portal_name]");
      }
    }

    return Collections.emptyList();
  }

  private List<String> partial(String token, List<String> base) {
    String lower = token.toLowerCase();
    return base.stream()
        .filter(s -> s.toLowerCase().startsWith(lower))
        .collect(Collectors.toList());
  }

  private List<String> getGroupNames() {
    return plugin.getPortalManager().getGroups().stream()
        .map(PortalGroup::getName)
        .collect(Collectors.toList());
  }

  private List<String> getIconNames() {
    return plugin.getIconManager().getIcons().stream()
        .map(PortalIcon::getName)
        .collect(Collectors.toList());
  }

  private List<String> getWorldNames() {
    return Bukkit.getWorlds().stream()
        .map(World::getName)
        .collect(Collectors.toList());
  }
}
