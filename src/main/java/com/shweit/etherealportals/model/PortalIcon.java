package fr.kazotaruumc72.etherealportals.model;

/**
 * Represents a custom skull icon definition (base64 texture value).
 */
public class PortalIcon {
  private final String name;
  private final String base64;

  /**
   * Creates a new portal icon.
   *
   * @param name the icon name
   * @param base64 the base64-encoded texture
   */
  public PortalIcon(String name, String base64) {
    this.name = name.toLowerCase();
    this.base64 = base64;
  }

  public String getName() {
    return name;
  }

  public String getBase64() {
    return base64;
  }
}
