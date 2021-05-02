package me.matsubara.roulette.file;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.game.GameData;
import me.matsubara.roulette.game.GameType;
import me.matsubara.roulette.npc.NPC;
import me.matsubara.roulette.util.RUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class Games {

    private final Roulette plugin;
    private final Logger logger;

    private final List<GameData> gameDatas;
    private final Set<Game> games;

    private boolean isRunning;

    private File file;
    private FileConfiguration configuration;

    public Games(Roulette plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.gameDatas = new ArrayList<>();
        this.games = new HashSet<>();
        this.isRunning = false;
        if (!hasMultiverse()) load();
    }

    private boolean hasMultiverse() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Multiverse-Core")) return false;

        // If the server is using MC, then we'll wait until every world has been loaded.
        MultiverseCore core = (MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
        if (core == null) return false;

        MVWorldManager manager = core.getMVWorldManager();

        // If all worlds has been loaded, then load every game.
        if (manager.getUnloadedWorlds().isEmpty()) {
            load();
            return true;
        }

        logger.info("Multiverse-Core has been detected, waiting for all worlds to be loaded...");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (manager.getUnloadedWorlds().isEmpty()) {
                    logger.info("All the worlds have been loaded, ready to load the games!");
                    load();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        return true;
    }

    private void load() {
        file = new File(plugin.getDataFolder(), "games.yml");
        if (!file.exists()) {
            plugin.saveResource("games.yml", false);
        }
        configuration = new YamlConfiguration();
        try {
            configuration.load(file);

            // Remove entities from MC worlds, if present.
            if (Bukkit.getPluginManager().isPluginEnabled("Multiverse-Core")) {
                MultiverseCore core = (MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
                if (core != null) {
                    MVWorldManager manager = core.getMVWorldManager();
                    plugin.removeEntities(manager.getMVWorlds().stream().map(MultiverseWorld::getCBWorld).collect(Collectors.toList()));
                }
            }

            update(false);
        } catch (IOException | InvalidConfigurationException exception) {
            exception.printStackTrace();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void update(boolean isReload) {
        cancelGamesCreation();

        ConfigurationSection section = getConfig().getConfigurationSection("games");
        if (section == null) return;

        Set<String> keys = section.getKeys(false);

        int loaded = 0;

        // Get values from games.
        for (String path : keys) {
            loaded++;

            boolean betAll = getConfig().getBoolean("games." + path + ".bet-all");
            int startTime = getConfig().getInt("games." + path + ".start-time");

            String uuid = getConfig().getString("games." + path + ".creator-uuid");
            UUID creatorUUID = uuid.equalsIgnoreCase("null") ? null : UUID.fromString(uuid);

            uuid = getConfig().getString("games." + path + ".account-uuid");
            UUID accountUUID = uuid.equalsIgnoreCase("null") ? null : UUID.fromString(uuid);

            // If the game type from games.yml is wrong, set AMERICAN by default.
            GameType type;
            try {
                type = GameType.valueOf(getConfig().getString("games." + path + ".type"));
            } catch (IllegalArgumentException exception) {
                type = GameType.AMERICAN;
            }

            // Check that the location from the file isn't null.
            Location location = loadLocation(path);
            if (location == null) {
                plugin.getLogger().severe("Location from game " + path + " is null, can't be created.");
                continue;
            }

            uuid = getConfig().getString("games." + path + ".npc-uuid");
            UUID npcUUID = uuid.equalsIgnoreCase("null") ? null : UUID.fromString(uuid);

            int minPlayers = getConfig().getInt("games." + path + ".min-players");
            int maxPlayers = getConfig().getInt("games." + path + ".max-players");

            boolean shouldRecreate = true;

            // If is a reload, we'll check if some values has changed, if so, we'll check if the game should be recreated (or not).
            if (isReload) {
                Iterator<Game> iterator = games.iterator();

                while (iterator.hasNext()) {
                    Game current = iterator.next();
                    if (!current.getName().equalsIgnoreCase(path)) continue;

                    if (current.getType() != type || !current.getLocation().equals(location) || !current.getNPCUUID().equals(npcUUID)) {
                        shouldRecreate = true;
                        current.delete(false, true, false);
                        iterator.remove();
                        break;
                    }

                    if (accountUUID != null && current.getAccount() != null && current.getAccount().getUniqueId() != accountUUID) {
                        OfflinePlayer player = Bukkit.getOfflinePlayer(accountUUID);
                        if (player != null && player.hasPlayedBefore()) current.setAccount(player);
                    }

                    shouldRecreate = false;
                    current.restart();
                    current.setLimitPlayers(minPlayers, maxPlayers);
                    current.setupJoinHologram(String.format(Roulette.USE_HOLOGRAPHIC ? Game.HD_PLAYING : Game.CMI_PLAYING, path));
                    current.setStartTime(startTime);
                    current.setBetAll(betAll);
                    saveGame(current);
                    break;
                }
            }

            if (shouldRecreate) {
                GameData data = new GameData(path, betAll, startTime, creatorUUID, accountUUID, type, location, null, npcUUID, minPlayers, maxPlayers, true);
                gameDatas.add(data);
            }
        }

        if (loaded > 0) {
            logger.info("All games have been loaded from games.yml!");
            createNextName();
            return;
        }

        logger.info("No games have been loaded from games.yml, why don't you create one?");
    }

    // For reload, if any game hasn't finish, then we cancel and delete it.
    private void cancelGamesCreation() {
        isRunning = false;

        Iterator<Game> iterator = games.iterator();
        while (iterator.hasNext()) {
            Game current = iterator.next();
            if (current.isDone()) continue;
            cancelGameCreation(current, iterator);
        }
    }

    public void cancelGameCreation(Game game, @Nullable Iterator<Game> iterator) {
        if (gameDatas.isEmpty() && isRunning) isRunning = false;
        // Cancel the creation task.
        if (game.getTask() != null) plugin.getServer().getScheduler().cancelTask(game.getTask());

        // Tell the creator of the game that the creation task has been cancelled.
        if (game.getData().getCreator() != null) {
            Game.CREATING.remove(game.getData().getCreator());

            Player player = Bukkit.getPlayer(game.getData().getCreator());
            if (player == null) return;

            RUtils.handleMessage(player, Messages.Message.CANCELLED.asString().replace("%name%", game.getName()));
        }

        // If has NPC, remove it.
        if (game.getNPC() == null) {
            NPC npc = RUtils.getNPCByUniqueId(game.getData().getNPCUUID(), game);
            if (npc != null) npc.destroy();
        }

        game.delete(false, iterator != null, false);
        if (iterator != null) iterator.remove();
    }

    public void createNextName() {
        if (gameDatas.isEmpty()) {
            isRunning = false;
            return;
        }
        GameData data = gameDatas.get(0);
        new Game(plugin, data);

        gameDatas.remove(0);
        isRunning = true;
    }

    public void saveGame(Game game) {
        games.add(game);
        getConfig().set("games." + game.getName() + ".bet-all", game.isBetAll());
        getConfig().set("games." + game.getName() + ".start-time", game.getStartTime());
        // Save the UUID of the creator.
        String creatorUUID = (game.getData().getCreator() == null) ? "null" : game.getData().getCreator().toString();
        getConfig().set("games." + game.getName() + ".creator-uuid", creatorUUID);

        // Save the UUID of the player account, if exist.
        String accountUUID = (game.getAccount() == null) ? "null" : game.getAccount().getUniqueId().toString();
        getConfig().set("games." + game.getName() + ".account-uuid", accountUUID);

        getConfig().set("games." + game.getName() + ".type", game.getType().toString());
        saveLocation(game.getName(), game.getLocation());
        //getConfig().set("games." + game.getName() + ".location", game.getLocation());

        // Save the UUID of the NPC, if exist.
        String npcUUID = (game.getNPC() == null) ? "null" : game.getNPC().getUniqueId().toString();
        getConfig().set("games." + game.getName() + ".npc-uuid", npcUUID);

        getConfig().set("games." + game.getName() + ".min-players", game.getMinPlayers());
        getConfig().set("games." + game.getName() + ".max-players", game.getMaxPlayers());
        saveConfig();
    }

    @SuppressWarnings("ConstantConditions")
    private Location loadLocation(String path) {
        String worldName = getConfig().getString("games." + path + ".location.world");
        double x = getConfig().getDouble("games." + path + ".location.x");
        double y = getConfig().getDouble("games." + path + ".location.y");
        double z = getConfig().getDouble("games." + path + ".location.z");
        float yaw = (float) getConfig().getDouble("games." + path + ".location.yaw");
        float pitch = (float) getConfig().getDouble("games." + path + ".location.pitch");

        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            // Maybe the world doens't exist anymore.
            if (!plugin.hasDependency("Multiverse-Core")) return null;

            // If is using MC, we tried to get the world from the plugin.
            MultiverseCore core = (MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
            if (core != null) {
                MVWorldManager manager = core.getMVWorldManager();

                MultiverseWorld mvWorld = manager.getMVWorld(worldName);
                if (mvWorld == null) return null;

                world = mvWorld.getCBWorld();
            } else return null;
        }

        return new Location(world, x, y, z, yaw, pitch);
    }

    private void saveLocation(String path, Location location) {
        Validate.notNull(location.getWorld(), "World can't be null.");
        getConfig().set("games." + path + ".location.world", location.getWorld().getName());
        getConfig().set("games." + path + ".location.x", location.getX());
        getConfig().set("games." + path + ".location.y", location.getY());
        getConfig().set("games." + path + ".location.z", location.getZ());
        getConfig().set("games." + path + ".location.yaw", location.getYaw());
        getConfig().set("games." + path + ".location.pitch", location.getPitch());
    }

    public void deleteGame(Game game, boolean isIterator) {
        getConfig().set("games." + game.getName(), null);
        if (!isIterator) games.remove(game);
        saveConfig();
    }

    private void saveConfig() {
        try {
            getConfig().save(file);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void reloadConfig() {
        try {
            configuration = new YamlConfiguration();
            configuration.load(file);
            update(true);
        } catch (IOException | InvalidConfigurationException exception) {
            exception.printStackTrace();
        }
    }

    public Game getGameByName(String name) {
        for (Game game : games) {
            if (game.getName().equalsIgnoreCase(name)) return game;
        }
        return null;
    }

    public Game getGameByPlayer(Player player) {
        for (Game game : games) {
            if (game.inGame(player)) return game;
        }
        return null;
    }

    public Game getGameByNPCUUID(UUID uuid) {
        for (Game game : games) {
            if (game.getNPC() != null && game.getNPC().getUniqueId().equals(uuid)) return game;
        }
        return null;
    }

    public boolean isUpdate() {
        // Should be called only on start up.
        for (Game game : games) {
            if (!game.isDone() && !game.getData().isUpdate() && game.getData().getCreator() != null) return false;
        }
        return true;
    }

    public List<GameData> getGameDatas() {
        return gameDatas;
    }

    public Set<Game> getGamesSet() {
        return games;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public FileConfiguration getConfig() {
        return configuration;
    }
}