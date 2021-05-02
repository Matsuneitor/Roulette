package me.matsubara.roulette.trait;

import com.cryptomorin.xseries.XMaterial;
import me.matsubara.roulette.data.Part;
import me.matsubara.roulette.event.NPCLookCloseModifiedChangeTargetEvent;
import me.matsubara.roulette.file.Configuration;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.npc.NPC;
import me.matsubara.roulette.util.RUtils;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.trait.Toggleable;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.PlayerAnimation;
import net.citizensnpcs.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.*;

@TraitName("lookclosemodified")
public final class LookCloseModified extends Trait implements Toggleable {

    @Persist("disablewhilenavigating")
    private final boolean disableWhileNavigating;
    @Persist("enabled")
    private boolean enabled;
    @Persist
    private final boolean enableRandomLook;
    private Player lookingAt;
    @Persist
    private final int randomLookDelay;
    @Persist
    private final float[] randomPitchRange;
    @Persist
    private final float[] randomYawRange;
    private double range;
    @Persist("realisticlooking")
    private boolean realisticLooking;
    private int t;
    private static final Location CACHE_LOCATION = new Location(null, 0.0d, 0.0d, 0.0d);
    private static final Location NPC_LOCATION = new Location(null, 0.0d, 0.0d, 0.0d);
    private static final Location PLAYER_LOCATION = new Location(null, 0.0d, 0.0d, 0.0d);
    private Game game;
    private final Map<UUID, Long> viewers;

    public LookCloseModified() {
        super("lookclosemodified");
        this.disableWhileNavigating = true;
        this.enabled = false;
        this.enableRandomLook = false;
        this.randomLookDelay = 60;
        this.randomPitchRange = new float[]{-10.0F, 0.0F};
        this.randomYawRange = new float[]{0.0F, 360.0F};
        this.game = null;
        this.viewers = new HashMap<>();
    }

    private boolean canSee(Player player) {
        return realisticLooking && npc.getEntity() instanceof LivingEntity ? ((LivingEntity) npc.getEntity()).hasLineOfSight(player) : player != null && player.isValid();
    }

    public boolean disableWhileNavigating() {
        return disableWhileNavigating;
    }

    public void findNewTarget() {
        double min = range * range;
        Player old = lookingAt;

        for (Entity entity : npc.getEntity().getNearbyEntities(range, range, range)) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                Location location = player.getLocation(CACHE_LOCATION);
                if (location.getWorld() == NPC_LOCATION.getWorld()) {
                    double dist = location.distanceSquared(NPC_LOCATION);
                    if (dist <= min && CitizensAPI.getNPCRegistry().getNPC(entity) == null && !isInvisible(player)) {
                        min = dist;
                        lookingAt = player;
                    }
                }
            }
        }

        if (old != lookingAt) {
            NPCLookCloseModifiedChangeTargetEvent event = new NPCLookCloseModifiedChangeTargetEvent(npc, old, lookingAt);
            Bukkit.getPluginManager().callEvent(event);
            if (lookingAt != event.getNewTarget() && event.getNewTarget() != null && !isValid(event.getNewTarget())) {
                return;
            }

            lookingAt = event.getNewTarget();
        }

    }

    private boolean isEqual(float[] array) {
        return (double) Math.abs(array[0] - array[1]) < 0.001d;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isInvisible(Player player) {
        return player.getGameMode() == GameMode.SPECTATOR || player.hasPotionEffect(PotionEffectType.INVISIBILITY) || RUtils.isPluginVanished(player) || !canSee(player);
    }

    private boolean isValid(Player entity) {
        return entity.isOnline() && entity.isValid() && entity.getWorld() == npc.getEntity().getWorld() && entity.getLocation(PLAYER_LOCATION).distanceSquared(NPC_LOCATION) < range * range && !isInvisible(entity);
    }

    public void load(DataKey key) {
        range = key.getDouble("range");
    }

    public void lookClose(boolean lookClose) {
        Util.faceEntity(npc.getEntity(), game.getParts().get(Part.NPC_TARGET));
        enabled = lookClose;
    }

    public void onDespawn() {
        NPCLookCloseModifiedChangeTargetEvent event = new NPCLookCloseModifiedChangeTargetEvent(npc, lookingAt, null);
        Bukkit.getPluginManager().callEvent(event);
        if (event.getNewTarget() != null && isValid(event.getNewTarget())) {
            lookingAt = event.getNewTarget();
        } else {
            lookingAt = null;
        }

    }

    private void randomLook() {
        Random rand = new Random();
        float pitch = isEqual(randomPitchRange) ? randomPitchRange[0] : rand.doubles(randomPitchRange[0], randomPitchRange[1]).iterator().next().floatValue();
        float yaw = isEqual(randomYawRange) ? randomYawRange[0] : rand.doubles(randomYawRange[0], randomYawRange[1]).iterator().next().floatValue();
        Util.assumePose(npc.getEntity(), yaw, pitch);
    }

    public void run() {
        if (enabled && npc.isSpawned()) {
            if (!npc.getNavigator().isNavigating() || !disableWhileNavigating()) {
                npc.getEntity().getLocation(NPC_LOCATION);
                if (tryInvalidateTarget()) {
                    findNewTarget();
                }

                if (npc.getNavigator().isNavigating()) {
                    npc.getNavigator().setPaused(lookingAt != null);
                } else if (lookingAt == null && enableRandomLook && t <= 0) {
                    randomLook();
                    t = randomLookDelay;
                }

                --t;
                if (lookingAt != null && game != null && !game.getPlayers().contains(lookingAt.getUniqueId())) {
                    if (!inCooldown(lookingAt.getUniqueId()) && Configuration.Config.NPC_INVITE.asBoolean()) {
                        RUtils.handleMessage(lookingAt, game.getPlugin().getMessages().getRandomNPCMessage(game.getNPC(), "invitations"));
                        viewers.put(lookingAt.getUniqueId(), System.currentTimeMillis() + Configuration.Config.INVITE_INTERVAL.asLong());
                    }

                    Util.faceEntity(npc.getEntity(), lookingAt);
                    if (lookingAt.isSneaking() && !lookingAt.isFlying()) game.getNPC().setPose(NPC.Pose.SNEAKING);
                    else if (game.getNPC().isSneaking()) game.getNPC().setPose(NPC.Pose.STANDING);
                    if (npc.getEntity().getType().name().equals("SHULKER")) {
                        NMS.setPeekShulker(npc.getEntity(), 100 - (int) Math.floor(npc.getStoredLocation().distanceSquared(lookingAt.getLocation(PLAYER_LOCATION))));
                    }
                } else if (game != null) {
                    Util.faceEntity(npc.getEntity(), game.getParts().get(Part.NPC_TARGET));
                }
            }
        }
    }

    private boolean inCooldown(UUID uuid) {
        return viewers.containsKey(uuid) && viewers.get(uuid) > System.currentTimeMillis();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNPCLeftClick(NPCLeftClickEvent event) {
        if (!event.getNPC().equals(npc)) {
            return;
        }

        if (!event.getClicker().equals(lookingAt)) {
            return;
        }

        if (game == null || !Configuration.Config.NPC_IMITATE.asBoolean()) {
            return;
        }

        Player target = getEntityInLineOfSight((Player) npc.getEntity());

        // If the NPC doesn't have a target, return.
        if (target == null) {
            return;
        }

        // If the target of the NPC is the current player, play hit animation.
        if (target.equals(event.getClicker())) {
            PlayerAnimation.ARM_SWING.play((Player) event.getNPC().getEntity());

            // Ball can be null.
            if (game.getPlugin().getConfiguration().getBall() == null) return;

            // Both options must be true.
            if (!Configuration.Config.NPC_IMITATE.asBoolean() || !Configuration.Config.NPC_PROJECTILE.asBoolean()) {
                return;
            }

            Vector direction = ((Player) npc.getEntity()).getEyeLocation().getDirection().multiply(2.0d);
            Location location = ((Player) npc.getEntity()).getEyeLocation().add(direction.getX(), direction.getY(), direction.getZ());

            Projectile projectile = null;
            EnderSignal signal = null;

            switch (XMaterial.matchXMaterial(game.getPlugin().getConfiguration().getBall().getType())) {
                case SNOWBALL:
                    projectile = target.getWorld().spawn(location, Snowball.class);
                    break;
                case ENDER_PEARL:
                    projectile = target.getWorld().spawn(location, EnderPearl.class);
                    break;
                case EGG:
                    projectile = target.getWorld().spawn(location, Egg.class);
                    break;
                case FIRE_CHARGE:
                    projectile = target.getWorld().spawn(location, Fireball.class);
                    ((Fireball) projectile).setIsIncendiary(false);
                    break;
                case ENDER_EYE:
                    signal = target.getWorld().spawn(location, EnderSignal.class);
                    break;
            }

            if (projectile == null && signal == null) return;

            if (projectile != null) {
                projectile.setMetadata("isRoulette", new FixedMetadataValue(game.getPlugin(), true));
                projectile.setShooter((ProjectileSource) npc.getEntity());
                projectile.setVelocity(direction);
            } else {
                signal.setMetadata("isRoulette", new FixedMetadataValue(game.getPlugin(), true));
                signal.setTargetLocation(target.getLocation());
                signal.setDropItem(false);
            }
        }
    }

    private Player getEntityInLineOfSight(Player player) {
        List<Entity> entities = player.getNearbyEntities(3, 3, 3);
        BlockIterator iterator = new BlockIterator(player, 3);

        while (iterator.hasNext()) {
            Block block = iterator.next();
            int bx = block.getX();
            int by = block.getY();
            int bz = block.getZ();

            if (block.getType().isSolid()) {
                break;
            }

            for (Entity entity : entities) {
                if (!(entity instanceof Player)) {
                    continue;
                }

                Location location = entity.getLocation();
                double ex = location.getX();
                double ey = location.getY();
                double ez = location.getZ();

                if ((bx - 0.75d <= ex && ex <= bx + 1.75d) && (bz - 0.75d <= ez && ez <= bz + 1.75d) && (by - 1.0d <= ey && ey <= by + 2.5d)) {
                    return (Player) entity;
                }
            }
        }
        return null;
    }

    public void setRange(int range) {
        this.range = range;
    }

    public void setRealisticLooking(boolean realistic) {
        this.realisticLooking = realistic;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public boolean toggle() {
        enabled = !enabled;
        return enabled;
    }

    public String toString() {
        return "LookCloseModified{" + enabled + "}";
    }

    private boolean tryInvalidateTarget() {
        if (lookingAt == null) {
            return true;
        } else {
            if (!isValid(lookingAt)) {
                NPCLookCloseModifiedChangeTargetEvent event = new NPCLookCloseModifiedChangeTargetEvent(npc, lookingAt, null);
                Bukkit.getPluginManager().callEvent(event);
                if (event.getNewTarget() != null && isValid(event.getNewTarget())) {
                    lookingAt = event.getNewTarget();
                } else {
                    lookingAt = null;
                }
            }

            return lookingAt == null;
        }
    }

    public Player getTarget() {
        return this.lookingAt;
    }
}