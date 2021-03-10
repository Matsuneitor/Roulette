package me.matsubara.roulette.file;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.util.RUtilities;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("ConstantConditions")
public final class Messages {

    private final Roulette plugin;

    private File file;
    private FileConfiguration configuration;

    public Messages(Roulette plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        configuration = new YamlConfiguration();
        try {
            configuration.load(file);
        } catch (IOException | InvalidConfigurationException exception) {
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

    public List<String> getInvitations() {
        return RUtilities.translate(getConfig().getStringList("messages.invitations"));
    }

    public String getRandomInvitation() {
        return getInvitations().get(ThreadLocalRandom.current().nextInt(getInvitations().size()));
    }

    public String getFromConsole() {
        return RUtilities.translate(getConfig().getString("messages.from-console"));
    }

    public String getNotPermission() {
        return RUtilities.translate(getConfig().getString("messages.not-permission"));
    }

    public String getReload() {
        return RUtilities.translate(getConfig().getString("messages.reload"));
    }

    public String getCreate(String name) {
        return RUtilities.translate(getConfig().getString("messages.create")
                .replace("%name%", name));
    }

    public String getDelete(String name) {
        return RUtilities.translate(getConfig().getString("messages.delete")
                .replace("%name%", name));
    }

    public String getPurge(String name) {
        return RUtilities.translate(getConfig().getString("messages.purge")
                .replace("%name%", name));
    }

    public String getExist(String name) {
        return RUtilities.translate(getConfig().getString("messages.exist")
                .replace("%name%", name));
    }

    public String getUnknown(String name) {
        return RUtilities.translate(getConfig().getString("messages.unknown")
                .replace("%name%", name));
    }

    public String getStarting(int seconds) {
        return RUtilities.translate(getConfig().getString("messages.starting")
                .replace("%seconds%", String.valueOf(seconds)));
    }

    public String getSelectBet() {
        return RUtilities.translate(getConfig().getString("messages.select-bet"));
    }

    public String getSorting(int seconds) {
        return RUtilities.translate(getConfig().getString("messages.spinning")
                .replace("%seconds%", String.valueOf(seconds)));
    }

    public String getOutOfTime() {
        return RUtilities.translate(getConfig().getString("messages.out-of-time"));
    }

    public List<String> getYourBet(String bet, String numbers, String chance) {
        List<String> list = getConfig().getStringList("messages.your-bet");
        list.replaceAll(line -> line
                .replace("%bet%", bet)
                .replace("%numbers%", numbers)
                .replace("%chance%", String.valueOf(chance)));
        return RUtilities.translate(list);
    }

    public String getJoinMessage(String name, int online, int max) {
        return RUtilities.translate(getConfig().getString("messages.join")
                .replace("%player%", name)
                .replace("%playing%", String.valueOf(online))
                .replace("%max%", String.valueOf(max)));
    }

    public String getLeaveMessage(String name, int online, int max) {
        return RUtilities.translate(getConfig().getString("messages.leave")
                .replace("%player%", name)
                .replace("%playing%", String.valueOf(online))
                .replace("%max%", String.valueOf(max)));
    }

    public String getNoWinner() {
        return RUtilities.translate(getConfig().getString("messages.no-winner"));
    }

    public List<String> getWinners(int amount, String winners) {
        List<String> list = getConfig().getStringList("messages.winners");
        list.replaceAll(line -> line
                .replace("%amount%", String.valueOf(amount))
                .replace("%winners%", winners));
        return RUtilities.translate(list);
    }

    public String getPrice(String amount, int multiplier) {
        return RUtilities.translate(getConfig().getString("messages.price")
                .replace("%amount%", amount)
                .replace("%multiplier%", String.valueOf(multiplier)));
    }

    public String getRestart() {
        return RUtilities.translate(getConfig().getString("messages.restart"));
    }

    public String getAlreadyInGame() {
        return RUtilities.translate(getConfig().getString("messages.already-ingame"));
    }

    public String getAlreadyStarted() {
        return RUtilities.translate(getConfig().getString("messages.already-started"));
    }

    public String getMinRequired(String min) {
        return RUtilities.translate(getConfig().getString("messages.min-required")
                .replace("%money%", min));
    }

    public String getLeavePlayer() {
        return RUtilities.translate(getConfig().getString("messages.leave-player"));
    }

    public String getConfirm() {
        return RUtilities.translate(getConfig().getString("messages.confirm"));
    }

    public String getConfirmLose() {
        return RUtilities.translate(getConfig().getString("messages.confirm-lose"));
    }

    public String getSelected(String amount) {
        return RUtilities.translate(getConfig().getString("messages.selected-amount")
                .replace("%money%", amount));
    }

    public String getControl() {
        return RUtilities.translate(getConfig().getString("messages.control"));
    }

    public String getSpinningStart() {
        return RUtilities.translate(getConfig().getString("messages.spinning-start"));
    }

    public String getCreating() {
        return RUtilities.translate(getConfig().getString("messages.creating"));
    }

    public List<String> getHelp() {
        return RUtilities.translate(getConfig().getStringList("messages.help"));
    }

    public FileConfiguration getConfig() {
        return configuration;
    }
}