package me.matsubara.roulette.listener.holographic;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.file.Messages;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.util.RUtils;
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
            RUtils.handleMessage(player, Messages.Message.ALREADY_INGAME.asString());
            return;
        }

        // If the game already started, return. (Shouldn't happen)
        if (!game.getState().isWaiting() && !game.getState().isCountdown()) {
            RUtils.handleMessage(player, Messages.Message.ALREADY_STARTED.asString());
            return;
        }

        Double minAmount = plugin.getChips().getMinAmount();
        if (minAmount == null) return;

        // If the player doesn't have the minimum amount of money required, return.
        if (!plugin.getEconomy().has(player, minAmount)) {
            RUtils.handleMessage(player, Messages.Message.MIN_REQUIRED.asString().replace("%money%", String.valueOf(minAmount)));
            return;
        }

        // Add player to the game and announce it.
        game.addPlayer(player);
        game.broadcast(plugin.getMessages().getJoinMessage(player.getName(), game.size(), game.getMaxPlayers()));
    }
}