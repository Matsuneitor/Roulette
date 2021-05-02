package me.matsubara.roulette.listener;

import com.cryptomorin.xseries.XMaterial;
import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.data.Chip;
import me.matsubara.roulette.file.Configuration;
import me.matsubara.roulette.file.Messages;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.gui.GUIHolder;
import me.matsubara.roulette.gui.GUIType;
import me.matsubara.roulette.util.ItemBuilder;
import me.matsubara.roulette.util.RUtils;
import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.EconomyResponse;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
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
            String startTime = plugin.getConfiguration().getStartTimeDisplayName(holder.getGame().getStartTime());
            String laPartage = plugin.getConfiguration().getDisplayName("game-menu", "la-partage");
            String enPrison = plugin.getConfiguration().getDisplayName("game-menu", "en-prison");
            String surrender = plugin.getConfiguration().getDisplayName("game-menu", "surrender");

            String state = holder.getGame().isBetAll() ? Configuration.Config.STATE_ENABLED.asString() : Configuration.Config.STATE_DISABLED.asString();
            String betAll = plugin.getConfiguration().getDisplayName("game-menu", "bet-all").replace("%state%", state);

            String close = plugin.getConfiguration().getDisplayName("game-menu", "close");

            if (hasDisplayName && (displayName.equalsIgnoreCase(minAmount) || displayName.equalsIgnoreCase(maxAmount))) {
                setLimitPlayers(event, holder.getGame(), min, max, displayName.equalsIgnoreCase(maxAmount));
            } else if (isAccountItem(current) || (hasDisplayName && displayName.equalsIgnoreCase(noAccount))) {
                if (current.getType() == XMaterial.PLAYER_HEAD.parseMaterial() && event.getClick() == ClickType.RIGHT) {
                    holder.getGame().setAccount(null);
                    RUtils.handleMessage(player, Messages.Message.NO_ACCOUNT.asString());
                    event.setCurrentItem(plugin.getConfiguration().getItem("game-menu", "no-account", null));
                } else {
                    // To check if the player clicked the result slot.
                    player.setMetadata("notClicked", new FixedMetadataValue(plugin, true));
                    openAnvilGUI(player, holder.getGame());
                }
            } else if (hasDisplayName && displayName.equalsIgnoreCase(startTime)) {
                setStartTime(event, holder.getGame());
            } else if (hasDisplayName && (displayName.equalsIgnoreCase(laPartage) || displayName.equalsIgnoreCase(enPrison) || displayName.equalsIgnoreCase(surrender))) {
                // TODO: Add special rules in the next update.
                if (current.containsEnchantment(Enchantment.ARROW_DAMAGE)) {
                    current.removeEnchantment(Enchantment.ARROW_DAMAGE);
                } else {
                    current.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 1);
                    if (!current.getItemMeta().hasItemFlag(ItemFlag.HIDE_ENCHANTS)) {
                        ItemMeta meta = current.getItemMeta();
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                        current.setItemMeta(meta);
                    }
                }
            } else if (hasDisplayName && displayName.equalsIgnoreCase(betAll)) {
                setBetAll(event, holder.getGame());
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
        } else if (holder.getType() == GUIType.CONFIRM_BET_ALL || holder.getType() == GUIType.CONFIRM_LEAVE) {
            switch (current.getType()) {
                case LIME_STAINED_GLASS_PANE:
                    if (holder.getType() == GUIType.CONFIRM_LEAVE) {
                        // Close inventory and leave the game.
                        holder.getGame().removePlayer(player, false);
                        plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
                        event.setCancelled(true);
                        return;
                    }

                    double money = plugin.getEconomy().getBalance(player);

                    String skin;

                    // If the @bet-all item has URL, use it. Otherwise, use a default one.
                    if (plugin.getConfiguration().hasUrl("shop", "bet-all")) {
                        skin = plugin.getConfiguration().getUrl("shop", "bet-all");
                    } else {
                        skin = "e36e94f6c34a35465fce4a90f2e25976389eb9709a12273574ff70fd4daa6852";
                    }

                    Chip customChip = new Chip(player.getName(), null, null, skin, money);

                    // TODO: Test
                    // If the bet-all money is the same of one chip from chips.yml, use that chip.
                    for (Chip chip : plugin.getChips().getChipsList()) {
                        if (money == chip.getPrice()) customChip = chip;
                    }

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
                    break;
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

            // If has "fromRouletteMoney" should have this too.
            String chipName = container.get(new NamespacedKey(plugin, "fromRouletteChip"), PersistentDataType.STRING);
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
            openConfirmGUI(player, game, plugin.getConfiguration().getItem("shop", "bet-all", null), GUIType.CONFIRM_BET_ALL);
        }
        event.setCancelled(true);
    }

    private boolean isAccountItem(ItemStack item) {
        //noinspection ConstantConditions, already checked in the event.
        return item.getType() == XMaterial.PLAYER_HEAD.parseMaterial() && ((SkullMeta) item.getItemMeta()).hasOwner();
    }

    public static void openConfirmGUI(Player player, Game game, ItemStack center, GUIType type) {
        String title = Configuration.Config.CONFIRM_GUI_TITLE.asString();
        Inventory inventory = Bukkit.createInventory(new GUIHolder(null, type, game), 9, title);

        String confirm = Configuration.Config.CONFIRM_GUI_CONFIRM.asString();
        for (int i = 0; i < 4; i++) {
            inventory.setItem(i, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setDisplayName(confirm).build());
        }

        inventory.setItem(4, center);

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
                        plugin.getServer().getScheduler().runTask(plugin, () -> RUtils.openMainGUI(opener, game));
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
            event.getClickedInventory().getItem(!isMax ? 12 : 11).setAmount(!isMax ? game.getMaxPlayers() : game.getMinPlayers());
        }

        game.restart();
        game.setupJoinHologram(String.format(Roulette.USE_HOLOGRAPHIC ? Game.HD_PLAYING : Game.CMI_PLAYING, game.getName()));
        plugin.getGames().saveGame(game);
    }

    @SuppressWarnings("ConstantConditions")
    private void setStartTime(InventoryClickEvent event, Game game) {
        int time = game.getStartTime();
        if (event.getClick() == ClickType.LEFT) {
            time -= 5;
            if (time < 5) time = 5;
        } else if (event.getClick() == ClickType.RIGHT) {
            time += 5;
            if (time > 60) time = 60;
        }

        game.setStartTime(time);

        if (event.getCurrentItem().getAmount() != time) {
            event.getCurrentItem().setAmount(time);

            ItemMeta meta = event.getCurrentItem().getItemMeta();
            if (meta == null) return;

            meta.setDisplayName(plugin.getConfiguration().getStartTimeDisplayName(time));
            event.getCurrentItem().setItemMeta(meta);
        }

        game.restart();
        plugin.getGames().saveGame(game);
    }

    @SuppressWarnings("ConstantConditions")
    private void setBetAll(InventoryClickEvent event, Game game) {
        boolean betAll = !game.isBetAll();

        game.setBetAll(betAll);

        String state = game.isBetAll() ? Configuration.Config.STATE_ENABLED.asString() : Configuration.Config.STATE_DISABLED.asString();

        ItemMeta meta = event.getCurrentItem().getItemMeta();
        meta.setDisplayName(plugin.getConfiguration().getDisplayName("game-menu", "bet-all").replace("%state%", state));
        if (!meta.hasItemFlag(ItemFlag.HIDE_ATTRIBUTES)) meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        if (betAll) {
            event.getCurrentItem().addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 1);
        } else {
            if (event.getCurrentItem().containsEnchantment(Enchantment.ARROW_DAMAGE)) {
                event.getCurrentItem().removeEnchantment(Enchantment.ARROW_DAMAGE);
            }
        }

        event.getCurrentItem().setItemMeta(meta);

        game.restart();
        plugin.getGames().saveGame(game);
    }
}