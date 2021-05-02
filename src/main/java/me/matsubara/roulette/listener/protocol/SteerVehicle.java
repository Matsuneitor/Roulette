package me.matsubara.roulette.listener.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.file.Configuration;
import me.matsubara.roulette.file.Messages;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.gui.GUIType;
import me.matsubara.roulette.listener.InventoryClick;
import me.matsubara.roulette.util.RUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
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

        ArmorStand stand = (ArmorStand) player.getVehicle();

        PersistentDataContainer container = stand.getPersistentDataContainer();

        // Check if the vehicle has our identity key.
        NamespacedKey key = new NamespacedKey(plugin, "fromRoulette");
        if (!container.has(key, PersistentDataType.STRING)) return;

        // Check if the game exists.
        Game game = plugin.getGames().getGameByName(container.get(key, PersistentDataType.STRING));
        if (game == null) return;

        // Get required values from the packet.
        StructureModifier<Float> floats = event.getPacket().getFloat();
        StructureModifier<Boolean> booleans = event.getPacket().getBooleans();

        float side = floats.read(0);
        boolean dismount = booleans.read(1);

        // Check if the player is in steer cooldown. If true, return, since the packet PacketPlayInSteerVehicle is called too quickly.
        if (isInCooldown(player.getUniqueId(), steerCooldown)) {
            if (dismount) event.setCancelled(true);
            return;
        }

        // Can be forward / backward and jump, something we don't need at the moment.
        if (!(side > 0.0f) && !(side < 0.0f) && !dismount) return;

        // Left button.
        if (side > 0.0f) {
            if (canSwapChair(player, game)) {
                execute(() -> game.previousChair(player));
            } else if (game.getState().isSelecting()) {
                execute(() -> game.previousChip(player.getUniqueId()));
            }
        }

        // Right button.
        if (side < 0.0f) {
            if (canSwapChair(player, game)) {
                execute(() -> game.nextChair(player));
            } else if (game.getState().isSelecting()) {
                execute(() -> game.nextChip(player.getUniqueId()));
            }
        }

        // Shift button.
        if (dismount) {
            // If @leave-confirm is true, open confirm GUI and return.
            if (Configuration.Config.LEAVE_CONFIRM.asBoolean()) {
                execute(() -> InventoryClick.openConfirmGUI(player, game, plugin.getConfiguration().getItem("shop", "exit", null), GUIType.CONFIRM_LEAVE));
                event.setCancelled(true);
                return;
            }

            if (isInCooldown(player.getUniqueId(), dismountCooldown)) return;

            // Send confirm message.
            if (!game.canLoseMoney()) {
                RUtils.handleMessage(player, Messages.Message.CONFIRM.asString());
            } else {
                RUtils.handleMessage(player, Messages.Message.CONFIRM_LOSE.asString());
            }

            // Add to cooldown and cancel event.
            dismountCooldown.put(player.getUniqueId(), System.currentTimeMillis() + Configuration.Config.LEAVE_CONFIRM_INTERVAL.asLong());
            event.setCancelled(true);
        }
        steerCooldown.put(player.getUniqueId(), System.currentTimeMillis() + Configuration.Config.MOVE_CHIP_INTERVAL.asLong());
    }

    private boolean canSwapChair(Player player, Game game) {
        return (game.getState().isWaiting() || game.getState().isCountdown()) && (Configuration.Config.SWAP_CHAIR.asBoolean() || player.hasPermission("roulette.swap_chair"));
    }

    private void execute(Runnable runnable) {
        plugin.getServer().getScheduler().runTask(plugin, runnable);
    }

    public boolean isInCooldown(UUID uuid, Map<UUID, Long> map) {
        return map.containsKey(uuid) && map.get(uuid) > System.currentTimeMillis();
    }
}