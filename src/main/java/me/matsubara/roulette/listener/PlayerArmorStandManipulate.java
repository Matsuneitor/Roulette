package me.matsubara.roulette.listener;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.file.Messages;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.util.RUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class PlayerArmorStandManipulate implements Listener {

    private final Roulette plugin;

    public PlayerArmorStandManipulate(Roulette plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        ArmorStand stand = event.getRightClicked();

        PersistentDataContainer container = stand.getPersistentDataContainer();

        // Check if the vehicle has our identity key.
        NamespacedKey key = new NamespacedKey(plugin, "fromRoulette");
        if (!container.has(key, PersistentDataType.STRING)) return;

        // Check if the game exists.
        Game game = plugin.getGames().getGameByName(container.get(key, PersistentDataType.STRING));

        Player player = event.getPlayer();

        // If the player is sneaking and has admin permissions, delete the game.
        if (game != null && game.isDone() && player.isSneaking() && player.hasPermission("roulette.delete")) {
            if (!game.getData().getCreator().equals(player.getUniqueId()) && !player.hasPermission("roulette.delete.others")) {
                RUtils.handleMessage(player, Messages.Message.NOT_PERMISSION.asString());
                return;
            }
            game.delete(true, false, false);
            RUtils.handleMessage(player, Messages.Message.DELETE.asString().replace("%name%", game.getName()));
        }
        event.setCancelled(true);
    }
}