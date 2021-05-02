package me.matsubara.roulette.npc;

import me.matsubara.roulette.data.Part;
import me.matsubara.roulette.event.NPCLibLookCloseModifiedChangeTargetEvent;
import me.matsubara.roulette.file.Configuration;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.util.RUtils;
import net.jitse.npclib.api.skin.Skin;
import net.jitse.npclib.api.state.NPCAnimation;
import net.jitse.npclib.api.state.NPCSlot;
import net.jitse.npclib.api.state.NPCState;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.util.*;

public class NPC_NPCLib implements NPC {

    private final Game game;
    private final net.jitse.npclib.api.NPC npc;
    private final Map<UUID, Long> viewers;

    private Player target;
    private Integer task;

    private static final Location CACHE_LOCATION = new Location(null, 0.0d, 0.0d, 0.0d);
    private static final Location NPC_LOCATION = new Location(null, 0.0d, 0.0d, 0.0d);
    private static final Location PLAYER_LOCATION = new Location(null, 0.0d, 0.0d, 0.0d);

    private int range;

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    public NPC_NPCLib(Game game, String name) {
        this.game = game;
        this.npc = game.getPlugin().getNPCLibrary().createNPC(Arrays.asList(RUtils.translate(name)));

        Skin skin = new Skin(Configuration.Config.SKIN_TEXTURE.asString(), Configuration.Config.SKIN_SIGNATURE.asString());

        this.npc.setSkin(skin);

        viewers = new HashMap<>();

        range = Configuration.Config.LOOK_DISTANCE.asInt();
    }

    @Override
    public Object getNPC() {
        return npc;
    }

    @Override
    public void spawn(Location location) {
        npc.setLocation(location);
        npc.create();
        for (Player player : Bukkit.getOnlinePlayers()) npc.show(player);
    }

    @Override
    public void show(Player player) {
        if (!npc.isShown(player)) npc.show(player);
    }

    @Nullable
    @Override
    public String getName() {
        return getFullName();
    }

    @Nullable
    @Override
    public String getFullName() {
        return npc.getText().isEmpty() ? "" : npc.getText().get(0);
    }

    @Override
    public UUID getUniqueId() {
        return npc.getUniqueId();
    }

    @Override
    public void setItemInHand(ItemStack item) {
        npc.setItem(NPCSlot.MAINHAND, item);
    }

    @Override
    public void lookAround(NPC npcInterface, boolean lookClose) {
        if (!lookClose) {
            if (task != null) Bukkit.getScheduler().cancelTask(task);
            if (npc.getState(NPCState.CROUCHED)) setPose(Pose.STANDING);
            // Reset look.
            if (game.getParts().get(Part.NPC_TARGET) != null) {
                npc.lookAt(game.getParts().get(Part.NPC_TARGET).getLocation());
            }
            this.task = null;
            return;
        }

        if (task != null) return;

        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                NPC_LOCATION.setWorld(npc.getWorld());
                NPC_LOCATION.setX(npc.getLocation().getX());
                NPC_LOCATION.setY(npc.getLocation().getY());
                NPC_LOCATION.setZ(npc.getLocation().getZ());
                NPC_LOCATION.setYaw(npc.getLocation().getYaw());
                NPC_LOCATION.setPitch(npc.getLocation().getPitch());

                if (tryInvalidateTarget()) findNewTarget();

                if (target != null && game != null && !game.getPlayers().contains(target.getUniqueId())) {
                    if (!inCooldown(target.getUniqueId()) && Configuration.Config.NPC_INVITE.asBoolean()) {
                        RUtils.handleMessage(target, game.getPlugin().getMessages().getRandomNPCMessage(npcInterface, "invitations"));
                        viewers.put(target.getUniqueId(), System.currentTimeMillis() + Configuration.Config.INVITE_INTERVAL.asLong());
                    }

                    npc.lookAt(target.getLocation());
                    if (target.isSneaking() && !target.isFlying()) setPose(Pose.SNEAKING);
                    else if (isSneaking()) setPose(Pose.STANDING);
                } else if (game != null) {
                    npc.lookAt(game.getParts().get(Part.NPC_TARGET).getLocation());
                }
            }
        }.runTaskTimer(game.getPlugin(), 0L, 1L).getTaskId();
    }

    @Override
    public void playAnimation(Animation animation) {
        switch (animation) {
            case SWING:
                npc.playAnimation(NPCAnimation.SWING_MAINHAND);
                break;
            case HURT:
                npc.playAnimation(NPCAnimation.TAKE_DAMAGE);
                break;
        }
    }

    @Override
    public void jump() {
        // Not possible with the current API.
        assert true;
    }

    @Override
    public void setPose(Pose pose) {
        switch (pose) {
            case SNEAKING:
                if (!npc.getState(NPCState.CROUCHED)) npc.toggleState(NPCState.CROUCHED);
                break;
            case STANDING:
                if (npc.getState(NPCState.CROUCHED)) npc.toggleState(NPCState.CROUCHED);
                break;
        }
    }

    @Override
    public boolean isSneaking() {
        return npc.getState(NPCState.CROUCHED);
    }

    @Nullable
    @Override
    public Player getTarget() {
        return target;
    }

    @Override
    public void setRange(int range) {
        this.range = range;
        assert true;
    }

    @Override
    public void setRealisticLooking(boolean realisticLooking) {
        // Can't do that.
        assert true;
    }

    @Override
    public void hideName() {
        npc.setText(Collections.emptyList());
    }

    @Override
    public void destroy() {
        lookAround(null, false);
        npc.destroy();
    }

    public void findNewTarget() {
        Validate.notNull(npc.getLocation().getWorld(), "World can't be null.");
        double min = range * range;
        Player old = target;

        for (Entity entity : npc.getLocation().getWorld().getNearbyEntities(npc.getLocation(), range, range, range)) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                Location location = player.getLocation(CACHE_LOCATION);
                if (location.getWorld() == NPC_LOCATION.getWorld()) {
                    double dist = location.distanceSquared(NPC_LOCATION);
                    if (dist <= min && !isInvisible(player)) {
                        min = dist;
                        target = player;
                    }
                }
            }
        }

        if (old != target) {
            NPCLibLookCloseModifiedChangeTargetEvent event = new NPCLibLookCloseModifiedChangeTargetEvent(npc, old, target);
            Bukkit.getPluginManager().callEvent(event);
            if (target != event.getNewTarget() && event.getNewTarget() != null && !isValid(event.getNewTarget())) {
                return;
            }

            target = event.getNewTarget();
        }
    }

    private boolean tryInvalidateTarget() {
        if (target == null) {
            return true;
        } else {
            if (!isValid(target)) {
                NPCLibLookCloseModifiedChangeTargetEvent event = new NPCLibLookCloseModifiedChangeTargetEvent(npc, target, null);
                Bukkit.getPluginManager().callEvent(event);
                if (event.getNewTarget() != null && isValid(event.getNewTarget())) {
                    target = event.getNewTarget();
                } else {
                    target = null;
                }
            }

            return target == null;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isInvisible(Player player) {
        return player.getGameMode() == GameMode.SPECTATOR || player.hasPotionEffect(PotionEffectType.INVISIBILITY) || isPluginVanished(player) || !canSee(player);
    }

    private boolean isPluginVanished(Player player) {
        Iterator<MetadataValue> iterator = player.getMetadata("vanished").iterator();

        MetadataValue meta;
        do {
            if (!iterator.hasNext()) {
                return false;
            }

            meta = iterator.next();
        } while (!meta.asBoolean());

        return true;
    }

    private boolean isValid(Player entity) {
        return entity.isOnline() && entity.isValid() && entity.getWorld() == npc.getWorld() && entity.getLocation(PLAYER_LOCATION).distanceSquared(NPC_LOCATION) < range * range && !isInvisible(entity);
    }

    private boolean canSee(Player player) {
        return player != null && player.isValid();
    }

    private boolean inCooldown(UUID uuid) {
        return viewers.containsKey(uuid) && viewers.get(uuid) > System.currentTimeMillis();
    }
}