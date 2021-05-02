package me.matsubara.roulette.event;

import net.jitse.npclib.api.NPC;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@SuppressWarnings("unused")
public final class NPCLibLookCloseModifiedChangeTargetEvent extends Event {

    private Player next;
    private final Player old;
    private static final HandlerList handlers = new HandlerList();

    public NPCLibLookCloseModifiedChangeTargetEvent(NPC npc, Player old, Player next) {
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