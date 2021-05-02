package me.matsubara.roulette.npc;

import me.matsubara.roulette.file.Configuration;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.trait.LookCloseModified;
import me.matsubara.roulette.trait.SneakingTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.util.PlayerAnimation;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.UUID;

public final class NPC_Citizens implements NPC {

    private final Game game;
    private final net.citizensnpcs.api.npc.NPC npc;

    public NPC_Citizens(@Nullable UUID npcUUID, Game game, @Nullable String name) {
        this.game = game;
        if (npcUUID != null) {
            this.npc = CitizensAPI.getNPCRegistry().getByUniqueId(npcUUID);
        } else {
            this.npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, name);
        }
    }

    @Override
    public Object getNPC() {
        return npc;
    }

    @Override
    public void spawn(Location location) {
        // Spawn the NPC if isn't spawned.
        if (!npc.isSpawned()) npc.spawn(location);
    }

    @Override
    public void show(Player player) {
        // Not needed for Citizens.
        assert true;
    }

    @Override
    public String getName() {
        return npc.getName();
    }

    @Override
    public String getFullName() {
        return npc.getFullName();
    }

    @Override
    public UUID getUniqueId() {
        return npc.getUniqueId();
    }

    @Override
    public void setItemInHand(ItemStack item) {
        npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.HAND, item);
    }

    @Override
    public void lookAround(NPC npcInterface, boolean lookClose) {
        if (!lookClose) {
            npc.getOrAddTrait(LookCloseModified.class).lookClose(false);
            return;
        }
        npc.getOrAddTrait(LookCloseModified.class).setGame(game);
        npc.getOrAddTrait(LookCloseModified.class).setRealisticLooking(Configuration.Config.REALISTIC_LOOKING.asBoolean());
        npc.getOrAddTrait(LookCloseModified.class).setRange(Configuration.Config.LOOK_DISTANCE.asInt());
        npc.getOrAddTrait(LookCloseModified.class).lookClose(true);
    }

    @Override
    public void playAnimation(Animation animation) {
        switch (animation) {
            case SWING:
                PlayerAnimation.ARM_SWING.play((Player) npc.getEntity());
                break;
            case HURT:
                PlayerAnimation.HURT.play((Player) npc.getEntity());
                break;
        }
    }

    @Override
    public void jump() {
        npc.getEntity().setVelocity(npc.getEntity().getVelocity().add(new Vector(0.0f, 0.42f, 0.0f)));
    }

    @Override
    public void setPose(Pose pose) {
        switch (pose) {
            case SNEAKING:
                npc.getOrAddTrait(SneakingTrait.class).sneak();
                break;
            case STANDING:
                npc.getOrAddTrait(SneakingTrait.class).stand();
                break;
        }
    }

    @Override
    public boolean isSneaking() {
        return npc.getOrAddTrait(SneakingTrait.class).isSneaking();
    }

    @Override
    public Player getTarget() {
        return npc.getOrAddTrait(LookCloseModified.class).getTarget();
    }

    @Override
    public void setRange(int range) {
        npc.getOrAddTrait(LookCloseModified.class).setRange(range);
    }

    @Override
    public void setRealisticLooking(boolean realistic) {
        npc.getOrAddTrait(LookCloseModified.class).setRealisticLooking(realistic);
    }

    @Override
    public void hideName() {
        npc.data().setPersistent(net.citizensnpcs.api.npc.NPC.NAMEPLATE_VISIBLE_METADATA, false);
    }

    @Override
    public void destroy() {
        npc.destroy();
    }
}