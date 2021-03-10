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
import org.bukkit.persistence.PersistentDataType;
import org.spigotmc.event.entity.EntityDismountEvent;

public final class EntityDismount implements Listener {

    private final Roulette plugin;

    public EntityDismount(Roulette plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDismount(EntityDismountEvent event) {
        // Check if the entity who dismounted is a player.
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        // Check if the vehicle is an armor stand.
        if (!(event.getDismounted() instanceof ArmorStand)) return;

        ArmorStand armorStand = (ArmorStand) event.getDismounted();
        NamespacedKey key = new NamespacedKey(plugin, "fromRoulette");

        // Check if the vehicle has our identity key.
        if (!armorStand.getPersistentDataContainer().has(key, PersistentDataType.STRING)) return;

        // Check if the game exists.
        Game game = plugin.getGames().getGameByName(armorStand.getPersistentDataContainer().get(key, PersistentDataType.STRING));
        if (game == null) return;

        // Check a tick later to make sure the player didn't swap chair / leave the game.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // The player still in game.
            if (player.isInsideVehicle()) {
                return;
            }

            // The player leave the game (dismount / change gamemode to spectator), remove him.
            if (!game.getState().isEnding()) {
                RUtilities.handleMessage(player, plugin.getMessages().getLeavePlayer());
            }
            game.removePlayer(player, false);
        });
    }
}