package me.matsubara.roulette.listener;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.game.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerQuit implements Listener {

    private final Roulette plugin;

    public PlayerQuit(Roulette plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        Game.CREATING.remove(player.getUniqueId());
        Game game = plugin.getGames().getGameByPlayer(player);
        if (game == null) return;

        game.removePlayer(player, false);
    }
}