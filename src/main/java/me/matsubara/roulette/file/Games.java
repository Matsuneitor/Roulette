package me.matsubara.roulette.file;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.game.GameData;
import me.matsubara.roulette.game.GameType;
import me.matsubara.roulette.util.RUtils;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

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
        load();
    }

    private void load() {
        file = new File(plugin.getDataFolder(), "games.yml");
        if (!file.exists()) {
            plugin.saveResource("games.yml", false);
        }
        configuration = new YamlConfiguration();
        try {
            configuration.load(file);
            update(false, null);
        } catch (IOException | InvalidConfigurationException exception) {
            exception.printStackTrace();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void update(boolean isReload, @Nullable Player creator) {
        cancelGamesCreation();

        ConfigurationSection section = getConfig().getConfigurationSection("games");
        if (section == null) return;

        Set<String> keys = section.getKeys(false);

        int loaded = 0;

        // Get values from games.
        for (String path : keys) {
            loaded++;

            // If the game type from games.yml is wrong, set AMERICAN by default.
            GameType type;
            try {
                type = GameType.valueOf(getConfig().getString("games." + path + ".type"));
            } catch (IllegalArgumentException exception) {
                type = GameType.AMERICAN;
            }

            // Check that the location from the file isn't null.
            Location location = (Location) getConfig().get("games." + path + ".location");
            Validate.notNull(location, "Location can't be null.");

            String uuid = getConfig().getString("games." + path + ".npc-uuid");
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
                        current.delete(false, true);
                        iterator.remove();
                        break;
                    }

                    shouldRecreate = false;
                    current.restart();
                    current.setLimitPlayers(minPlayers, maxPlayers);
                    current.setupJoinHologram(String.format(Game.S_PLAYING, path));
                    saveGame(current);
                    break;
                }
            }

            if (shouldRecreate) {
                GameData data = new GameData(path, type, location, null, npcUUID, minPlayers, maxPlayers, true, creator);
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

            // Cancel the creation task.
            if (current.getTask() != null) plugin.getServer().getScheduler().cancelTask(current.getTask());

            // Tell the creator of the game that the creation task has been canceled.
            if (current.getData().getCreator() != null) {
                Game.CREATING.remove(current.getData().getCreator().getUniqueId());
                RUtils.handleMessage(current.getData().getCreator(), plugin.getMessages().getCancelled(current.getName()));
            }

            // If has NPC, remove it.
            if (current.getNPC() == null) {
                NPC npc = CitizensAPI.getNPCRegistry().getByUniqueId(current.getData().getNPCUUID());
                if (npc != null) npc.destroy();
            }

            current.delete(false, true);
            iterator.remove();
        }
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
        getConfig().set("games." + game.getName() + ".type", game.getType().toString());
        getConfig().set("games." + game.getName() + ".location", game.getLocation());

        String uuid = (game.getNPC() == null) ? "null" : game.getNPC().getUniqueId().toString();
        getConfig().set("games." + game.getName() + ".npc-uuid", uuid);

        getConfig().set("games." + game.getName() + ".min-players", game.getMinPlayers());
        getConfig().set("games." + game.getName() + ".max-players", game.getMaxPlayers());
        saveConfig();
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

    public void reloadConfig(@Nullable Player creator) {
        try {
            configuration = new YamlConfiguration();
            configuration.load(file);
            update(true, creator);
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