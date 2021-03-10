package me.matsubara.roulette.listener;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.gui.GUIHolder;
import me.matsubara.roulette.util.RUtilities;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;
import java.util.Optional;

public final class InventoryClick implements Listener {

    private final Roulette plugin;

    public InventoryClick(Roulette plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        // If the entity who click the inventory isn't a player, return.
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        // If the clicked inventory is null, return.
        if (event.getClickedInventory() == null) return;

        // If the clicked item is null, return.
        if (event.getCurrentItem() == null) return;

        // If the clicked item doesn't have meta, return.
        if (event.getCurrentItem().getItemMeta() == null) return;

        ItemStack current = event.getCurrentItem();
        Inventory inventory = event.getClickedInventory();

        // If the clicked inventory holder isn't an instance of GUIHolder, return.
        if (!(inventory.getHolder() instanceof GUIHolder)) return;

        GUIHolder holder = (GUIHolder) inventory.getHolder();
        NamespacedKey key = new NamespacedKey(plugin, "fromRouletteMoney");

        // Check if the playe belongs to a game, else return.
        Game game = plugin.getGames().getGameByPlayer(player);
        if (game == null) return;

        // Play click sound.
        XSound.play(game.getSpinHologram().getLocation(), plugin.getConfiguration().getClickSound());

        boolean hasDisplayName = current.getItemMeta().hasDisplayName();
        String displayName = current.getItemMeta().getDisplayName();

        // If the clicked item has our money identity key.
        if (current.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.DOUBLE)) {
            Double money = current.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.DOUBLE);

            // If somehow the store value is null, return.
            if (money == null) return;

            // Check if the player has the required money for this chip.
            if (!plugin.getEconomy().has(player, money)) {
                Optional<XMaterial> material = XMaterial.matchXMaterial(plugin.getConfiguration().getNotEnoughMaterial());
                if (material.isPresent()) {
                    current.setType(Objects.requireNonNull(material.get().parseMaterial()));
                    ItemMeta meta = current.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(plugin.getConfiguration().getNotEnoughDisplayName());
                        meta.setLore(plugin.getConfiguration().getNotEnoughLore());
                        current.setItemMeta(meta);
                    }
                }
                event.setCancelled(true);
                return;
            }

            NamespacedKey keyChip = new NamespacedKey(plugin, "fromRouletteChip");
            String chipName = current.getItemMeta().getPersistentDataContainer().get(keyChip, PersistentDataType.STRING);

            // If somehow the store value is null, return.
            if (chipName == null) {
                return;
            }

            // Set the clicked chip to the player and show it in the game.
            game.getChips().put(player.getUniqueId(), plugin.getChips().getByName(chipName));
            game.nextChip(player.getUniqueId());

            // Remove the money and close inventory.
            RUtilities.handleMessage(player, plugin.getMessages().getSelected(plugin.getEconomy().format(money)));
            RUtilities.handleMessage(player, plugin.getMessages().getControl());
            plugin.getEconomy().withdrawPlayer(player, money);
            plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
        } else if (hasDisplayName && displayName.equalsIgnoreCase(plugin.getConfiguration().getDisplayName("next"))) {
            // Previous page.
            holder.getGUI().nextPage(event.getClick().isShiftClick());
        } else if (hasDisplayName && displayName.equalsIgnoreCase(plugin.getConfiguration().getDisplayName("previous"))) {
            // Next page.
            holder.getGUI().previousPage(event.getClick().isShiftClick());
        } else if (hasDisplayName && displayName.equalsIgnoreCase(plugin.getConfiguration().getDisplayName("exit"))) {
            // Close inventory and leave the game.
            game.removePlayer(player, false);
            plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
        }
        event.setCancelled(true);
    }
}