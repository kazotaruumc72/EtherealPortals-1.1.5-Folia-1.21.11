package fr.kazotaruumc72.etherealportals.listener;

import fr.kazotaruumc72.etherealportals.EtherealPortals;
import fr.kazotaruumc72.etherealportals.manager.CooldownManager;
import fr.kazotaruumc72.etherealportals.manager.IconManager;
import fr.kazotaruumc72.etherealportals.manager.PortalManager;
import fr.kazotaruumc72.etherealportals.model.Portal;
import fr.kazotaruumc72.etherealportals.model.PortalGroup;
import fr.kazotaruumc72.etherealportals.model.PortalIcon;
import fr.kazotaruumc72.etherealportals.util.MessageUtils;
import fr.kazotaruumc72.etherealportals.util.SkullUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Detects portal entry when player moves.
 * Folia-compatible version using RegionScheduler and async teleportation.
 */
public class PlayerMoveListener implements Listener {
    private final EtherealPortals plugin;
    private final Set<UUID> insidePortal = new HashSet<>();

    /**
     * Creates a new player move listener.
     *
     * @param plugin the plugin instance
     */
    public PlayerMoveListener(EtherealPortals plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles player movement events to detect portal entry.
     * Safe for Folia's region-based threading.
     *
     * @param event the player move event
     */
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PortalManager pm = plugin.getPortalManager();
        PortalManager.PortalResult result = pm.findPortalAt(event.getTo(),
                plugin.getHitboxWidth(), plugin.getHitboxDepth(), plugin.getHitboxHeight());
        UUID uuid = player.getUniqueId();

        if (result != null) {
            if (!insidePortal.contains(uuid)) {
                insidePortal.add(uuid);
                handlePortalEnter(player, result.getPortal(), result.getGroup());
            }
        } else {
            insidePortal.remove(uuid);
        }
    }

    /**
     * Handles portal entry logic.
     * Routes to either direct teleport (2 portals) or selection GUI (3+ portals).
     *
     * @param player the player entering the portal
     * @param source the source portal
     * @param group the portal group
     */
    private void handlePortalEnter(Player player, Portal source, PortalGroup group) {
        if (group == null) {
            return;
        }

        int count = group.getPortals().size();
        if (count == 2) {
            // Direct teleport to other portal
            Portal target = group.getPortals().stream()
                    .filter(p -> !p.getName().equals(source.getName()))
                    .findFirst()
                    .orElse(null);
            if (target != null) {
                teleport(player, target);
            }
        } else if (count >= 3) {
            // Open selection GUI for multiple portals
            openSelectionInventory(player, group, source);
        }
    }

    /**
     * Opens a portal selection inventory for players to choose their destination.
     *
     * @param player the player
     * @param group the portal group
     * @param source the source portal (excluded from selection)
     */
    private void openSelectionInventory(Player player, PortalGroup group, Portal source) {
        int options = group.getPortals().size() - 1;
        int rows = Math.min(6, Math.max(1, (int) Math.ceil(options / 9.0)));
        int size = rows * 9;
        String title = ChatColor.DARK_PURPLE + "Select Portal";
        Inventory inv = Bukkit.createInventory(player, size, title);
        IconManager im = plugin.getIconManager();

        group.getPortals().stream()
                .filter(p -> !p.getName().equals(source.getName()))
                .forEach(portal -> {
                    // Create nice display name (e.g., "Home #1" instead of just "1")
                    String displayName = formatPortalDisplayName(group.getName(), portal.getName());
                    String coloredDisplayName = ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + displayName;

                    ItemStack item;
                    String iconName = portal.getIconName();
                    if (iconName != null) {
                        PortalIcon icon = im.getIcon(iconName);
                        if (icon != null) {
                            item = SkullUtils.createHead(icon.getBase64(), coloredDisplayName);
                        } else {
                            item = SkullUtils.createHead(plugin.getDefaultPortalTexture(), coloredDisplayName);
                        }
                    } else {
                        item = SkullUtils.createHead(plugin.getDefaultPortalTexture(), coloredDisplayName);
                    }

                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        if (!meta.hasDisplayName()) {
                            meta.setDisplayName(coloredDisplayName);
                        }

                        // Store group and portal name in NBT for reliable lookup
                        NamespacedKey groupKey = new NamespacedKey(plugin, "portal_group");
                        NamespacedKey portalKey = new NamespacedKey(plugin, "portal_name");
                        meta.getPersistentDataContainer().set(groupKey, PersistentDataType.STRING,
                                group.getName());
                        meta.getPersistentDataContainer().set(portalKey, PersistentDataType.STRING,
                                portal.getName());

                        // Add lore with location info
                        List<String> lore = new ArrayList<>();
                        lore.add(" ");
                        lore.add(ChatColor.GRAY + MessageUtils.formatCoords(portal.getBaseLocation()));
                        lore.add(ChatColor.GRAY + portal.getBaseLocation().getWorld().getName());
                        lore.add(" ");
                        lore.add(ChatColor.GREEN + "Click to teleport!");
                        meta.setLore(lore);

                        if (!item.setItemMeta(meta)) {
                            plugin.getLogger().warning(
                                    "Failed to set item meta for portal: " + portal.getName());
                        }
                    }
                    inv.addItem(item);
                });

        player.openInventory(inv);
    }

    /**
     * Formats a portal name for display in the GUI.
     * For player-created portals (e.g., "playername:home" with portal "1"),
     * returns "Home #1".
     * For command-created portals, returns the portal name as-is.
     *
     * @param groupName the full group name
     * @param portalName the portal name (usually a number for player-created portals)
     * @return the formatted display name
     */
    private String formatPortalDisplayName(String groupName, String portalName) {
        // Check if this is a player-created portal (format: "playername:basename")
        int colonIndex = groupName.indexOf(':');
        if (colonIndex != -1 && colonIndex < groupName.length() - 1) {
            // Extract base name after colon
            String baseName = groupName.substring(colonIndex + 1);
            // Capitalize first letter
            if (!baseName.isEmpty()) {
                baseName = Character.toUpperCase(baseName.charAt(0)) + baseName.substring(1);
            }
            // Return formatted name like "Home #1"
            return baseName + " #" + portalName;
        }
        // Command-created portal: return portal name as-is
        return portalName;
    }

    /**
     * Teleports a player to the target portal with animation and sound.
     * Uses Folia-compatible async teleportation with RegionScheduler for delayed execution.
     *
     * @param player the player to teleport
     * @param target the target portal
     */
    private void teleport(Player player, Portal target) {
        CooldownManager cm = plugin.getCooldownManager();

        // Check cooldown
        if (!cm.canTeleport(player.getUniqueId())) {
            if (cm.canMessage(player.getUniqueId())) {
                MessageUtils.cooldown(player, cm.remainingTeleport(player.getUniqueId()));
                cm.triggerMessage(player.getUniqueId());
            }
            return;
        }

        // Burst animation and sound at source location
        Location sourceLoc = player.getLocation();
        sourceLoc.getWorld().spawnParticle(Particle.PORTAL,
                sourceLoc, 40, 0.5, 0.5, 0.5, 0.2);
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
                        targetLoc.getWorld().spawnParticle(Particle.PORTAL,
                                targetLoc, 50, 0.5, 0.5, 0.5, 0.25);
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