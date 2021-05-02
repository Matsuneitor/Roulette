package me.matsubara.roulette.file;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.npc.NPC;
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

    private static Roulette plugin;

    private File file;
    private FileConfiguration configuration;

    public Messages(Roulette plugin) {
        Messages.plugin = plugin;
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

    public String getRandomNPCMessage(@Nullable NPC npc, String type) {
        String npcName = npc.getName().equalsIgnoreCase("") ? null : npc.getName();
        String message;
        switch (type) {
            case "invitations":
                message = getMessage(Message.INVITATIONS.asList());
                break;
            case "bets":
                message = getMessage(Message.BETS.asList());
                break;
            case "no-bets":
                message = getMessage(Message.NO_BETS.asList());
                break;
            default:
                message = getMessage(Message.WINNER.asList());
                break;
        }
        if (npcName == null || Message.CROUPIER_PREFIX.asString().equalsIgnoreCase("")) return message;
        return Message.CROUPIER_PREFIX.asString().replace("%croupier%", npcName).concat(message);
    }

    private String getMessage(List<String> list) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return list.get(random.nextInt(list.size()));
    }

    public List<String> getYourBet(String bet, String numbers, String chance) {
        List<String> list = Message.YOUR_BET.asList();
        list.replaceAll(line -> line
                .replace("%bet%", bet)
                .replace("%numbers%", numbers)
                .replace("%chance%", String.valueOf(chance)));
        return RUtils.translate(list);
    }

    public String getJoinMessage(String name, int online, int max) {
        return RUtils.translate(Message.JOIN.asString()
                .replace("%player%", name)
                .replace("%playing%", String.valueOf(online))
                .replace("%max%", String.valueOf(max)));
    }

    public String getLeaveMessage(String name, int online, int max) {
        return RUtils.translate(Message.LEAVE.asString()
                .replace("%player%", name)
                .replace("%playing%", String.valueOf(online))
                .replace("%max%", String.valueOf(max)));
    }

    public List<String> getWinners(int amount, String winners) {
        List<String> list = Message.WINNERS.asList();
        list.replaceAll(line -> line
                .replace("%amount%", String.valueOf(amount))
                .replace("%winners%", winners));
        return RUtils.translate(list);
    }

    public String getPrice(String amount, int multiplier) {
        return RUtils.translate(Message.PRICE.asString()
                .replace("%amount%", amount)
                .replace("%multiplier%", String.valueOf(multiplier)));
    }

    public FileConfiguration getConfig() {
        return configuration;
    }

    public enum Message {
        CROUPIER_PREFIX("messages.npc.croupier-prefix"),
        INVITATIONS("messages.npc.invitations"),
        BETS("messages.npc.bets"),
        NO_BETS("messages.npc.no-bets"),
        WINNER("messages.npc.winner"),
        FROM_CONSOLE("messages.plugin.from-console"),
        NOT_PERMISSION("messages.plugin.not-permission"),
        RELOAD("messages.plugin.reload"),
        CREATE("messages.commands.create"),
        DELETE("messages.commands.delete"),
        PURGE("messages.commands.purge"),
        EXIST("messages.commands.exist"),
        UNKNOWN("messages.commands.unknown"),
        CANCELLED("messages.commands.cancelled"),
        CREATING("messages.commands.creating"),
        WAIT("messages.commands.wait"),
        SINTAX("messages.commands.sintax"),
        STARTING("messages.states.starting"),
        SELECT_BET("messages.states.select-bet"),
        SPINNING("messages.states.spinning"),
        OUT_OF_TIME("messages.states.out-of-time"),
        YOUR_BET("messages.states.your-bet"),
        SPINNING_START("messages.states.spinning-start"),
        JOIN("messages.states.join"),
        LEAVE("messages.states.leave"),
        NO_WINNER("messages.states.no-winner"),
        WINNERS("messages.states.winners"),
        PRICE("messages.states.price"),
        RESTART("messages.states.restart"),
        LEAVE_PLAYER("messages.states.leave-player"),
        ALREADY_INGAME("messages.others.already-ingame"),
        ALREADY_STARTED("messages.others.already-started"),
        MIN_REQUIRED("messages.others.min-required"),
        CONFIRM("messages.others.confirm"),
        CONFIRM_LOSE("messages.others.confirm-lose"),
        SELECTED_AMOUNT("messages.others.selected-amount"),
        CONTROL("messages.others.control"),
        ACCOUNT("messages.others.account"),
        NO_ACCOUNT("messages.others.no-account"),
        UNKNOWN_ACCOUNT("messages.others.unknown-account"),
        RECEIVED("messages.others.received"),
        VANISH("messages.others.vanish"),
        HELP("messages.help");

        private final String path;

        Message(String path) {
            this.path = path;
        }

        public String asString() {
            return RUtils.translate(plugin.getMessages().getConfig().getString(path));
        }

        public List<String> asList() {
            return RUtils.translate(plugin.getMessages().getConfig().getStringList(path));
        }
    }
}