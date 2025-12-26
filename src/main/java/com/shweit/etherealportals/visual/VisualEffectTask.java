package fr.kazotaruumc72.etherealportals.visual;

import fr.kazotaruumc72.etherealportals.EtherealPortals;
import fr.kazotaruumc72.etherealportals.model.PortalGroup;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;

/**
 * Periodic task spawning particle spirals for portals.
 * Folia-compatible version using RegionScheduler for region-based scheduling.
 */
public class VisualEffectTask {
  private final EtherealPortals plugin;
  private volatile ScheduledTask regionTask;

  /**
   * Creates a new visual effect task.
   *
   * @param plugin the plugin instance
   */
  public VisualEffectTask(EtherealPortals plugin) {
    this.plugin = plugin;
    this.regionTask = null;
  }

  /**
   * Starts the visual effect task.
   * Uses GlobalRegionScheduler for particle spawning across all regions.
   */
  public void start() {
    if (regionTask != null) {
      return;
    }
    
    // One-time sync: create missing TextDisplays and ArmorStands for existing portals.
    // IMPORTANT: These must be scheduled per-region to avoid "Async chunk retrieval" errors on Folia.
    syncMissingTextDisplays();
    syncMissingArmorStands();
    
    // Schedule particle effects using GlobalRegionScheduler
    // This runs on a global thread, safe for cross-region particle spawning
    regionTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
        plugin,
        (task) -> {
          run();
        },
        40L,  // Initial delay
        20L   // Period (every 20 ticks = 1 second)
    );
  }

  /**
   * Stops the visual effect task.
   */
  public void stop() {
    if (regionTask != null) {
      regionTask.cancel();
      regionTask = null;
    }
  }

  /**
   * Main runnable method for particle spawning.
   * Spawns spiral particles around all portals.
   */
  private void run() {
    double t = (System.currentTimeMillis() % 5000) / 5000.0; // progress [0, 1)
    
    plugin.getPortalManager().getGroups().forEach(group ->
        group.getPortals().forEach(portal -> {
          Location center = portal.getCenterLocation().clone().add(0, 0.1, 0);
          spawnSpiral(center, t);
        })
    );
  }

  /**
   * Spawns a spiral of particles at the given location.
   *
   * @param center the center location
   * @param progress animation progress (0-1)
   */
  private void spawnSpiral(Location center, double progress) {
    if (center.getWorld() == null) {
      return;
    }
    
    for (int i = 0; i < 24; i++) {
      double angle = (progress * 2 * Math.PI) + (i * Math.PI / 6);
      double radius = 0.7;
      double y = (i / 24.0) * 2.5;
      double x = center.getX() + Math.cos(angle) * radius;
      double z = center.getZ() + Math.sin(angle) * radius;
      
      center.getWorld().spawnParticle(
          Particle.END_ROD,
          x,
          center.getY() + y,
          z,
          1,
          0, 0, 0,
          0
      );
    }
  }

  /**
   * Removes the text display and armor stand marker for a specific portal.
   * Uses RegionScheduler to ensure thread safety.
   *
   * @param groupName The name of the group
   * @param portalName The name of the portal whose display should be removed
   * @param portal The portal object containing the location
   */
  public void removeTextDisplay(String groupName, String portalName,
      fr.kazotaruumc72.etherealportals.model.Portal portal) {
    String textTag = "ep_portal:" + groupName.toLowerCase() + ":" + portalName.toLowerCase();
    String markerTag = "ep_portal_marker:" + groupName.toLowerCase()
        + ":" + portalName.toLowerCase();

    Location portalLoc = portal.getBaseLocation();
    
    if (portalLoc.getWorld() == null) {
      return;
    }

    // Schedule cleanup on the correct region thread
    Bukkit.getRegionScheduler().execute(plugin, portalLoc, () -> {
      // It is safe to check chunk loading here because we are on the region thread
      if (!portalLoc.isChunkLoaded()) {
        // If chunk isn't loaded, entities aren't active, so we might not need to do anything.
        // Or we can load it if strictly necessary, but usually we skip unloaded chunks for cleanup
        // to save performance unless we are sure.
        return; 
      }

      // Remove TextDisplay entities with matching tag
      portalLoc.getWorld().getEntitiesByClass(TextDisplay.class).stream()
          .filter(td -> td.getScoreboardTags().contains(textTag))
          .forEach(Entity::remove);
      
      // Remove ArmorStand marker entities with matching tag
      portalLoc.getWorld().getEntitiesByClass(ArmorStand.class).stream()
          .filter(as -> as.getScoreboardTags().contains(markerTag))
          .forEach(Entity::remove);
    });
  }

  /**
   * Removes all text displays for all portals in a group.
   *
   * @param group The portal group whose displays should be removed
   */
  public void removeGroupTextDisplays(PortalGroup group) {
    group.getPortals().forEach(portal ->
        removeTextDisplay(group.getName(), portal.getName(), portal)
    );
  }

  /**
   * Creates a text display for a newly added portal.
   * Called when a portal is added via command.
   *
   * @param loc The location for the text display
   * @param groupName The portal group name
   * @param portalName The portal name
   */
  public void createTextDisplay(Location loc, String groupName, String portalName) {
    createTextDisplay(loc, groupName, portalName, portalName);
  }

  /**
   * Creates a text display for a newly added portal with custom display text.
   * Uses RegionScheduler to ensure the entity is created on the correct region thread.
   *
   * @param loc The location for the text display
   * @param groupName The portal group name
   * @param portalName The portal name (used for tag)
   * @param displayText The text to display above the portal
   */
  public void createTextDisplay(Location loc, String groupName,
      String portalName, String displayText) {
    
    if (loc.getWorld() == null) {
      return;
    }

    String tag = "ep_portal:" + groupName.toLowerCase() + ":" + portalName.toLowerCase();
    
    // Schedule entity creation on the correct region thread
    Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
      loc.getWorld().spawn(loc, TextDisplay.class, d -> {
        d.text(Component.text(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + displayText));
        d.setBillboard(Display.Billboard.CENTER);
        d.setSeeThrough(true);
        d.setShadowed(true);
        d.setViewRange(10);
        d.addScoreboardTag(tag);
      });
    });
  }

  /**
   * Syncs missing TextDisplays for existing portals on startup.
   * FIXED for Folia: Schedules each check on the correct RegionScheduler thread.
   */
  private void syncMissingTextDisplays() {
    for (PortalGroup group : plugin.getPortalManager().getGroups()) {
      for (fr.kazotaruumc72.etherealportals.model.Portal portal : group.getPortals()) {
        Location loc = portal.getBaseLocation().clone().add(0.5, 3, 0.5);
        
        if (loc.getWorld() == null) {
          continue;
        }

        String tag = "ep_portal:" + group.getName().toLowerCase()
            + ":" + portal.getName().toLowerCase();

        // CRITICAL FIX: Schedule on the Region thread.
        // We cannot call getChunk() or isChunkLoaded() from onEnable directly.
        Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
            // Now we are on the correct thread.
            // If the chunk is unloaded, we generally don't want to force load it just for a visual check
            // unless we really need to.
            if (!loc.isChunkLoaded()) {
                // Optional: loc.getChunk().load(); if you absolutely must spawn it now.
                // For performance, we can skip spawning if no one is there to see it.
                // But to be safe and consistent, let's load it if we want to ensure it exists.
                // Or better: just spawn if loaded. 
                // Let's force load to ensure persistence as per original logic, but safely.
                loc.getChunk().load(); 
            }

            // Check if TextDisplay already exists
            boolean exists = loc.getWorld().getEntitiesByClass(TextDisplay.class).stream()
                .anyMatch(td -> td.getScoreboardTags().contains(tag));

            if (!exists) {
              loc.getWorld().spawn(loc, TextDisplay.class, d -> {
                d.text(Component.text(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + portal.getName()));
                d.setBillboard(Display.Billboard.CENTER);
                d.setSeeThrough(true);
                d.setShadowed(true);
                d.setViewRange(10);
                d.addScoreboardTag(tag);
              });
              plugin.getLogger().info("Created missing TextDisplay for " + portal.getName());
            }
        });
      }
    }
  }

  /**
   * Creates an invisible armor stand marker at the portal center.
   *
   * @param loc The center location for the armor stand
   * @param groupName The portal group name
   * @param portalName The portal name
   */
  public void createArmorStandMarker(Location loc, String groupName, String portalName) {
    
    if (loc.getWorld() == null) {
      return;
    }

    String tag = "ep_portal_marker:" + groupName.toLowerCase() + ":" + portalName.toLowerCase();
    
    Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
      loc.getWorld().spawn(loc, ArmorStand.class, as -> {
        as.setVisible(false);
        as.setSmall(false);
        as.setGravity(false);
        as.setMarker(false);
        as.setInvulnerable(false);
        as.setCustomName(ChatColor.GRAY + "(Portal - Punch to break)");
        as.setCustomNameVisible(false);
        as.addScoreboardTag(tag);
      });
    });
  }

  /**
   * Syncs missing ArmorStand markers for existing breakable portals on startup.
   * FIXED for Folia: Schedules each check on the correct RegionScheduler thread.
   */
  private void syncMissingArmorStands() {
    for (PortalGroup group : plugin.getPortalManager().getGroups()) {
      for (fr.kazotaruumc72.etherealportals.model.Portal portal : group.getPortals()) {
        if (!portal.isBreakable()) {
          continue;
        }

        Location loc = portal.getCenterLocation();
        
        if (loc.getWorld() == null) {
          continue;
        }

        String tag = "ep_portal_marker:" + group.getName().toLowerCase()
            + ":" + portal.getName().toLowerCase();

        // CRITICAL FIX: Schedule on the Region thread.
        Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
            if (!loc.isChunkLoaded()) {
                loc.getChunk().load();
            }

            boolean exists = loc.getWorld().getEntitiesByClass(ArmorStand.class).stream()
                .anyMatch(as -> as.getScoreboardTags().contains(tag));

            if (!exists) {
              loc.getWorld().spawn(loc, ArmorStand.class, as -> {
                as.setVisible(false);
                as.setSmall(false);
                as.setGravity(false);
                as.setMarker(false);
                as.setInvulnerable(false);
                as.setCustomName(ChatColor.GRAY + "(Portal - Punch to break)");
                as.setCustomNameVisible(false);
                as.addScoreboardTag(tag);
              });
              plugin.getLogger().info("Created missing ArmorStand for " + portal.getName());
            }
        });
      }
    }
  }
}
