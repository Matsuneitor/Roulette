package me.matsubara.roulette.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.data.Slot;
import me.matsubara.roulette.file.winner.Winner;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.util.RUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class RoulettePlaceholder extends PlaceholderExpansion {

    private final Roulette plugin;

    public RoulettePlaceholder(Roulette plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "roulette";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        // If somehow the player is null, return empty.
        if (player == null) return "";

        // Split the entire parameter with underscores.
        String[] values = params.split("_");

        // We only need 1 or 2 parameters.
        if (values.length == 0 || values.length > 2) return "";

        // if parameter doesn't contain underscores.
        if (values.length == 1) {
            // Get the game by the player, can be null.
            Game game = plugin.getGames().getGameByPlayer(player);

            // Get the winner object by the player UUID, can be null.
            Winner winner = plugin.getWinners().getByUniqueId(player.getUniqueId());

            switch (params) {
                // Return the selected slot of a player in a game (%roulette_selected%).
                case "selected":
                    // If game is null, return empty.
                    if (game == null) return "";

                    // If the game didn't select a slot yet, return empty.
                    if (!game.getSelected().containsKey(player.getUniqueId())) return "";

                    Slot slot = game.getSelected().get(player.getUniqueId()).getKey();
                    return RUtils.getSlotName(slot);
                // Return the amount of wins of a player (%roulette_wins%).
                case "wins":
                    // If the winner is null, return 0.
                    if (winner == null) return "0";

                    return String.valueOf(winner.getWinnerData().size());
                // Returns all winnings of a player (%roulette_winnings%).
                case "winnings":
                    // If the winner is null, return 0.
                    if (winner == null) return "0";

                    return String.valueOf(winner.getWinnerData().stream().mapToDouble(Winner.WinnerData::getMoney).sum());
            }
        }

        // Get the game by the name using the second parameter, can be null.
        Game game = plugin.getGames().getGameByName(values[1]);

        switch (values[0]) {
            // Returns the amount of players in a game (%roulette_count_Test%).
            case "count":
                // If the game is null, return 0.
                if (game == null) return "0";

                return String.valueOf(game.size());
            // Returns the winner slot of a game (%roulette_winner_Test%).
            case "winner":
                if (game == null) return "";
                return (game.getWinner() != null) ? RUtils.getSlotName(game.getWinner()) : "";
            default:
                return "";
        }
    }
}