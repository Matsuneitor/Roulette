package me.matsubara.roulette.listener;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.util.RUtilities;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.persistence.PersistentDataType;

public final class PlayerArmorStandManipulate implements Listener {

    private final Roulette plugin;

    public PlayerArmorStandManipulate(Roulette plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        ArmorStand armorStand = event.getRightClicked();

        // Check if vehicle has our identity key, else return.
        NamespacedKey key = new NamespacedKey(plugin, "fromRoulette");
        if (!armorStand.getPersistentDataContainer().has(key, PersistentDataType.STRING)) return;

        // Check if the game exists, if not, maybe isn't done yet.
        Game game = plugin.getGames().getGameByName(armorStand.getPersistentDataContainer().get(key, PersistentDataType.STRING));

        Player player = event.getPlayer();

        // If the player is sneaking and has admin permissions, delete the game.
        if (game != null && game.isDone() && player.isSneaking() && player.hasPermission("roulette.admin")) {
            game.delete(true);
            plugin.getGames().deleteGame(game);
            RUtilities.handleMessage(player, plugin.getMessages().getDelete(game.getName()));
        }
        event.setCancelled(true);
    }
}