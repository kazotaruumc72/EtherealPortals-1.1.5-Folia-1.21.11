package fr.kazotaruumc72.etherealportals.listener;

import fr.kazotaruumc72.etherealportals.EtherealPortals;
import fr.kazotaruumc72.etherealportals.manager.PortalManager;
import fr.kazotaruumc72.etherealportals.model.Portal;
import fr.kazotaruumc72.etherealportals.model.PortalGroup;
import fr.kazotaruumc72.etherealportals.util.MessageUtils;
import fr.kazotaruumc72.etherealportals.util.PortalItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener for portal item placement and breaking.
 */
public class PortalItemListener implements Listener {
  private final EtherealPortals plugin;

  /**
   * Creates a new portal item listener.
   *
   * @param plugin the plugin instance
   */
  public PortalItemListener(EtherealPortals plugin) {
    this.plugin = plugin;
  }

  /**
   * Handles portal item placement.
   */
  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    // Only handle right-click on block (placement)
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }

    ItemStack item = event.getItem();

    // Check if item is a portal item
    if (!PortalItemUtils.isPortalItem(plugin, item)) {
      return;
    }

    event.setCancelled(true);

    Player player = event.getPlayer();

    // Check if craftable portals are enabled
    if (!plugin.isCraftablePortalsEnabled()) {
      MessageUtils.error(player, "Craftable portals are disabled on this server.");
      return;
    }

    // Check permission
    if (!player.hasPermission("portal.item.place")) {
      MessageUtils.error(player, "You don't have permission to place portal items.");
      return;
    }

    // Get clicked block location (+1 block up so players don't teleport into the ground)
    org.bukkit.block.Block clickedBlock = event.getClickedBlock();
    if (clickedBlock == null) {
      return;
    }
    Location loc = clickedBlock.getLocation().add(0, 1, 0);

    // Extract group name from item display name
    String displayName = item.getItemMeta().getDisplayName();
    String stripped = ChatColor.stripColor(displayName);
    String groupBaseName;
    if (stripped == null || stripped.isEmpty()) {
      groupBaseName = "portals";
    } else {
      groupBaseName = stripped.replace(":", "")
        .replace(" ", "_").trim(); // Remove colons, replace spaces
      if (groupBaseName.isEmpty()) {
        groupBaseName = "portals";
      }
    }

    // Create group name: playername:groupBaseName
    String groupName = player.getName().toLowerCase() + ":" + groupBaseName.toLowerCase();

    // Generate unique portal name by finding the highest existing number
    // This ensures we don't reuse numbers when portals are deleted
    PortalManager pm = plugin.getPortalManager();
    PortalGroup group = pm.getGroup(groupName);
    int maxNumber = 0;
    if (group != null) {
      for (Portal p : group.getPortals()) {
        try {
          int num = Integer.parseInt(p.getName());
          if (num > maxNumber) {
            maxNumber = num;
          }
        } catch (NumberFormatException e) {
          // Portal name is not a number, skip
        }
      }
    }
    String portalName = String.valueOf(maxNumber + 1);

    // Try to add portal
    boolean added = pm.addPortal(groupName, portalName, loc, null, true);

    if (!added) {
      MessageUtils.error(player, "Failed to create portal. Please try again.");
      return;
    }

    // Save groups
    plugin.getDataManager().saveGroups();

    // Spawn visual effects (use display name like "Home #1" for better UX)
    Location textLoc = loc.clone().add(0.5, 3, 0.5);
    Location markerLoc = loc.clone().add(0.5, 0, 0.5);
    String displayNameForText = groupBaseName + " #" + portalName;
    plugin.getVisualTask().createTextDisplay(textLoc, groupName, portalName, displayNameForText);
    plugin.getVisualTask().createArmorStandMarker(markerLoc, groupName, portalName);

    // Spawn particles at placement
    loc.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(0.5, 0.5, 0.5),
        40, 0.3, 0.3, 0.3, 0.5);

    // Remove item from hand
    item.setAmount(item.getAmount() - 1);

    // Success message with group info
    int totalPortals = pm.getGroup(groupName).getPortals().size();
    MessageUtils.success(player,
        "Portal &d#" + portalName + "&a created in group &d" + groupBaseName
        + "&a! (" + totalPortals + " total)");
  }

  /**
   * Handles armor stand breaking (portal removal).
   */
  @EventHandler(priority = org.bukkit.event.EventPriority.HIGH)
  public void onEntityDamage(EntityDamageByEntityEvent event) {
    // Check if entity is an armor stand
    if (!(event.getEntity() instanceof ArmorStand)) {
      return;
    }

    // Check if damager is a player
    if (!(event.getDamager() instanceof Player)) {
      return;
    }

    ArmorStand armorStand = (ArmorStand) event.getEntity();

    // Check if armor stand has portal marker tag
    String tag = armorStand.getScoreboardTags().stream()
        .filter(t -> t.startsWith("ep_portal_marker:"))
        .findFirst()
        .orElse(null);

    if (tag == null) {
      return;
    }

    event.setCancelled(true);

    // Parse group and portal name from tag: ep_portal_marker:<group>:<portal>
    // The group name may contain colons (e.g., "playername:home"), so we split at the LAST colon
    String remaining = tag.substring("ep_portal_marker:".length());
    int lastColon = remaining.lastIndexOf(':');
    if (lastColon == -1) {
      plugin.getLogger().warning("Invalid portal marker tag format: " + tag);
      return;
    }

    String groupName = remaining.substring(0, lastColon);
    String portalName = remaining.substring(lastColon + 1);

    // Find portal
    PortalManager pm = plugin.getPortalManager();
    PortalGroup group = pm.getGroup(groupName);
    if (group == null) {
      return;
    }

    Portal portal = group.getPortal(portalName);
    if (portal == null) {
      return;
    }

    Player player = (Player) event.getDamager();

    // Check if portal is breakable
    if (!portal.isBreakable()) {
      MessageUtils.error(player, "This portal cannot be broken.");
      return;
    }

    // Check permission
    if (!player.hasPermission("portal.item.break")) {
      MessageUtils.error(player, "You don't have permission to break portal items.");
      return;
    }

    // Remove visual entities first (before removing portal object)
    plugin.getVisualTask().removeTextDisplay(groupName, portalName, portal);

    // Remove portal from group
    group.removePortal(portalName);

    // Remove group if empty
    if (group.getPortals().isEmpty()) {
      pm.deleteGroup(groupName);
    }

    // Save groups
    plugin.getDataManager().saveGroups();

    // Drop portal item
    ItemStack droppedItem = PortalItemUtils.createPortalItem(
        plugin,
        plugin.getDefaultPortalTexture(),
        plugin.getPortalItemName(),
        plugin.getPortalItemLore()
    );
    armorStand.getWorld().dropItemNaturally(armorStand.getLocation(), droppedItem);

    // Spawn particles
    armorStand.getWorld().spawnParticle(Particle.PORTAL, armorStand.getLocation(),
        40, 0.3, 0.3, 0.3, 0.5);

    // Success message
    MessageUtils.success(player, "Portal &d" + portal.getName() + "&a removed!");
  }
}
