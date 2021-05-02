package me.matsubara.roulette.listener.npc.npclib;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.util.RUtils;
import net.jitse.npclib.api.NPC;
import net.jitse.npclib.api.events.NPCInteractEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class NPCInteract implements Listener {

    private final Roulette plugin;

    public NPCInteract(Roulette plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNPCInteract(NPCInteractEvent event) {
        if (event.getClickType() != NPCInteractEvent.ClickType.RIGHT_CLICK) return;

        NPC npc = event.getNPC();

        Game game = plugin.getGames().getGameByNPCUUID(npc.getUniqueId());
        if (game == null) return;

        Player player = event.getWhoClicked();
        if (game.inGame(player)) return;
        if (!player.hasPermission("roulette.edit")) return;
        if (!game.getData().getCreator().equals(player.getUniqueId()) && !player.hasPermission("roulette.edit.others")) {
            return;
        }

        RUtils.openMainGUI(player, game);
    }
}