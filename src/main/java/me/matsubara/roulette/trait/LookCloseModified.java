package me.matsubara.roulette.trait;

import me.matsubara.roulette.data.Part;
import me.matsubara.roulette.event.NPCLookCloseModifiedChangeTargetEvent;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.util.RUtils;
import net.citizensnpcs.Settings.Setting;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.command.CommandConfigurable;
import net.citizensnpcs.api.command.CommandContext;
import net.citizensnpcs.api.command.exception.CommandException;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockIterator;

import java.util.*;

@SuppressWarnings("unused")
@TraitName("lookclosemodified")
public final class LookCloseModified extends Trait implements Toggleable, CommandConfigurable {
    @Persist("disablewhilenavigating")
    private boolean disableWhileNavigating;
    @Persist("enabled")
    private boolean enabled;
    @Persist
    private boolean enableRandomLook;
    private Player lookingAt;
    @Persist
    private int randomLookDelay;
    @Persist
    private float[] randomPitchRange;
    @Persist
    private float[] randomYawRange;
    private double range;
    @Persist("realisticlooking")
    private boolean realisticLooking;
    private int t;
    private static final Location CACHE_LOCATION = new Location(null, 0.0d, 0.0d, 0.0d);
    private static final Location NPC_LOCATION = new Location(null, 0.0d, 0.0d, 0.0d);
    private static final Location PLAYER_LOCATION = new Location(null, 0.0d, 0.0d, 0.0d);
    private Game game;
    private Map<UUID, Long> viewers;

    public LookCloseModified() {
        super("lookclosemodified");
        this.disableWhileNavigating = Setting.DISABLE_LOOKCLOSE_WHILE_NAVIGATING.asBoolean();
        this.enabled = Setting.DEFAULT_LOOK_CLOSE.asBoolean();
        this.enableRandomLook = Setting.DEFAULT_RANDOM_LOOK_CLOSE.asBoolean();
        this.randomLookDelay = Setting.DEFAULT_RANDOM_LOOK_DELAY.asInt();
        this.randomPitchRange = new float[]{-10.0F, 0.0F};
        this.randomYawRange = new float[]{0.0F, 360.0F};
        this.range = Setting.DEFAULT_LOOK_CLOSE_RANGE.asDouble();
        this.realisticLooking = Setting.DEFAULT_REALISTIC_LOOKING.asBoolean();
        this.game = null;
        this.viewers = new HashMap<>();
    }

    private boolean canSee(Player player) {
        return this.realisticLooking && this.npc.getEntity() instanceof LivingEntity ? ((LivingEntity) this.npc.getEntity()).hasLineOfSight(player) : player != null && player.isValid();
    }

    public boolean canSeeTarget() {
        return this.canSee(this.lookingAt);
    }

    public void configure(CommandContext args) throws CommandException {
        try {
            this.range = args.getFlagDouble("range", args.getFlagDouble("r", this.range));
        } catch (NumberFormatException var3) {
            throw new CommandException("citizens.commands.invalid-number");
        }

        this.realisticLooking = args.hasFlag('r');
    }

    public boolean disableWhileNavigating() {
        return this.disableWhileNavigating;
    }

    public void findNewTarget() {
        double min = this.range * this.range;
        Player old = this.lookingAt;

        for (Entity entity : this.npc.getEntity().getNearbyEntities(this.range, this.range, this.range)) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                Location location = player.getLocation(CACHE_LOCATION);
                if (location.getWorld() == NPC_LOCATION.getWorld()) {
                    double dist = location.distanceSquared(NPC_LOCATION);
                    if (dist <= min && CitizensAPI.getNPCRegistry().getNPC(entity) == null && !this.isInvisible(player)) {
                        min = dist;
                        this.lookingAt = player;
                    }
                }
            }
        }

        if (old != this.lookingAt) {
            NPCLookCloseModifiedChangeTargetEvent event = new NPCLookCloseModifiedChangeTargetEvent(this.npc, old, this.lookingAt);
            Bukkit.getPluginManager().callEvent(event);
            if (this.lookingAt != event.getNewTarget() && event.getNewTarget() != null && !this.isValid(event.getNewTarget())) {
                return;
            }

            this.lookingAt = event.getNewTarget();
        }

    }

    public int getRandomLookDelay() {
        return this.randomLookDelay;
    }

    public float[] getRandomLookPitchRange() {
        return this.randomPitchRange;
    }

    public float[] getRandomLookYawRange() {
        return this.randomYawRange;
    }

    public double getRange() {
        return this.range;
    }

    public Player getTarget() {
        return this.lookingAt;
    }

    private boolean isEqual(float[] array) {
        return (double) Math.abs(array[0] - array[1]) < 0.001D;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isInvisible(Player player) {
        return player.getGameMode() == GameMode.SPECTATOR || player.hasPotionEffect(PotionEffectType.INVISIBILITY) || this.isPluginVanished(player) || !this.canSee(player);
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

    public boolean isRandomLook() {
        return this.enableRandomLook;
    }

    private boolean isValid(Player entity) {
        return entity.isOnline() && entity.isValid() && entity.getWorld() == this.npc.getEntity().getWorld() && entity.getLocation(PLAYER_LOCATION).distanceSquared(NPC_LOCATION) < this.range * this.range && !this.isInvisible(entity);
    }

    public void load(DataKey key) {
        this.range = key.getDouble("range");
    }

    public void lookClose(boolean lookClose) {
        this.enabled = lookClose;
    }

    public void onDespawn() {
        NPCLookCloseModifiedChangeTargetEvent event = new NPCLookCloseModifiedChangeTargetEvent(this.npc, this.lookingAt, null);
        Bukkit.getPluginManager().callEvent(event);
        if (event.getNewTarget() != null && this.isValid(event.getNewTarget())) {
            this.lookingAt = event.getNewTarget();
        } else {
            this.lookingAt = null;
        }

    }

    private void randomLook() {
        Random rand = new Random();
        float pitch = this.isEqual(this.randomPitchRange) ? this.randomPitchRange[0] : rand.doubles(this.randomPitchRange[0], this.randomPitchRange[1]).iterator().next().floatValue();
        float yaw = this.isEqual(this.randomYawRange) ? this.randomYawRange[0] : rand.doubles(this.randomYawRange[0], this.randomYawRange[1]).iterator().next().floatValue();
        Util.assumePose(this.npc.getEntity(), yaw, pitch);
    }

    public void run() {
        if (this.enabled && this.npc.isSpawned()) {
            if (!this.npc.getNavigator().isNavigating() || !this.disableWhileNavigating()) {
                this.npc.getEntity().getLocation(NPC_LOCATION);
                if (this.tryInvalidateTarget()) {
                    this.findNewTarget();
                }

                if (this.npc.getNavigator().isNavigating()) {
                    this.npc.getNavigator().setPaused(this.lookingAt != null);
                } else if (this.lookingAt == null && this.enableRandomLook && this.t <= 0) {
                    this.randomLook();
                    this.t = this.randomLookDelay;
                }

                --this.t;
                if (this.lookingAt != null && this.game != null && !game.getPlayers().contains(this.lookingAt.getUniqueId())) {
                    if (!inCooldown(this.lookingAt.getUniqueId()) && this.game.getPlugin().getConfiguration().npcInvite()) {
                        String npcName = npc.getName().equalsIgnoreCase("") ? null : npc.getName();
                        RUtils.handleMessage(this.lookingAt, this.game.getPlugin().getMessages().getRandomInvitation(npcName));
                        this.viewers.put(this.lookingAt.getUniqueId(), System.currentTimeMillis() + game.getPlugin().getConfiguration().getInviteInterval());
                    }

                    Util.faceEntity(this.npc.getEntity(), this.lookingAt);
                    if (this.npc.getEntity().getType().name().equals("SHULKER")) {
                        NMS.setPeekShulker(this.npc.getEntity(), 100 - (int) Math.floor(this.npc.getStoredLocation().distanceSquared(this.lookingAt.getLocation(PLAYER_LOCATION))));
                    }

                } else if (this.game != null) {
                    Util.faceEntity(this.npc.getEntity(), this.game.getParts().get(Part.NPC_TARGET));
                }
            }
        }
    }

    private boolean inCooldown(UUID uuid) {
        return viewers.containsKey(uuid) && viewers.get(uuid) > System.currentTimeMillis();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNPCLeftClick(NPCLeftClickEvent event) {
        if (!event.getNPC().equals(this.npc)) {
            return;
        }

        if (!event.getClicker().equals(this.lookingAt)) {
            return;
        }

        if (game == null || !game.getPlugin().getConfiguration().npcImitate()) {
            return;
        }

        Player target = getEntityInLineOfSight((Player) this.npc.getEntity());

        // If the NPC doesn't have a target, return.
        if (target == null) {
            return;
        }

        // If the target of the NPC is the current player, play hit animation.
        if (target.equals(event.getClicker())) {
            PlayerAnimation.ARM_SWING.play((Player) event.getNPC().getEntity());
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

    public void save(DataKey key) {
        key.setDouble("range", this.range);
    }

    public void setDisableWhileNavigating(boolean set) {
        this.disableWhileNavigating = set;
    }

    public void setRandomLook(boolean enableRandomLook) {
        this.enableRandomLook = enableRandomLook;
    }

    public void setRandomLookDelay(int delay) {
        this.randomLookDelay = delay;
    }

    public void setRandomLookPitchRange(float min, float max) {
        this.randomPitchRange = new float[]{min, max};
    }

    public void setRandomLookYawRange(float min, float max) {
        this.randomYawRange = new float[]{min, max};
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
        this.enabled = !this.enabled;
        return this.enabled;
    }

    public String toString() {
        return "LookCloseModified{" + this.enabled + "}";
    }

    private boolean tryInvalidateTarget() {
        if (this.lookingAt == null) {
            return true;
        } else {
            if (!this.isValid(this.lookingAt)) {
                NPCLookCloseModifiedChangeTargetEvent event = new NPCLookCloseModifiedChangeTargetEvent(this.npc, this.lookingAt, null);
                Bukkit.getPluginManager().callEvent(event);
                if (event.getNewTarget() != null && this.isValid(event.getNewTarget())) {
                    this.lookingAt = event.getNewTarget();
                } else {
                    this.lookingAt = null;
                }
            }

            return this.lookingAt == null;
        }
    }

    public boolean useRealisticLooking() {
        return this.realisticLooking;
    }
}