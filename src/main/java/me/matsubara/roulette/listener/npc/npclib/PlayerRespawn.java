package me.matsubara.roulette.listener.npc.npclib;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.game.Game;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class PlayerRespawn implements Listener {

    private final Roulette plugin;

    public PlayerRespawn(Roulette plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Show NPC's to the player.
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            for (Game game : plugin.getGames().getGamesSet()) {
                game.getNPC().show(event.getPlayer());
            }
        }, 20L);
    }
}