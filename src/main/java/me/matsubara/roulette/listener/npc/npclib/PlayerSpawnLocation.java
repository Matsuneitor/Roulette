package me.matsubara.roulette.listener.npc.npclib;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.game.Game;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

public final class PlayerSpawnLocation implements Listener {

    private final Roulette plugin;

    public PlayerSpawnLocation(Roulette plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSpawnLocation(PlayerSpawnLocationEvent event) {
        // Show NPC's to the player.
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            for (Game game : plugin.getGames().getGamesSet()) {
                game.getNPC().show(event.getPlayer());
            }
        }, 20L);
    }
}