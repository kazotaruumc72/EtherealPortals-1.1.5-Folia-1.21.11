package fr.kazotaruumc72.etherealportals.listener;

import fr.kazotaruumc72.etherealportals.EtherealPortals;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class RecipeDiscoveryListener implements Listener {

    private final EtherealPortals plugin;

    public RecipeDiscoveryListener(EtherealPortals plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // La clé doit correspondre EXACTEMENT à celle définie dans EtherealPortals.java
        NamespacedKey recipeKey = new NamespacedKey(plugin, "portal_crystal");
        
        // Débloque la recette pour le joueur
        event.getPlayer().discoverRecipe(recipeKey);
    }
}
