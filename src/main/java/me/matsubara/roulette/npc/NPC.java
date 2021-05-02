package me.matsubara.roulette.npc;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

@SuppressWarnings("unused")
public interface NPC {

    Object getNPC();

    void spawn(Location location);

    void show(Player player);

    String getName();

    String getFullName();

    UUID getUniqueId();

    void setItemInHand(ItemStack item);

    /**
     * Start look around trait, realistic only for Citizens.
     */
    void lookAround(NPC npcInterface, boolean enabled);

    void playAnimation(Animation animation);

    Player getTarget();

    void jump();

    void setPose(Pose pose);

    boolean isSneaking();

    void setRange(int range);

    void setRealisticLooking(boolean realistic);

    void hideName();

    void destroy();

    enum Animation {
        SWING,
        HURT
    }

    enum Pose {
        SNEAKING,
        STANDING
    }
}