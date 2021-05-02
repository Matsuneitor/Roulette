package me.matsubara.roulette.listener;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.game.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerJoin implements Listener {

    private final Roulette plugin;

    public PlayerJoin(Roulette plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Show NPC's to the player.
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            for (Game game : plugin.getGames().getGamesSet()) {
                game.getNPC().show(event.getPlayer());

                // Hide all personal holograms to the joining player if using CMI.
                if (!Roulette.USE_HOLOGRAPHIC) {
                    game.getHolograms().values().forEach(hologram -> hologram.hideTo(player));
                }
            }
        }, 20L);
    }
}