package fr.kazotaruumc72.etherealportals.manager;

import fr.kazotaruumc72.etherealportals.model.Portal;
import fr.kazotaruumc72.etherealportals.model.PortalGroup;
import fr.kazotaruumc72.etherealportals.model.PortalIcon;
import java.io.File;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles YAML persistence for groups and icons.
 */
public class DataManager {
  private final JavaPlugin plugin;
  private final PortalManager portalManager;
  private final IconManager iconManager;
  private File groupsFile;
  private File iconsFile;
  private FileConfiguration groupsCfg;
  private FileConfiguration iconsCfg;

  /**
   * Creates a new data manager.
   *
   * @param plugin the plugin instance
   * @param portalManager the portal manager
   * @param iconManager the icon manager
   */
  public DataManager(JavaPlugin plugin, PortalManager portalManager, IconManager iconManager) {
    this.plugin = plugin;
    this.portalManager = portalManager;
    this.iconManager = iconManager;
    init();
  }

  private void init() {
    if (!plugin.getDataFolder().exists()) {
      if (!plugin.getDataFolder().mkdirs()) {
        plugin.getLogger().warning("Failed to create plugin data folder");
      }
    }
    groupsFile = new File(plugin.getDataFolder(), "groups.yml");
    iconsFile = new File(plugin.getDataFolder(), "icons.yml");
    if (!groupsFile.exists()) {
      try {
        if (!groupsFile.createNewFile()) {
          plugin.getLogger().warning("Failed to create groups.yml");
        }
      } catch (IOException e) {
        plugin.getLogger().severe("IOException creating groups.yml: " + e.getMessage());
      }
    }
    if (!iconsFile.exists()) {
      try {
        if (!iconsFile.createNewFile()) {
          plugin.getLogger().warning("Failed to create icons.yml");
        }
      } catch (IOException e) {
        plugin.getLogger().severe("IOException creating icons.yml: " + e.getMessage());
      }
    }
    groupsCfg = YamlConfiguration.loadConfiguration(groupsFile);
    iconsCfg = YamlConfiguration.loadConfiguration(iconsFile);
    loadGroups();
    loadIcons();
  }

  private void loadGroups() {
    for (String groupName : groupsCfg.getKeys(false)) {
      PortalGroup group = portalManager.createGroupIfAbsent(groupName);
      ConfigurationSection groupSection = groupsCfg.getConfigurationSection(groupName);
      if (groupSection == null) {
        continue;
      }
      for (String portalName : groupSection.getKeys(false)) {
        String path = groupName + "." + portalName;
        String worldName = groupsCfg.getString(path + ".world");
        if (worldName == null) {
          continue;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
          continue; // skip
        }
        ConfigurationSection portalSection = groupsCfg.getConfigurationSection(path);
        if (portalSection == null) {
          continue;
        }
        Portal portal = Portal.deserialize(portalName, portalSection, world);
        group.addPortal(portal);
      }
    }
  }

  private void loadIcons() {
    for (String iconName : iconsCfg.getKeys(false)) {
      String base64 = iconsCfg.getString(iconName + ".base64");
      if (base64 != null) {
        iconManager.addIcon(iconName, base64);
      }
    }
  }

  /**
   * Saves all portal groups to disk.
   */
  public void saveGroups() {
    FileConfiguration tmp = new YamlConfiguration();
    for (PortalGroup group : portalManager.getGroups()) {
      String groupName = group.getName();
      for (Portal portal : group.getPortals()) {
        String path = groupName + "." + portal.getName();
        portal.serialize(tmp.createSection(path));
      }
    }
    groupsCfg = tmp;
    try {
      groupsCfg.save(groupsFile);
    } catch (IOException e) {
      plugin.getLogger().severe("Failed to save groups.yml: " + e.getMessage());
    }
  }

  /**
   * Saves all custom icons to disk.
   */
  public void saveIcons() {
    FileConfiguration tmp = new YamlConfiguration();
    for (PortalIcon icon : iconManager.getIcons()) {
      String path = icon.getName();
      tmp.set(path + ".base64", icon.getBase64());
    }
    iconsCfg = tmp;
    try {
      iconsCfg.save(iconsFile);
    } catch (IOException e) {
      plugin.getLogger().severe("Failed to save icons.yml: " + e.getMessage());
    }
  }
}
