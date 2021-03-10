package me.matsubara.roulette.listener.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.util.RUtilities;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SteerVehicle extends PacketAdapter {

    private final Roulette plugin;
    private final Map<UUID, Long> steerCooldown;
    private final Map<UUID, Long> dismountCooldown;

    public SteerVehicle(Plugin plugin, ListenerPriority priority, PacketType... types) {
        super(plugin, priority, types);
        this.plugin = (Roulette) plugin;
        this.steerCooldown = new HashMap<>();
        this.dismountCooldown = new HashMap<>();
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();

        // Check if current vehicle is an armor stand.
        if (!(player.getVehicle() instanceof ArmorStand)) return;

        ArmorStand armorStand = (ArmorStand) player.getVehicle();
        NamespacedKey key = new NamespacedKey(plugin, "fromRoulette");

        // Check if the vehicle has our identity key.
        if (!armorStand.getPersistentDataContainer().has(key, PersistentDataType.STRING)) return;

        // Check if the game exists.
        Game game = plugin.getGames().getGameByName(armorStand.getPersistentDataContainer().get(key, PersistentDataType.STRING));
        if (game == null) return;

        // Get required values from the packet.
        StructureModifier<Float> floats = event.getPacket().getFloat();
        StructureModifier<Boolean> booleans = event.getPacket().getBooleans();

        float side = floats.read(0);
        boolean dismount = booleans.read(1);

        // Check if the player is in steer cooldown. If true, return, since the packet PacketPlayInSteerVehicle is called too quickly.
        if (inCooldown(player.getUniqueId(), steerCooldown)) {
            if (dismount) event.setCancelled(true);
            return;
        }

        // Can be forward / backward and jump, something we don't need at the moment.
        if (!(side > 0.0f) && !(side < 0.0f) && !dismount) return;

        // Left button.
        if (side > 0.0f) {
            if (swapChair(player, game)) {
                execute(() -> game.previousChair(player));
            } else if (game.getState().isSelecting()) {
                execute(() -> game.previousChip(player.getUniqueId()));
            }
        }

        // Right button.
        if (side < 0.0f) {
            if (swapChair(player, game)) {
                execute(() -> game.nextChair(player));
            } else if (game.getState().isSelecting()) {
                execute(() -> game.nextChip(player.getUniqueId()));
            }
        }

        // Shift button.
        if (dismount) {
            if (inCooldown(player.getUniqueId(), dismountCooldown)) return;

            // Send confirm message.
            if (!game.canLoseMoney()) {
                RUtilities.handleMessage(player, plugin.getMessages().getConfirm());
            } else {
                RUtilities.handleMessage(player, plugin.getMessages().getConfirmLose());
            }

            // Add to cooldown and cancel event.
            dismountCooldown.put(player.getUniqueId(), System.currentTimeMillis() + plugin.getConfiguration().getLeaveConfirmInterval());
            event.setCancelled(true);
        }
        steerCooldown.put(player.getUniqueId(), System.currentTimeMillis() + plugin.getConfiguration().getMoveChipInterval());
    }

    private boolean swapChair(Player player, Game game) {
        return (game.getState().isWaiting() || game.getState().isCountdown()) && (plugin.getConfiguration().swapChair() || player.hasPermission("roulette.swap_chair"));
    }

    private void execute(Runnable runnable) {
        plugin.getServer().getScheduler().runTask(plugin, runnable);
    }

    public boolean inCooldown(UUID uuid, Map<UUID, Long> map) {
        return map.containsKey(uuid) && map.get(uuid) > System.currentTimeMillis();
    }
}