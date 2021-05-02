package me.matsubara.roulette.listener.npc.citizens;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.util.RUtils;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class NPCRightClick implements Listener {

    private static Roulette plugin;

    public NPCRightClick(Roulette plugin) {
        NPCRightClick.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNPCRightClick(NPCRightClickEvent event) {
        if (event.getNPC().getEntity().getType() != EntityType.PLAYER) return;

        NPC npc = event.getNPC();

        Game game = plugin.getGames().getGameByNPCUUID(npc.getUniqueId());
        if (game == null) return;

        Player player = event.getClicker();
        if (game.inGame(player)) return;
        if (!player.hasPermission("roulette.edit")) return;
        if (!game.getData().getCreator().equals(player.getUniqueId()) && !player.hasPermission("roulette.edit.others")) {
            return;
        }

        RUtils.openMainGUI(player, game);
    }
}