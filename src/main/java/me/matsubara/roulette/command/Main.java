package me.matsubara.roulette.command;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.game.GameData;
import me.matsubara.roulette.game.GameType;
import me.matsubara.roulette.util.RUtils;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.StringUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Main implements CommandExecutor, TabCompleter {

    private final Roulette plugin;

    public Main(Roulette plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings({"NullableProblems", "ConstantConditions"})
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            RUtils.handleMessage(sender, plugin.getMessages().getFromConsole());
            return true;
        }

        if (!sender.hasPermission("roulette.admin")) {
            RUtils.handleMessage(sender, plugin.getMessages().getNotPermission());
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0 || args.length > 6) {
            sendHelp(player);
            return true;
        }

        if (args.length == 1) {
            if (!args[0].equalsIgnoreCase("reload")) {
                sendHelp(player);
                return true;
            }
            plugin.getLogger().info("Reloading " + plugin.getDescription().getFullName());

            plugin.reloadConfig();
            plugin.getChips().reloadConfig();
            plugin.getGames().reloadConfig(player);
            plugin.getMessages().reloadConfig();

            RUtils.handleMessage(player, plugin.getMessages().getReload());
            return true;
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "delete": {
                    Game game = plugin.getGames().getGameByName(args[1]);
                    if (game != null) {
                        game.delete(true, false);
                        RUtils.handleMessage(player, plugin.getMessages().getDelete(args[1]));
                    } else {
                        RUtils.handleMessage(player, plugin.getMessages().getUnknown(args[1]));
                    }
                    break;
                }
                case "purge": {
                    Game game = plugin.getGames().getGameByName(args[1]);
                    if (game != null) game.delete(true, false);
                    else {
                        for (Entity entity : player.getWorld().getEntities()) {
                            PersistentDataContainer container = entity.getPersistentDataContainer();

                            NamespacedKey key = new NamespacedKey(plugin, "fromRoulette");
                            if (!container.has(key, PersistentDataType.STRING)) continue;
                            if (!container.get(key, PersistentDataType.STRING).equalsIgnoreCase(args[1])) continue;

                            entity.remove();
                        }
                    }
                    RUtils.handleMessage(player, plugin.getMessages().getPurge(args[1]));
                    break;
                }
                default:
                    sendHelp(player);
                    break;
            }
            return true;
        }

        if (plugin.getGames().getGameByName(args[1]) != null) {
            RUtils.handleMessage(player, plugin.getMessages().getExist(args[1]));
            return true;
        }

        if (Game.CREATING.contains(player.getUniqueId())) {
            RUtils.handleMessage(player, plugin.getMessages().getCreating());
            return true;
        }

        if (plugin.getGames().isRunning() && plugin.getGames().isUpdate()) {
            RUtils.handleMessage(player, plugin.getMessages().getWait());
            return true;
        }

        String npc = notEquals(args[2].toLowerCase(), "none|null") ? args[2] : null;

        GameType type = null;
        Integer min = null, max = null;

        try {
            type = GameType.valueOf(args[3].toUpperCase());
            min = Integer.parseInt(args[4]);
            max = Integer.parseInt(args[5]);
        } catch (IllegalArgumentException exception) {
            sendHelp(player);
            return true;
        } catch (IndexOutOfBoundsException exception) {
            if (type == null) type = GameType.AMERICAN;
            if (min == null) min = 1;
            if (max == null) max = 10;
        }

        Location location = getCorrectLocation(player);

        GameData data = new GameData(args[1], type, location, npc, null, min, max, false, player);
        plugin.getGames().getGameDatas().add(data);

        // If create next game is running, no need to call the method again.
        if (!plugin.getGames().isRunning()) plugin.getGames().createNextName();
        return true;
    }

    private void sendHelp(Player player) {
        plugin.getMessages().getHelp().forEach(line -> player.sendMessage(RUtils.translate(line)));
    }

    private Location getCorrectLocation(Player player) {
        BlockFace fromYaw = RUtils.faceFromYaw(player.getLocation().getYaw(), false);

        Location location = player.getTargetBlock(null, 5).getLocation();
        location.setDirection(RUtils.getDirection(RUtils.getNextFace(fromYaw).getOppositeFace()));
        return location;
    }

    @SuppressWarnings({"NullableProblems"})
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("roulette.admin")) return null;

        switch (args.length) {
            case 1:
                return getCompletions(args[0], "create", "delete", "purge", "reload");
            case 2:
                if (notEquals(args[0], "create|delete")) return null;
                String[] completions = notEquals(args[0], "create") ? getGames() : new String[]{"<name>"};
                return (completions.length == 0) ? null : getCompletions(args[1], completions);
            case 3:
                if (notEquals(args[0], "create")) return null;
                return getCompletions(args[2], "<croupier|none/null>");
            case 4:
                if (notEquals(args[0], "create")) return null;
                return getCompletions(args[3], "european", "american");
            case 5:
                if (notEquals(args[0], "create")) return null;
                return getCompletions(args[4], "<min>");
            case 6:
                if (notEquals(args[0], "create")) return null;
                return getCompletions(args[5], "<max>");
        }
        return null;
    }

    private boolean notEquals(String argument, @Nullable String expected) {
        return expected != null && !ArrayUtils.contains(expected.split("\\|"), argument);
    }

    private ArrayList<String> getCompletions(String argument, String... completions) {
        Iterable<String> iterable = (completions.length > 1) ? Arrays.asList(completions) : Collections.singletonList(completions[0]);

        return StringUtil.copyPartialMatches(argument, iterable, new ArrayList<>());
    }

    private String[] getGames() {
        return plugin.getGames().getGamesSet().stream().map(Game::getName).sorted().toArray(String[]::new);
    }
}