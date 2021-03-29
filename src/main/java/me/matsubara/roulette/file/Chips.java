package me.matsubara.roulette.file;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.data.Chip;
import me.matsubara.roulette.util.RUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Chips {

    private final Roulette plugin;
    private final List<Chip> chips;

    private File file;
    private FileConfiguration configuration;

    public Chips(Roulette plugin) {
        this.plugin = plugin;
        this.chips = new ArrayList<>();
        load();
    }

    private void load() {
        file = new File(plugin.getDataFolder(), "chips.yml");
        if (!file.exists()) {
            plugin.saveResource("chips.yml", false);
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
        chips.clear();

        ConfigurationSection section = getConfig().getConfigurationSection("chips");
        if (section == null) return;

        int loaded = 0;

        for (String path : section.getKeys(false)) {
            String displayName = hasDisplayName(path) ? getDisplayName(path) : null;
            List<String> lore = hasLore(path) ? getLore(path) : null;
            String url = getConfig().getString("chips." + path + ".url");
            double price = getConfig().getDouble("chips." + path + ".price");

            Chip chip = new Chip(path, displayName, lore, url, price);
            chips.add(chip);
            loaded++;
        }

        if (loaded > 0) {
            plugin.getLogger().info("All chips have been loaded from chips.yml!");
            chips.sort(Comparator.comparing(Chip::getPrice));
            return;
        }

        plugin.getLogger().info("No chips have been loaded from chips.yml, why don't you create one?");
    }

    private boolean hasDisplayName(String path) {
        return getConfig().contains("chips." + path + ".display-name", false);
    }

    private boolean hasLore(String path) {
        return getConfig().contains("chips." + path + ".lore", false);
    }

    private String getDisplayName(String path) {
        return RUtils.translate(getConfig().getString("chips." + path + ".display-name"));
    }

    private List<String> getLore(String path) {
        return RUtils.translate(getConfig().getStringList("chips." + path + ".lore"));
    }

    public Double getMinAmount() {
        if (chips.isEmpty()) return null;
        return chips.get(0).getPrice();
    }

    public Chip getByName(String name) {
        for (Chip chip : chips) {
            if (chip.getName().equalsIgnoreCase(name)) return chip;
        }
        return null;
    }

    public void reloadConfig() {
        try {
            configuration = new YamlConfiguration();
            configuration.load(file);
            update();
        } catch (IOException | InvalidConfigurationException exception) {
            exception.printStackTrace();
        }
    }

    public List<Chip> getChipsList() {
        return chips;
    }

    public FileConfiguration getConfig() {
        return configuration;
    }
}