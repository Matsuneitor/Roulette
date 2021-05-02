package me.matsubara.roulette.listener.clearlag;

import me.matsubara.roulette.Roulette;
import me.minebuilders.clearlag.events.EntityRemoveEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;

public final class EntityRemove implements Listener {

    private final Roulette plugin;

    public EntityRemove(Roulette plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityRemove(EntityRemoveEvent event) {
        NamespacedKey key = new NamespacedKey(plugin, "fromRoulette");
        event.getEntityList().removeIf(entity -> entity.getPersistentDataContainer().has(key, PersistentDataType.STRING));
    }
}