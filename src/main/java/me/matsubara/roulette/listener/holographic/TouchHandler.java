package me.matsubara.roulette.listener.holographic;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.util.RUtilities;
import org.bukkit.entity.Player;

public final class TouchHandler implements com.gmail.filoghost.holographicdisplays.api.handler.TouchHandler {

    private final Roulette plugin;
    private final Game game;

    public TouchHandler(Roulette plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
    }

    @Override
    public void onTouch(Player player) {
        // If the player is already in game, return. (Shouldn't happen)
        if (game.inGame(player)) {
            RUtilities.handleMessage(player, plugin.getMessages().getAlreadyInGame());
            return;
        }

        // If the game already started, return. (Shouldn't happen)
        if (!game.getState().isWaiting() && !game.getState().isCountdown()) {
            RUtilities.handleMessage(player, plugin.getMessages().getAlreadyStarted());
            return;
        }

        double min = plugin.getChips().getMinAmount();

        // If the player doesn't have the minimum amount of money required, return.
        if (!plugin.getEconomy().has(player, min)) {
            RUtilities.handleMessage(player, plugin.getMessages().getMinRequired(plugin.getEconomy().format(min)));
            return;
        }

        // Add player to the game and announce it.
        game.addPlayer(player);
        game.broadcast(plugin.getMessages().getJoinMessage(player.getName(), game.size(), game.getMaxPlayers()));
    }
}