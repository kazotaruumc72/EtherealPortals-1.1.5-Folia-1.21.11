package fr.kazotaruumc72.etherealportals.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks teleport and message cooldowns.
 */
public class CooldownManager {
  private final Map<UUID, Long> teleportCooldownEnds = new HashMap<>();
  private final Map<UUID, Long> messageCooldownEnds = new HashMap<>();
  private int teleportSeconds;
  private int messageSeconds;

  /**
   * Creates a new cooldown manager.
   *
   * @param teleportSeconds cooldown seconds for teleportation
   * @param messageSeconds cooldown seconds for cooldown messages
   */
  public CooldownManager(int teleportSeconds, int messageSeconds) {
    this.teleportSeconds = teleportSeconds;
    this.messageSeconds = messageSeconds;
  }

  /**
   * Updates the cooldown configuration.
   *
   * @param teleportSeconds cooldown seconds for teleportation
   * @param messageSeconds cooldown seconds for cooldown messages
   */
  public void updateConfig(int teleportSeconds, int messageSeconds) {
    this.teleportSeconds = teleportSeconds;
    this.messageSeconds = messageSeconds;
  }

  /**
   * Checks if a player can teleport.
   *
   * @param uuid the player UUID
   * @return true if the player can teleport
   */
  public boolean canTeleport(UUID uuid) {
    long now = System.currentTimeMillis();
    Long end = teleportCooldownEnds.get(uuid);
    return end == null || end <= now;
  }

  /**
   * Gets the remaining teleport cooldown in seconds.
   *
   * @param uuid the player UUID
   * @return remaining seconds
   */
  public int remainingTeleport(UUID uuid) {
    Long end = teleportCooldownEnds.get(uuid);
    if (end == null) {
      return 0;
    }
    long diff = end - System.currentTimeMillis();
    return diff <= 0 ? 0 : (int) Math.ceil(diff / 1000.0);
  }

  /**
   * Triggers a teleport cooldown for a player.
   *
   * @param uuid the player UUID
   */
  public void triggerTeleport(UUID uuid) {
    teleportCooldownEnds.put(uuid, System.currentTimeMillis() + teleportSeconds * 1000L);
  }

  /**
   * Checks if a player can receive cooldown messages.
   *
   * @param uuid the player UUID
   * @return true if the player can receive messages
   */
  public boolean canMessage(UUID uuid) {
    long now = System.currentTimeMillis();
    Long end = messageCooldownEnds.get(uuid);
    return end == null || end <= now;
  }

  /**
   * Triggers a message cooldown for a player.
   *
   * @param uuid the player UUID
   */
  public void triggerMessage(UUID uuid) {
    messageCooldownEnds.put(uuid, System.currentTimeMillis() + messageSeconds * 1000L);
  }
}
