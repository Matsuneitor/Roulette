package me.matsubara.roulette.listener.npc;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.file.Configuration;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.gui.GUIHolder;
import me.matsubara.roulette.gui.GUIType;
import me.matsubara.roulette.util.ItemBuilder;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class NPCRightClick implements Listener {

    private static Roulette plugin;

    public NPCRightClick(Roulette plugin) {
        NPCRightClick.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onNPCRightClick(NPCRightClickEvent event) {
        if (event.getNPC().getEntity().getType() != EntityType.PLAYER) return;

        NPC npc = event.getNPC();

        Game game = plugin.getGames().getGameByNPC(npc);
        if (game == null) return;

        Player player = event.getClicker();
        if (game.inGame(player)) return;
        if (!player.hasPermission("roulette.edit")) return;
        if (!game.getData().getCreator().equals(player.getUniqueId()) && !player.hasPermission("roulette.edit.others")) {
            return;
        }

        openMainGUI(player, game);
    }

    public static void openMainGUI(Player player, Game game) {
        String title = Configuration.Config.GAME_MENU_TITLE.asString().replace("%name%", game.getName());
        Inventory inventory = Bukkit.createInventory(new GUIHolder(null, GUIType.MAIN, game), 27, title);

        ItemStack background = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("&7").build();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, background);
        }

        ItemStack account = new ItemBuilder(Material.PLAYER_HEAD).setLore(plugin.getConfiguration().getAccountLore()).build();
        ItemStack noAccount = plugin.getConfiguration().getItem("game-menu", "no-account", null);

        inventory.setItem(10, (game.getAccount() != null) ? new ItemBuilder(account)
                .setOwningPlayer(game.getAccount())
                .setDisplayName(plugin.getConfiguration().getAccountDisplayName(game.getAccount().getName()))
                .build() : noAccount);

        int minAmount = game.getMinPlayers(), maxAmount = game.getMaxPlayers();

        ItemStack min = plugin.getConfiguration().getItem("game-menu", "min-amount", null);
        ItemStack max = plugin.getConfiguration().getItem("game-menu", "max-amount", null);

        inventory.setItem(12, new ItemBuilder(min).setAmount(minAmount).build());
        inventory.setItem(14, new ItemBuilder(max).setAmount(maxAmount).build());

        inventory.setItem(16, plugin.getConfiguration().getItem("game-menu", "close", null));

        player.openInventory(inventory);
    }
}