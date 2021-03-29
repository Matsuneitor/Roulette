package me.matsubara.roulette.gui;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.data.Chip;
import me.matsubara.roulette.file.Configuration;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.util.InventoryUpdate;
import me.matsubara.roulette.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GUI {

    private final Roulette plugin;

    private final Player player;
    private final Inventory inventory;

    private int pages, current;

    private final ItemStack background, previous, money, betAll, exit, next;

    public GUI(Roulette plugin, Player player, Game game) {
        this.plugin = plugin;
        this.player = player;

        this.inventory = plugin.getServer().createInventory(new GUIHolder(this, GUIType.CHIP, game), 36);

        this.pages = 0;
        this.current = 0;

        background = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("&7").build();
        previous = plugin.getConfiguration().getItem("shop", "previous", null);

        money = plugin.getConfiguration().getItem("shop", "money", plugin.getEconomy().format(plugin.getEconomy().getBalance(player)));
        betAll = plugin.getConfiguration().getItem("shop", "bet-all", null);

        exit = plugin.getConfiguration().getItem("shop", "exit", null);
        next = plugin.getConfiguration().getItem("shop", "next", null);

        player.openInventory(inventory);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::updateInventory);
    }

    private void updateInventory() {
        inventory.clear();

        // Slots for chips.
        List<Integer> slots = Arrays.asList(10, 11, 12, 13, 14, 15, 16);

        // Simple page formula.
        pages = (int) (Math.ceil((double) plugin.getChips().getChipsList().size() / slots.size()));

        // Background items.
        for (int i = 0; i < 35; i++) {
            if (slots.contains(i) || Arrays.asList(19, 20, 21, 22, 23, 24, 25).contains(i)) continue;
            inventory.setItem(i, background);
        }

        // Previous page button.
        if (current > 0) inventory.setItem(19, previous);

        inventory.setItem(22, money);
        inventory.setItem(23, betAll);
        inventory.setItem(35, exit);

        // Next page button.
        if (current < (pages - 1)) inventory.setItem(25, next);

        // Assigning slots.
        Map<Integer, Integer> slotIndex = new HashMap<>();
        for (int i : slots) {
            slotIndex.put(slots.indexOf(i), i);
        }

        // Where to start
        int startFrom = current * slots.size();

        // Populate inventory.
        for (int index = 0, aux = startFrom; isLast() ? index < plugin.getChips().getChipsList().size() - startFrom : index < slots.size(); index++, aux++) {
            Chip chip = plugin.getChips().getChipsList().get(aux);

            double price = chip.getPrice();

            String displayName = chip.getDisplayName() != null ? chip.getDisplayName() : plugin.getConfiguration().getChipDisplayName(price);
            List<String> lore = chip.getLore() != null ? chip.getLore() : plugin.getConfiguration().getChipLore();

            inventory.setItem(slotIndex.get(index), new ItemBuilder(chip.getUrl())
                    .setDisplayName(displayName)
                    .setLore(lore)
                    .plugin(plugin)
                    .setKey("fromRouletteMoney", price)
                    .setKey("fromRouletteChip", chip.getName())
                    .build());
        }

        // Update title.
        InventoryUpdate.updateInventory(player, Configuration.Config.SHOP_TITLE.asString()
                .replace("%page%", String.valueOf(current + 1))
                .replace("%max%", String.valueOf(pages)));
    }

    private boolean isLast() {
        return (current == pages - 1) && (pages > 1);
    }

    public void previousPage(boolean isShiftClick) {
        if (isShiftClick) {
            current = 0;
            updateInventory();
            return;
        }
        current--;
        updateInventory();
    }

    public void nextPage(boolean isShiftClick) {
        if (isShiftClick) {
            current = pages - 1;
            updateInventory();
            return;
        }
        current++;
        updateInventory();
    }

    public Inventory getInventory() {
        return inventory;
    }
}