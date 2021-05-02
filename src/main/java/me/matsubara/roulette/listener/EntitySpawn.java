package me.matsubara.roulette.listener;

import me.matsubara.roulette.Roulette;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.persistence.PersistentDataType;

public final class EntitySpawn implements Listener {

    private final Roulette plugin;

    public EntitySpawn(Roulette plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntitySpawn(EntitySpawnEvent event) {
        // Prevent armor stands not spawning.
        NamespacedKey key = new NamespacedKey(plugin, "fromRoulette");
        if (event.getEntity().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            event.setCancelled(false);
        }
    }
}