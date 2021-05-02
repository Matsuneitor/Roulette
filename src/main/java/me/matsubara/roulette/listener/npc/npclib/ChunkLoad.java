package me.matsubara.roulette.listener.npc.npclib;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.game.Game;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public final class ChunkLoad implements Listener {

    private final Roulette plugin;

    public ChunkLoad(Roulette plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        // Show NPC's to the player.
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            for (Entity entity : event.getChunk().getEntities()) {
                if (entity.getType() != EntityType.PLAYER) continue;
                for (Game game : plugin.getGames().getGamesSet()) {
                    game.getNPC().show((Player) entity);
                }
            }
        }, 20L);
    }
}