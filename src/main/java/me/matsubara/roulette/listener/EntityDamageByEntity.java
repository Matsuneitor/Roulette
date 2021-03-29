package me.matsubara.roulette.listener;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.game.Game;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class EntityDamageByEntity implements Listener {

    private final Roulette plugin;

    public EntityDamageByEntity(Roulette plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ArmorStand)) return;

        ArmorStand stand = (ArmorStand) event.getEntity();

        PersistentDataContainer container = stand.getPersistentDataContainer();

        // Check if the vehicle has our identity key.
        NamespacedKey key = new NamespacedKey(plugin, "fromRoulette");
        if (!container.has(key, PersistentDataType.STRING)) return;

        // Check if the game exists.
        Game game = plugin.getGames().getGameByName(container.get(key, PersistentDataType.STRING));
        if (game == null) return;

        event.setCancelled(true);
    }
}