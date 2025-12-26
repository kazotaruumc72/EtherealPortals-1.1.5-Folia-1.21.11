package fr.kazotaruumc72.etherealportals.listener;

import fr.kazotaruumc72.etherealportals.EtherealPortals;
import fr.kazotaruumc72.etherealportals.manager.CooldownManager;
import fr.kazotaruumc72.etherealportals.manager.PortalManager;
import fr.kazotaruumc72.etherealportals.model.Portal;
import fr.kazotaruumc72.etherealportals.model.PortalGroup;
import fr.kazotaruumc72.etherealportals.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handles inventory click events for plugin GUIs.
 * Folia-compatible version using RegionScheduler and async teleportation.
 */
public class InventoryClickListener implements Listener {
    private final EtherealPortals plugin;

    /**
     * Creates a new inventory click listener.
     *
     * @param plugin the plugin instance
     */
    public InventoryClickListener(EtherealPortals plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles inventory click events for plugin GUIs.
     * Cancels clicks in Icons and Select Portal inventories.
     * Triggers teleportation when a portal is selected.
     *
     * @param event the inventory click event
     */
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        // Cancel clicks in Icons GUI
        if (title.contains("Icons")) {
            event.setCancelled(true);
            return;
        }

        // Handle clicks in Select Portal GUI
        if (title.contains("Select Portal")) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            ItemStack current = event.getCurrentItem();

            if (current == null || !current.hasItemMeta()) {
                return;
            }

            // Read group and portal name from NBT
            NamespacedKey groupKey = new NamespacedKey(plugin, "portal_group");
            NamespacedKey portalKey = new NamespacedKey(plugin, "portal_name");
            String groupName = current.getItemMeta().getPersistentDataContainer()
                    .get(groupKey, PersistentDataType.STRING);
            String portalName = current.getItemMeta().getPersistentDataContainer()
                    .get(portalKey, PersistentDataType.STRING);

            if (groupName == null || portalName == null) {
                MessageUtils.error(player, "Invalid portal item.");
                return;
            }

            // Teleport to the selected portal
            teleportByGroupAndName(player, groupName, portalName);
        }
    }

    /**
     * Teleports a player to a portal by group and portal name.
     * Uses Folia-compatible async teleportation with RegionScheduler for delayed execution.
     *
     * @param player the player to teleport
     * @param groupName the portal group name
     * @param portalName the portal name
     */
    private void teleportByGroupAndName(Player player, String groupName, String portalName) {
        PortalManager pm = plugin.getPortalManager();
        PortalGroup group = pm.getGroup(groupName);

        if (group == null) {
            MessageUtils.error(player, "Could not find portal group.");
            return;
        }

        Portal target = group.getPortal(portalName);
        if (target == null) {
            MessageUtils.error(player, "Could not find portal.");
            return;
        }

        CooldownManager cm = plugin.getCooldownManager();

        // Check cooldown
        if (!cm.canTeleport(player.getUniqueId())) {
            if (cm.canMessage(player.getUniqueId())) {
                MessageUtils.cooldown(player, cm.remainingTeleport(player.getUniqueId()));
                cm.triggerMessage(player.getUniqueId());
            }
            return;
        }

        // Close inventory and play burst animation at source
        player.closeInventory();
        Location sourceLoc = player.getLocation();
        sourceLoc.getWorld().spawnParticle(Particle.PORTAL, sourceLoc,
                40, 0.5, 0.5, 0.5, 0.2);
        sourceLoc.getWorld().playSound(sourceLoc,
                Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

        Location targetLoc = target.getCenterLocation();

        // Schedule delayed teleport using RegionScheduler
        // This ensures the task runs on the correct region thread
        Bukkit.getRegionScheduler().runDelayed(
                plugin,
                targetLoc,
                (task) -> {
                    // Teleport asynchronously (handles region crossing safely)
                    player.teleportAsync(targetLoc).thenRun(() -> {
                        // Burst animation and sound at destination
                        targetLoc.getWorld().spawnParticle(Particle.PORTAL, targetLoc,
                                50, 0.5, 0.5, 0.5, 0.25);
                        targetLoc.getWorld().playSound(targetLoc,
                                Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

                        // Send message and trigger cooldown
                        MessageUtils.teleport(player, target.getName());
                        cm.triggerTeleport(player.getUniqueId());
                    });
                },
                10L  // 0.5s delay (10 ticks)
        );
    }
}