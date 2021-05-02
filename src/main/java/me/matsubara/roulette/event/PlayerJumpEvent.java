package me.matsubara.roulette.event;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
public class PlayerJumpEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private static final PlayerJumpEventListener listener = new PlayerJumpEventListener();

    private final Player player;

    public PlayerJumpEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    private static class PlayerJumpEventListener implements Listener {

        private final Map<UUID, Integer> jumps = new HashMap<>();

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerJoin(PlayerJoinEvent event) {
            jumps.put(event.getPlayer().getUniqueId(), event.getPlayer().getStatistic(Statistic.JUMP));
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerQuit(PlayerQuitEvent event) {
            jumps.remove(event.getPlayer().getUniqueId());
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPlayerMove(PlayerMoveEvent event) {
            Player player = event.getPlayer();

            if (event.getTo() == null) return;
            if (event.getFrom().getY() > event.getTo().getY()) return;

            int current = player.getStatistic(Statistic.JUMP);
            int last = jumps.getOrDefault(player.getUniqueId(), -1);

            if (last == current) return;

            jumps.put(player.getUniqueId(), current);

            double yDiff = (long) ((event.getTo().getY() - event.getFrom().getY()) * 1000d) / 1000d;

            if ((yDiff < 0.035d || yDiff > 0.037d) && (yDiff < 0.116d || yDiff > 0.118d)) {
                Bukkit.getPluginManager().callEvent(new PlayerJumpEvent(player));
            }
        }
    }

    public static void register(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }
}