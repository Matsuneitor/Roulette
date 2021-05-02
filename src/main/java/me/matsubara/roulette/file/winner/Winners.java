package me.matsubara.roulette.file.winner;

import me.matsubara.roulette.Roulette;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("unused")
public final class Winners {

    private final Roulette plugin;
    private final Set<Winner> winners;

    private File file;
    private FileConfiguration configuration;

    public Winners(Roulette plugin) {
        this.plugin = plugin;
        this.winners = new HashSet<>();
        load();
    }

    private void load() {
        file = new File(plugin.getDataFolder(), "winners.yml");
        if (!file.exists()) {
            plugin.saveResource("winners.yml", false);
        }
        configuration = new YamlConfiguration();
        try {
            configuration.load(file);
            update();
        } catch (IOException | InvalidConfigurationException exception) {
            exception.printStackTrace();
        }
    }

    private void update() {
        ConfigurationSection winners = getConfig().getConfigurationSection("winners");
        if (winners == null) return;

        for (String path : winners.getKeys(false)) {
            Winner winner = new Winner(UUID.fromString(path));

            ConfigurationSection games = getConfig().getConfigurationSection("winners." + path);
            if (games == null) continue;

            for (String innerPath : games.getKeys(false)) {
                Integer mapId;
                try {
                    String asString = getConfig().getString("winners." + path + "." + innerPath + ".mapId");
                    //noinspection ConstantConditions
                    mapId = Integer.parseInt(asString);
                } catch (NumberFormatException exception) {
                    mapId = null;
                }
                double money = getConfig().getDouble("winners." + path + "." + innerPath + ".money");
                winner.add(mapId, money);
            }

            this.winners.add(winner);
        }
    }

    public void saveWinner(Winner winner) {
        winners.add(winner);

        for (Winner.WinnerData data : winner.getWinnerData()) {
            int index = winner.getWinnerData().indexOf(data) + 1;
            getConfig().set("winners." + winner.getUUID().toString() + "." + index + ".mapId", (data.getMapId() == null) ? "null" : data.getMapId());
            getConfig().set("winners." + winner.getUUID().toString() + "." + index + ".money", data.getMoney());
        }

        saveConfig();
    }

    public void deleteWinner(Winner winner) {
        getConfig().set("winners." + winner.getUUID(), null);
        saveConfig();
    }

    public Winner getByUniqueId(UUID uuid) {
        for (Winner winner : winners) {
            if (winner.getUUID().equals(uuid)) return winner;
        }
        return null;
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
        } catch (IOException | InvalidConfigurationException exception) {
            exception.printStackTrace();
        }
    }

    public Set<Winner> getWinnersSet() {
        return winners;
    }

    public FileConfiguration getConfig() {
        return configuration;
    }
}