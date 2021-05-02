package me.matsubara.roulette.listener;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.event.PlayerJumpEvent;
import me.matsubara.roulette.game.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class PlayerJump implements Listener {

    private final Roulette plugin;

    public PlayerJump(Roulette plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();

        for (Game game : plugin.getGames().getGamesSet()) {
            // Maybe the NPC isn't created yet.
            if (game.getNPC() == null) continue;

            // If the game already started, don't jump.
            if (!game.getState().isWaiting() && !game.getState().isCountdown()) continue;

            Player target = game.getNPC().getTarget();
            if (target == null) continue;
            if (!target.equals(player)) continue;

            game.getNPC().jump();
        }
    }
}