package me.matsubara.roulette.gui;

import com.cryptomorin.xseries.XMaterial;
import me.matsubara.roulette.data.Chip;
import me.matsubara.roulette.util.InventoryUpdate;
import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.util.RUtilities;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public final class GUI {

    private final Roulette plugin;

    private final Player player;
    private final Inventory inventory;

    private int pages, current;

    // Default items.
    private final ItemStack background, previous, money, exit, next;

    public GUI(Roulette plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        // Create inventory.
        this.inventory = plugin.getServer().createInventory(new GUIHolder(this), 36);

        // Initialize integers.
        this.pages = 0;
        this.current = 0;

        // Background items.
        background = XMaterial.GRAY_STAINED_GLASS_PANE.parseItem();
        Objects.requireNonNull(background, "ItemStack can't be null.");
        ItemMeta backgroundMeta = background.getItemMeta();
        if (backgroundMeta != null) {
            backgroundMeta.setDisplayName(ChatColor.GRAY + "");
            background.setItemMeta(backgroundMeta);
        }
        // Previous page button.
        previous = plugin.getConfiguration().getItem("previous", null);

        // Money display.
        double balance = plugin.getEconomy().getBalance(player);
        money = plugin.getConfiguration().getItem("money", plugin.getEconomy().format(balance));

        // Exit button.
        exit = plugin.getConfiguration().getItem("exit", null);

        // Next page button.
        next = plugin.getConfiguration().getItem("next", null);

        player.openInventory(inventory);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::updateInventory);
    }

    private void updateInventory() {
        inventory.clear();

        // Slots for chips.
        List<Integer> slots = Arrays.asList(10, 11, 12, 13, 14, 15, 16);

        // Simple page formula.
        pages = (int) (Math.ceil((double) plugin.getChips().getList().size() / slots.size()));

        // Background items.
        for (int i = 0; i < 35; i++) {
            if (slots.contains(i) || Arrays.asList(19, 20, 21, 22, 23, 24, 25).contains(i)) {
                continue;
            }
            inventory.setItem(i, background);
        }

        // Previous page button.
        if (current > 0) {
            inventory.setItem(19, previous);
        }

        // Money display.
        inventory.setItem(22, money);

        // Exit button.
        inventory.setItem(35, exit);

        // Next page button.
        if (current < (pages - 1)) {
            inventory.setItem(25, next);
        }

        // Assigning slots.
        Map<Integer, Integer> slotIndex = new HashMap<>();
        for (int i : slots) {
            slotIndex.put(slots.indexOf(i), i);
        }

        // Where to start
        int startFrom = current * slots.size();

        // Populate inventory.
        NamespacedKey keyMoney = new NamespacedKey(plugin, "fromRouletteMoney"), keyChip = new NamespacedKey(plugin, "fromRouletteChip");
        for (int index = 0, aux = startFrom; isLast() ? index < plugin.getChips().getList().size() - startFrom : index < slots.size(); index++, aux++) {
            Chip chip = plugin.getChips().getList().get(aux);

            ItemStack item = RUtilities.createHead(chip.getUrl());
            if (item == null) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            double price = chip.getPrice();

            String displayName = chip.getDisplayName() != null ? chip.getDisplayName() : plugin.getConfiguration().getChipDisplayName(price);
            List<String> lore = chip.getLore() != null ? chip.getLore() : plugin.getConfiguration().getChipLore();

            meta.setDisplayName(displayName);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(keyMoney, PersistentDataType.DOUBLE, price);
            meta.getPersistentDataContainer().set(keyChip, PersistentDataType.STRING, chip.getName());
            item.setItemMeta(meta);

            inventory.setItem(slotIndex.get(index), item);
        }

        // Update title.
        InventoryUpdate.updateInventory(player, plugin.getConfiguration().getShopTitle(current + 1, pages));
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