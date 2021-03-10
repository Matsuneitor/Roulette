package me.matsubara.roulette.command;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.game.GameType;
import me.matsubara.roulette.util.RUtilities;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class Main implements CommandExecutor, TabCompleter {

    private final Roulette plugin;

    public Main(Roulette plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings({"NullableProblems", "ConstantConditions"})
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            RUtilities.handleMessage(sender, plugin.getMessages().getFromConsole());
            return true;
        }

        if (!sender.hasPermission("roulette.admin")) {
            RUtilities.handleMessage(sender, plugin.getMessages().getNotPermission());
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0 || args.length > 6) {
            sendHelp(player);
            return true;
        }

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                plugin.reloadConfig();
                plugin.getChips().reloadConfig();
                plugin.getGames().reloadConfig();
                plugin.getMessages().reloadConfig();
                RUtilities.handleMessage(player, plugin.getMessages().getReload());
            } else {
                sendHelp(player);
            }
            return true;
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "delete": {
                    Game game = plugin.getGames().getGameByName(args[1]);
                    if (game != null) {
                        game.delete(true);
                        plugin.getGames().deleteGame(game);
                        RUtilities.handleMessage(player, plugin.getMessages().getDelete(args[1]));
                    } else {
                        RUtilities.handleMessage(player, plugin.getMessages().getUnknown(args[1]));
                    }
                    break;
                }
                case "purge": {
                    Game game = plugin.getGames().getGameByName(args[1]);
                    if (game != null) {
                        game.delete(true);
                        plugin.getGames().deleteGame(game);
                    } else {
                        NamespacedKey key = new NamespacedKey(plugin, "fromRoulette");

                        for (Entity entity : player.getWorld().getEntities()) {
                            if (!entity.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                                continue;
                            }

                            if (entity.getPersistentDataContainer().get(key, PersistentDataType.STRING).equalsIgnoreCase(args[1])) {
                                entity.remove();
                            }
                        }
                    }
                    RUtilities.handleMessage(player, plugin.getMessages().getPurge(args[1]));
                    break;
                }
                default:
                    sendHelp(player);
                    break;
            }
            return true;
        }

        if (plugin.getGames().getGameByName(args[1]) != null) {
            RUtilities.handleMessage(player, plugin.getMessages().getExist(args[1]));
            return true;
        }

        if (Game.CREATING.contains(player.getUniqueId())) {
            RUtilities.handleMessage(player, plugin.getMessages().getCreating());
            return true;
        }

        String npc = Arrays.asList("none", "null").contains(args[2].toLowerCase()) ? null : args[2];

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

        Game game = new Game(plugin, args[1], RUtilities.getCorrectLocation(player), npc, null, min, max, type);
        game.createGame(player, true);
        return true;
    }

    private void sendHelp(Player player) {
        plugin.getMessages().getHelp().forEach(line -> player.sendMessage(RUtilities.translate(line)));
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("roulette.admin")) {
            return null;
        }
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("create", "delete", "purge", "reload"), new ArrayList<>());
        }
        if (args.length == 2 && Arrays.asList("create", "delete").contains(args[0].toLowerCase())) {
            boolean isCreate = args[0].equalsIgnoreCase("create");
            return StringUtil.copyPartialMatches(args[1], isCreate ? Collections.singletonList("<name>") : getGamesNames(), new ArrayList<>());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return StringUtil.copyPartialMatches(args[2], Collections.singletonList("<croupier>"), new ArrayList<>());
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            return StringUtil.copyPartialMatches(args[3], Arrays.asList("european", "american"), new ArrayList<>());
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("create")) {
            return StringUtil.copyPartialMatches(args[4], Collections.singletonList("<min>"), new ArrayList<>());
        }
        if (args.length == 6 && args[0].equalsIgnoreCase("create")) {
            return StringUtil.copyPartialMatches(args[5], Collections.singletonList("<max>"), new ArrayList<>());
        }
        return null;
    }

    private List<String> getGamesNames() {
        return plugin.getGames().getList().stream().map(Game::getName).sorted().collect(Collectors.toList());
    }
}