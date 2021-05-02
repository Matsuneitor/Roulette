package me.matsubara.roulette.listener;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.game.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public final class PlayerChangedWorld implements Listener {

    private final Roulette plugin;

    public PlayerChangedWorld(Roulette plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        // Show NPC's to the player.
        for (Game game : plugin.getGames().getGamesSet()) {
            game.getNPC().show(player);
        }

        Game game = plugin.getGames().getGameByPlayer(player);
        if (game == null) return;

        game.removePlayer(player, false);
    }
}