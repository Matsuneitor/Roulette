package me.matsubara.roulette.trait;

import me.matsubara.roulette.util.RUtils;
import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import org.bukkit.event.Listener;

@TraitName("sneaking")
public final class SneakingTrait extends Trait implements Listener {

    @Persist("sneaking")
    private boolean sneaking = false;

    public SneakingTrait() {
        super("sneaking");
    }

    @Override
    public void onSpawn() {
        if (sneaking) {
            sneak();
        }
    }

    /**
     * Makes the NPC sneak.
     */
    public void sneak() {
        RUtils.setSneaking(npc, sneaking = true);
    }

    /**
     * Makes the NPC stand.
     */
    public void stand() {
        RUtils.setSneaking(npc, sneaking = false);
    }

    /**
     * Checks if the NPC is currently sneaking.
     *
     * @return boolean.
     */
    public boolean isSneaking() {
        return sneaking;
    }
}