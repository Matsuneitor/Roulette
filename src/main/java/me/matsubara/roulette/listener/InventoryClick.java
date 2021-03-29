package me.matsubara.roulette.listener;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.data.Chip;
import me.matsubara.roulette.file.Configuration;
import me.matsubara.roulette.file.Messages;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.gui.GUIHolder;
import me.matsubara.roulette.gui.GUIType;
import me.matsubara.roulette.listener.npc.NPCRightClick;
import me.matsubara.roulette.util.ItemBuilder;
import me.matsubara.roulette.util.RUtils;
import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.EconomyResponse;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

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

        boolean hasDisplayName = current.getItemMeta().hasDisplayName();
        String displayName = current.getItemMeta().getDisplayName();

        // Play click sound.
        player.playSound(player.getLocation(), Sound.valueOf(Configuration.Config.SOUND_CLICK.asString()), 1.0f, 1.0f);

        if (holder.getType() == GUIType.MAIN) {
            int min = holder.getGame().getMinPlayers(), max = holder.getGame().getMaxPlayers();

            String noAccount = plugin.getConfiguration().getDisplayName("game-menu", "no-account");
            String minAmount = plugin.getConfiguration().getDisplayName("game-menu", "min-amount");
            String maxAmount = plugin.getConfiguration().getDisplayName("game-menu", "max-amount");
            String close = plugin.getConfiguration().getDisplayName("game-menu", "close");

            if (hasDisplayName && (displayName.equalsIgnoreCase(minAmount) || displayName.equalsIgnoreCase(maxAmount))) {
                setLimitPlayers(event, holder.getGame(), min, max, displayName.equalsIgnoreCase(maxAmount));
            } else if (current.getType() == Material.PLAYER_HEAD || (hasDisplayName && displayName.equalsIgnoreCase(noAccount))) {
                if (current.getType() == Material.PLAYER_HEAD && event.getClick() == ClickType.RIGHT) {
                    holder.getGame().setAccount(null);
                    RUtils.handleMessage(player, Messages.Message.NO_ACCOUNT.asString());
                    event.setCurrentItem(plugin.getConfiguration().getItem("game-menu", "no-account", null));
                } else {
                    // To check if the player clicked the result slot.
                    player.setMetadata("notClicked", new FixedMetadataValue(plugin, true));
                    openAnvilGUI(player, holder.getGame());
                }
            } else if (hasDisplayName && displayName.equalsIgnoreCase(close)) {
                if (event.getClick() == ClickType.RIGHT) {
                    if (!player.hasPermission("roulette.delete")) {
                        RUtils.handleMessage(player, Messages.Message.NOT_PERMISSION.asString());
                    } else {
                        if (!holder.getGame().getData().getCreator().equals(player.getUniqueId()) && !player.hasPermission("roulette.delete.others")) {
                            RUtils.handleMessage(player, Messages.Message.NOT_PERMISSION.asString());
                            return;
                        }
                        holder.getGame().delete(true, false, false);
                        RUtils.handleMessage(player, Messages.Message.DELETE.asString().replace("%name%", holder.getGame().getName()));
                    }
                }
                plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
            }
            event.setCancelled(true);
            return;
        } else if (holder.getType() == GUIType.CONFIRM) {
            switch (current.getType()) {
                case LIME_STAINED_GLASS_PANE:
                    double money = plugin.getEconomy().getBalance(player);
                    Chip customChip = new Chip(player.getName(), null, null, "e36e94f6c34a35465fce4a90f2e25976389eb9709a12273574ff70fd4daa6852", money);

                    // Set the clicked chip to the player and show it in the game.
                    holder.getGame().getChips().put(player.getUniqueId(), customChip);
                    holder.getGame().nextChip(player.getUniqueId());

                    // Remove the money and close inventory.
                    RUtils.handleMessage(player, Messages.Message.SELECTED_AMOUNT.asString().replace("%money%", plugin.getEconomy().format(money)));
                    RUtils.handleMessage(player, Messages.Message.CONTROL.asString());

                    EconomyResponse response = plugin.getEconomy().withdrawPlayer(player, money);
                    if (!response.transactionSuccess()) {
                        plugin.getLogger().info(String.format("It wasn't possible to withdraw $%s to %s.", money, player.getName()));
                        break;
                    }

                    plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
                case RED_STAINED_GLASS_PANE:
                    plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
                    break;
            }
            event.setCancelled(true);
            return;
        }

        Game game = holder.getGame();
        if (game == null) return;

        PersistentDataContainer container = current.getItemMeta().getPersistentDataContainer();

        NamespacedKey key = new NamespacedKey(plugin, "fromRouletteMoney");
        if (container.has(key, PersistentDataType.DOUBLE)) {
            Double money = container.get(key, PersistentDataType.DOUBLE);
            if (money == null) return;

            // Check if the player has the required money for this chip.
            if (!plugin.getEconomy().has(player, money)) {

                Material material;
                try {
                    material = Material.valueOf(Configuration.Config.NOT_ENOUGH_MONEY_MATERIAL.asString());
                } catch (IllegalArgumentException exception) {
                    return;
                }

                current.setType(material);

                ItemMeta meta = current.getItemMeta();
                if (meta == null) return;

                meta.setDisplayName(Configuration.Config.NOT_ENOUGH_MONEY_DISPLAY_NAME.asString());
                meta.setLore(Configuration.Config.NOT_ENOUGH_MONEY_LORE.asList());
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
            RUtils.handleMessage(player, Messages.Message.SELECTED_AMOUNT.asString().replace("%money%", plugin.getEconomy().format(money)));
            RUtils.handleMessage(player, Messages.Message.CONTROL.asString());

            EconomyResponse response = plugin.getEconomy().withdrawPlayer(player, money);
            if (!response.transactionSuccess()) {
                plugin.getLogger().info(String.format("It wasn't possible to withdraw $%s to %s.", money, player.getName()));
                return;
            }

            plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
        } else if (hasDisplayName && displayName.equalsIgnoreCase(plugin.getConfiguration().getDisplayName("shop", "next"))) {
            // Next page.
            holder.getGUI().nextPage(event.getClick().isShiftClick());
        } else if (hasDisplayName && displayName.equalsIgnoreCase(plugin.getConfiguration().getDisplayName("shop", "previous"))) {
            // Previous page.
            holder.getGUI().previousPage(event.getClick().isShiftClick());
        } else if (hasDisplayName && displayName.equalsIgnoreCase(plugin.getConfiguration().getDisplayName("shop", "exit"))) {
            // Close inventory and leave the game.
            game.removePlayer(player, false);
            plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
        } else if (hasDisplayName && displayName.equalsIgnoreCase(plugin.getConfiguration().getDisplayName("shop", "bet-all"))) {
            openConfirmGUI(player, game);
        }
        event.setCancelled(true);
    }

    private void openConfirmGUI(Player player, Game game) {
        String title = Configuration.Config.CONFIRM_GUI_TITLE.asString();
        Inventory inventory = Bukkit.createInventory(new GUIHolder(null, GUIType.CONFIRM, game), 9, title);

        String confirm = Configuration.Config.CONFIRM_GUI_CONFIRM.asString();
        for (int i = 0; i < 4; i++) {
            inventory.setItem(i, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayName(confirm).build());
        }

        inventory.setItem(4, plugin.getConfiguration().getItem("shop", "bet-all", null));

        String cancel = Configuration.Config.CONFIRM_GUI_CANCEL.asString();
        for (int i = 5; i < 9; i++) {
            inventory.setItem(i, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayName(cancel).build());
        }

        player.openInventory(inventory);
    }

    @SuppressWarnings("deprecation")
    private void openAnvilGUI(Player player, Game game) {
        new AnvilGUI.Builder()
                .onComplete((opener, text) -> {
                    // Play click sound.
                    opener.playSound(opener.getLocation(), Sound.valueOf(Configuration.Config.SOUND_CLICK.asString()), 1.0f, 1.0f);

                    OfflinePlayer target = Bukkit.getOfflinePlayer(ChatColor.stripColor(text));
                    if (target.hasPlayedBefore()) {
                        game.setAccount(target);
                        RUtils.handleMessage(opener, Messages.Message.ACCOUNT.asString());
                    } else {
                        RUtils.handleMessage(opener, Messages.Message.UNKNOWN_ACCOUNT.asString());
                    }
                    opener.removeMetadata("notClicked", plugin);
                    return AnvilGUI.Response.close();
                })
                .onClose(opener -> {
                    if (opener.hasMetadata("notClicked")) {
                        opener.removeMetadata("notClicked", plugin);
                        plugin.getServer().getScheduler().runTask(plugin, () -> NPCRightClick.openMainGUI(opener, game));
                    }
                })
                .item(new ItemStack(Material.PAPER))
                .title(Configuration.Config.SEARCH_TITLE.asString())
                .text(Configuration.Config.SEARCH_TEXT.asString())
                .plugin(plugin)
                .open(player);
    }

    @SuppressWarnings("ConstantConditions")
    private void setLimitPlayers(InventoryClickEvent event, Game game, int min, int max, boolean isMax) {
        if (event.getClick() == ClickType.LEFT) {
            game.setLimitPlayers(!isMax ? min - 1 : min, !isMax ? max : max - 1);
        } else if (event.getClick() == ClickType.RIGHT) {
            game.setLimitPlayers(!isMax ? min + 1 : min, !isMax ? max : max + 1);
        }

        int current = !isMax ? game.getMinPlayers() : game.getMaxPlayers();
        if (event.getCurrentItem().getAmount() != current) {
            event.getCurrentItem().setAmount(current);
            event.getClickedInventory().getItem(!isMax ? 14 : 12).setAmount(!isMax ? game.getMaxPlayers() : game.getMinPlayers());
        }

        game.restart();
        game.setupJoinHologram(String.format(Game.S_PLAYING, game.getName()));
        plugin.getGames().saveGame(game);
    }
}