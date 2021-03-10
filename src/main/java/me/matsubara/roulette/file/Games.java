package me.matsubara.roulette.file;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.game.GameType;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public final class Games {

    private final Roulette plugin;
    private final Set<Game> games;
    private final Logger logger;

    private File file;
    private FileConfiguration configuration;

    public Games(Roulette plugin) {
        this.plugin = plugin;
        this.games = new HashSet<>();
        this.logger = plugin.getLogger();
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
            update(false);
        } catch (IOException | InvalidConfigurationException exception) {
            exception.printStackTrace();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void update(boolean isReload) {
        ConfigurationSection section = getConfig().getConfigurationSection("games");
        if (section == null) return;

        int loaded = 0;

        Set<String> keys = section.getKeys(false);

        if (keys.size() > 0) {
            logger.info("Loading " + keys.size() + " game(s) from games.yml...");
        }

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
            Location location = (Location) getConfig().get("games." + path + ".location");
            UUID npcUUID = UUID.fromString(getConfig().getString("games." + path + ".npc-uuid"));
            int minPlayers = getConfig().getInt("games." + path + ".min-players");
            int maxPlayers = getConfig().getInt("games." + path + ".max-players");
            Validate.notNull(location, "Location can't be null.");

            boolean shouldRecreate = true, shouldSave = false;

            // If is a reload, we'll check if the type of the game / the location  has changed, and if so, we'll recreate the game.
            // If the min / max players also changed, we'll only change those values and recreate the hologram.
            if (isReload) {
                Iterator<Game> iterator = games.iterator();

                while (iterator.hasNext()) {
                    Game current = iterator.next();

                    if (!current.getName().equalsIgnoreCase(path)) {
                        shouldSave = true;
                        continue;
                    }

                    if (current.getType() != type || !current.getLocation().equals(location) || !current.getNPCUUID().equals(npcUUID)) {
                        current.delete(false);
                        iterator.remove();
                        shouldRecreate = true;
                        break;
                    }

                    current.setMinPlayers(minPlayers);
                    current.setMaxPlayers(maxPlayers);
                    current.restart();
                    current.setupJoinHologram(String.format(Game.PLACEHOLDER, path));
                    shouldRecreate = false;
                    break;
                }
            }

            if (shouldRecreate) {
                Game game = new Game(plugin, path, location, null, npcUUID, minPlayers, maxPlayers, type);
                game.createGame(null, shouldSave);
                games.add(game);
            }

            logger.info("Loaded " + loaded + " game(s) of " + keys.size() + " from games.yml...");
        }

        if (loaded > 0) {
            logger.info("All games have been loaded from games.yml, now they're ready to be created. Enable @debug in config.yml to see the progress in console.");
            return;
        }
        logger.info("No games have been loaded from games.yml, why don't you create one?");
    }

    public void saveGame(Game game) {
        games.add(game);
        getConfig().set("games." + game.getName() + ".type", game.getType().toString());
        getConfig().set("games." + game.getName() + ".location", game.getLocation());
        getConfig().set("games." + game.getName() + ".npc-uuid", game.getNPC().getUniqueId().toString());
        getConfig().set("games." + game.getName() + ".min-players", game.getMinPlayers());
        getConfig().set("games." + game.getName() + ".max-players", game.getMaxPlayers());
        saveConfig();
    }

    public void deleteGame(Game game) {
        getConfig().set("games." + game.getName(), null);
        games.remove(game);
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

    public Set<Game> getList() {
        return games;
    }

    public FileConfiguration getConfig() {
        return configuration;
    }
}