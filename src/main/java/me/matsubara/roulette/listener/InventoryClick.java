package me.matsubara.roulette.listener;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.gui.GUIHolder;
import me.matsubara.roulette.util.RUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public final class InventoryClick implements Listener {

    private final Roulette plugin;

    public InventoryClick(Roulette plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        if (event.getClickedInventory() == null) return;
        if (event.getCurrentItem() == null) return;
        if (event.getCurrentItem().getItemMeta() == null) return;

        ItemStack current = event.getCurrentItem();
        Inventory inventory = event.getClickedInventory();

        if (!(inventory.getHolder() instanceof GUIHolder)) return;

        GUIHolder holder = (GUIHolder) inventory.getHolder();

        // Check if the playe belongs to a game, else return.
        Game game = plugin.getGames().getGameByPlayer(player);
        if (game == null) return;

        // Play click sound.
        XSound.play(game.getSpinHologram().getLocation(), plugin.getConfiguration().getClickSound());

        boolean hasDisplayName = current.getItemMeta().hasDisplayName();
        String displayName = current.getItemMeta().getDisplayName();

        PersistentDataContainer container = current.getItemMeta().getPersistentDataContainer();

        NamespacedKey key = new NamespacedKey(plugin, "fromRouletteMoney");
        if (container.has(key, PersistentDataType.DOUBLE)) {
            Double money = container.get(key, PersistentDataType.DOUBLE);
            if (money == null) return;

            // Check if the player has the required money for this chip.
            if (!plugin.getEconomy().has(player, money)) {
                Optional<XMaterial> material = XMaterial.matchXMaterial(plugin.getConfiguration().getNotEnoughMaterial());
                if (!material.isPresent()) return;

                if (material.get().parseMaterial() == null) return;
                current.setType(material.get().parseMaterial());

                ItemMeta meta = current.getItemMeta();
                if (meta == null) return;

                meta.setDisplayName(plugin.getConfiguration().getNotEnoughDisplayName());
                meta.setLore(plugin.getConfiguration().getNotEnoughLore());
                current.setItemMeta(meta);

                event.setCancelled(true);
                return;
            }

            key = new NamespacedKey(plugin, "fromRouletteChip");
            String chipName = container.get(key, PersistentDataType.STRING);
            if (chipName == null) return;

            // Set the clicked chip to the player and show it in the game.
            game.getChips().put(player.getUniqueId(), plugin.getChips().getByName(chipName));
            game.nextChip(player.getUniqueId());

            // Remove the money and close inventory.
            RUtils.handleMessage(player, plugin.getMessages().getSelected(plugin.getEconomy().format(money)));
            RUtils.handleMessage(player, plugin.getMessages().getControl());

            plugin.getEconomy().withdrawPlayer(player, money);
            plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
        } else if (hasDisplayName && displayName.equalsIgnoreCase(plugin.getConfiguration().getDisplayName("next"))) {
            // Next page.
            holder.getGUI().nextPage(event.getClick().isShiftClick());
        } else if (hasDisplayName && displayName.equalsIgnoreCase(plugin.getConfiguration().getDisplayName("previous"))) {
            // Previous page.
            holder.getGUI().previousPage(event.getClick().isShiftClick());
        } else if (hasDisplayName && displayName.equalsIgnoreCase(plugin.getConfiguration().getDisplayName("exit"))) {
            // Close inventory and leave the game.
            game.removePlayer(player, false);
            plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
        }
        event.setCancelled(true);
    }
}