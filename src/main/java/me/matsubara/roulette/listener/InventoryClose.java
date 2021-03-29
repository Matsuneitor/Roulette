package me.matsubara.roulette.listener;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.game.GameState;
import me.matsubara.roulette.gui.GUI;
import me.matsubara.roulette.gui.GUIHolder;
import me.matsubara.roulette.gui.GUIType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

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

        GUIHolder holder = getHolder(event.getInventory());

        // If the closed inventory holder isn't an instance of GUIHolder, return.
        if (holder == null) return;

        // If the GUIHolder type is MAIN, return; we don't need to reopen any inventory.
        if (holder.getType() == GUIType.MAIN) return;

        Game game = holder.getGame();
        if (game == null) return;
        if (game.getState() != GameState.SELECTING) return;

        // If the player already selected a chip, return.
        if (game.getChips().containsKey(player.getUniqueId())) return;
        //if (game.getSelected().containsKey(player.getUniqueId())) return;

        // If the GUIHolder type is CONFIRM, reopen the chip inventory.
        if (holder.getType() == GUIType.CONFIRM) {
            plugin.getServer().getScheduler().runTask(plugin, () -> new GUI(plugin, player, game));
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                GUIHolder holder = getHolder(player.getOpenInventory().getTopInventory());
                if (holder == null) new GUI(plugin, player, game);
            }
        }.runTaskLater(plugin, 2L);
    }

    private GUIHolder getHolder(Inventory inventory) {
        if (!(inventory.getHolder() instanceof GUIHolder)) return null;
        return (GUIHolder) inventory.getHolder();
    }
}