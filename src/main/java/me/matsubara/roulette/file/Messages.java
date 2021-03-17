package me.matsubara.roulette.file;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.util.RUtils;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.annotation.Nullable;
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

    public String getCroupierPrefix() {
        return RUtils.translate(getConfig().getString("messages.croupier-prefix"));
    }

    public List<String> getInvitations() {
        return RUtils.translate(getConfig().getStringList("messages.invitations"));
    }

    public String getRandomInvitation(@Nullable String npcName) {
        String invitation = getInvitations().get(ThreadLocalRandom.current().nextInt(getInvitations().size()));
        if (npcName == null || getCroupierPrefix().equalsIgnoreCase("")) return invitation;

        return getCroupierPrefix().replace("%croupier%", npcName).concat(invitation);
    }

    public String getFromConsole() {
        return RUtils.translate(getConfig().getString("messages.from-console"));
    }

    public String getNotPermission() {
        return RUtils.translate(getConfig().getString("messages.not-permission"));
    }

    public String getReload() {
        return RUtils.translate(getConfig().getString("messages.reload"));
    }

    public String getCreate(String name) {
        return RUtils.translate(getConfig().getString("messages.create").replace("%name%", name));
    }

    public String getDelete(String name) {
        return RUtils.translate(getConfig().getString("messages.delete").replace("%name%", name));
    }

    public String getPurge(String name) {
        return RUtils.translate(getConfig().getString("messages.purge").replace("%name%", name));
    }

    public String getExist(String name) {
        return RUtils.translate(getConfig().getString("messages.exist").replace("%name%", name));
    }

    public String getUnknown(String name) {
        return RUtils.translate(getConfig().getString("messages.unknown").replace("%name%", name));
    }

    public String getStarting(int seconds) {
        return RUtils.translate(getConfig().getString("messages.starting").replace("%seconds%", String.valueOf(seconds)));
    }

    public String getSelectBet() {
        return RUtils.translate(getConfig().getString("messages.select-bet"));
    }

    public String getSorting(int seconds) {
        return RUtils.translate(getConfig().getString("messages.spinning").replace("%seconds%", String.valueOf(seconds)));
    }

    public String getOutOfTime() {
        return RUtils.translate(getConfig().getString("messages.out-of-time"));
    }

    public List<String> getYourBet(String bet, String numbers, String chance) {
        List<String> list = getConfig().getStringList("messages.your-bet");
        list.replaceAll(line -> line
                .replace("%bet%", bet)
                .replace("%numbers%", numbers)
                .replace("%chance%", String.valueOf(chance)));
        return RUtils.translate(list);
    }

    public String getJoinMessage(String name, int online, int max) {
        return RUtils.translate(getConfig().getString("messages.join")
                .replace("%player%", name)
                .replace("%playing%", String.valueOf(online))
                .replace("%max%", String.valueOf(max)));
    }

    public String getLeaveMessage(String name, int online, int max) {
        return RUtils.translate(getConfig().getString("messages.leave")
                .replace("%player%", name)
                .replace("%playing%", String.valueOf(online))
                .replace("%max%", String.valueOf(max)));
    }

    public String getNoWinner() {
        return RUtils.translate(getConfig().getString("messages.no-winner"));
    }

    public List<String> getWinners(int amount, String winners) {
        List<String> list = getConfig().getStringList("messages.winners");
        list.replaceAll(line -> line
                .replace("%amount%", String.valueOf(amount))
                .replace("%winners%", winners));
        return RUtils.translate(list);
    }

    public String getPrice(String amount, int multiplier) {
        return RUtils.translate(getConfig().getString("messages.price")
                .replace("%amount%", amount)
                .replace("%multiplier%", String.valueOf(multiplier)));
    }

    public String getRestart() {
        return RUtils.translate(getConfig().getString("messages.restart"));
    }

    public String getAlreadyInGame() {
        return RUtils.translate(getConfig().getString("messages.already-ingame"));
    }

    public String getAlreadyStarted() {
        return RUtils.translate(getConfig().getString("messages.already-started"));
    }

    public String getMinRequired(String min) {
        return RUtils.translate(getConfig().getString("messages.min-required")
                .replace("%money%", min));
    }

    public String getLeavePlayer() {
        return RUtils.translate(getConfig().getString("messages.leave-player"));
    }

    public String getConfirm() {
        return RUtils.translate(getConfig().getString("messages.confirm"));
    }

    public String getConfirmLose() {
        return RUtils.translate(getConfig().getString("messages.confirm-lose"));
    }

    public String getSelected(String amount) {
        return RUtils.translate(getConfig().getString("messages.selected-amount").replace("%money%", amount));
    }

    public String getControl() {
        return RUtils.translate(getConfig().getString("messages.control"));
    }

    public String getSpinningStart() {
        return RUtils.translate(getConfig().getString("messages.spinning-start"));
    }

    public String getCreating() {
        return RUtils.translate(getConfig().getString("messages.creating"));
    }

    public String getWait() {
        return RUtils.translate(getConfig().getString("messages.wait"));
    }

    public String getCancelled(String game) {
        return RUtils.translate(getConfig().getString("messages.cancelled").replace("%game%", game));
    }

    public List<String> getHelp() {
        return RUtils.translate(getConfig().getStringList("messages.help"));
    }

    public FileConfiguration getConfig() {
        return configuration;
    }
}