package me.matsubara.roulette.listener;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.gui.GUI;
import me.matsubara.roulette.gui.GUIHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public final class InventoryClose implements Listener {

    private final Roulette plugin;

    public InventoryClose(Roulette plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        // If the entity who close the inventory isn't a player, return.
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();

        // If the closed inventory holder isn't an instance of GUIHolder, return.
        if (!(event.getInventory().getHolder() instanceof GUIHolder)) return;

        // Check if the playe belongs to a game, else return.
        Game game = plugin.getGames().getGameByPlayer(player);
        if (game == null) return;

        // If the player already selected a chip, return.
        if (game.getSelected().containsKey(player.getUniqueId())) return;

        // Open the GUI until the player make a selection or leave the game.
        plugin.getServer().getScheduler().runTask(plugin, () -> new GUI(plugin, player));
    }
}