package me.matsubara.roulette.listener;

import com.cryptomorin.xseries.XMaterial;
import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.data.Part;
import me.matsubara.roulette.util.RUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public final class PlayerItemHeld implements Listener {

    private final Roulette plugin;

    private final static Map<UUID, Integer> tasks = new HashMap<>();

    public PlayerItemHeld(Roulette plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        ItemStack current = player.getInventory().getItem(event.getNewSlot());

        // If the item in the current slot is null or isn't a wooden shovel, return.
        if (current == null || current.getType() != XMaterial.WOODEN_SHOVEL.parseMaterial()) {
            if (!tasks.containsKey(player.getUniqueId())) return;
            if (tasks.get(player.getUniqueId()) == -1) return;
            // Cancel task.
            plugin.getServer().getScheduler().cancelTask(tasks.get(player.getUniqueId()));
            // Change task id to -1.
            tasks.put(player.getUniqueId(), -1);
            return;
        }

        Location center = getCorrectLocation(player);

        // Display particles.
        tasks.put(player.getUniqueId(), new BukkitRunnable() {
            Location old = null;

            @Override
            public void run() {
                for (Part part : Part.getValues(true)) {
                    if (!part.isBorder()) continue;

                    Vector offset = new Vector(part.getOffsetX(), part.getOffsetY(), part.getOffsetZ());

                    Location newLoc = center.clone().add(RUtils.offsetVector(offset, center.getYaw(), center.getPitch()));

                    // First iteration.
                    if (old == null) {
                        old = newLoc.clone();
                        continue;
                    }

                    spawnParticleAlongLine(old, newLoc, Particle.VILLAGER_HAPPY, 20, 1, 0.1d, 0.1d, 0.1d, 0d, null, false, loc -> loc.getBlock().isPassable());
                }
            }
        }.runTaskTimer(plugin, 0L, 30L).getTaskId());
    }

    public void spawnParticleAlongLine(Location start, Location end, Particle particle, int pointsPerLine, int particleCount, double offsetX, double offsetY, double offsetZ, double extra, @Nullable Double data, boolean forceDisplay, @Nullable Predicate<Location> operationPerPoint) {
        Validate.notNull(start.getWorld(), "World can't be null.");
        double d = start.distance(end) / pointsPerLine;
        for (int i = 0; i < pointsPerLine; i++) {
            Location l = start.clone();
            Vector direction = end.toVector().subtract(start.toVector()).normalize();
            Vector v = direction.multiply(i * d);
            l.add(v.getX(), v.getY(), v.getZ());
            if (operationPerPoint == null) {
                start.getWorld().spawnParticle(particle, l, particleCount, offsetX, offsetY, offsetZ, extra, data, forceDisplay);
                continue;
            }
            if (operationPerPoint.test(l)) {
                start.getWorld().spawnParticle(particle, l, particleCount, offsetX, offsetY, offsetZ, extra, data, forceDisplay);
            }
        }
    }

    private Location getCorrectLocation(Player player) {
        BlockFace fromYaw = RUtils.faceFromYaw(player.getLocation().getYaw(), false);

        Location location = player.getTargetBlock(null, 5).getLocation();
        location.setDirection(RUtils.getDirection(RUtils.getNextFace(fromYaw).getOppositeFace()));
        return location;
    }
}