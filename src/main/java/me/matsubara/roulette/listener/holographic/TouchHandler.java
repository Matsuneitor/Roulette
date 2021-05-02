package me.matsubara.roulette.listener.holographic;

import me.matsubara.roulette.Roulette;
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
        if (!RUtils.canJoin(player, game)) return;

        // Add player to the game and announce it.
        game.addPlayer(player);
        game.broadcast(plugin.getMessages().getJoinMessage(player.getName(), game.size(), game.getMaxPlayers()));
    }
}