package me.matsubara.roulette.event;

import net.citizensnpcs.api.event.NPCEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

@SuppressWarnings("unused")
public final class NPCLookCloseModifiedChangeTargetEvent extends NPCEvent {

    private Player next;
    private final Player old;
    private static final HandlerList handlers = new HandlerList();

    public NPCLookCloseModifiedChangeTargetEvent(NPC npc, Player old, Player next) {
        super(npc);
        this.old = old;
        this.next = next;
    }

    @SuppressWarnings("NullableProblems")
    public HandlerList getHandlers() {
        return handlers;
    }

    public Player getNewTarget() {
        return this.next;
    }

    public Player getPreviousTarget() {
        return this.old;
    }

    public void setNewTarget(Player target) {
        this.next = target;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}