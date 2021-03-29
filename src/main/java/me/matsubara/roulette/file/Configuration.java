package me.matsubara.roulette.file;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.util.ItemBuilder;
import me.matsubara.roulette.util.RUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

public final class Configuration {

    private static Roulette plugin;

    public Configuration(Roulette plugin) {
        Configuration.plugin = plugin;
    }

    public ItemStack getBall() {
        try {
            return new ItemStack(Material.valueOf(Config.CROUPIER_BALL.asString()));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public long getPeriod() {
        return (long) (((double) Config.RESTART_TIME.asInt() / Config.RESTART_FIREWORKS.asInt()) * 20L);
    }

    public String getColumnOrDozen(String type, int index) {
        return RUtils.translate(plugin.getConfig().getString("slots." + type + "." + index));
    }

    public String getAccountDisplayName(String name) {
        return getDisplayName("game-menu", "account").replace("%player%", name);
    }

    public List<String> getAccountLore() {
        return getLore("game-menu", "account");
    }

    public String getChipDisplayName(double price) {
        return getDisplayName("shop", "chip").replace("%money%", plugin.getEconomy().format(price));
    }

    public List<String> getChipLore() {
        return getLore("shop", "chip");
    }

    public ItemStack getItem(String gui, String type, @Nullable String money) {
        Material material = Material.valueOf(getMaterial(gui, type));

        String displayName = (money != null) ? getDisplayName(gui, type).replace("%money%", money) : getDisplayName(gui, type);

        if (isSkull(gui, type, material)) {
            return new ItemBuilder(getUrl(gui, type)).setDisplayName(displayName).setLore(getLore(gui, type)).build();
        }
        return new ItemBuilder(material).setDisplayName(displayName).setLore(getLore(gui, type)).build();
    }

    private boolean isSkull(String gui, String type, Material material) {
        return plugin.getConfig().contains(gui + "." + type + ".url", false) && material == Material.PLAYER_HEAD;
    }

    private String getUrl(String gui, String path) {
        return plugin.getConfig().getString(gui + "." + path + ".url");
    }

    private String getMaterial(String gui, String path) {
        return plugin.getConfig().getString(gui + "." + path + ".material");
    }

    public String getDisplayName(String gui, String path) {
        return RUtils.translate(plugin.getConfig().getString(gui + "." + path + ".display-name"));
    }

    public List<String> getLore(String gui, String path) {
        return RUtils.translate(plugin.getConfig().getStringList(gui + "." + path + ".lore"));
    }

    public enum Config {
        DEBUG("debug"),
        UPDATE_CHECKER("update-checker"),
        LOOK_DISTANCE("look-distance"),
        SWAP_CHAIR("swap-schair"),
        INSTANT_EXPLODE("instant-explode"),
        FIX_CHAIR_CAMERA("fix-chair-camera"),
        NPC_LOOK_AROUND("npc-look-around"),
        NPC_IMITATE("npc-imitate"),
        NPC_INVITE("npc-invite"),
        INVITE_INTERVAL("invite-interval"),
        LEAVE_CONFIRM_INTERVAL("leave-confirm-interval"),
        MOVE_CHIP_INTERVAL("move-chip-interval"),
        CROUPIER_BALL("croupier-ball.material"),
        CROUPIER_BALL_SPEED("croupier-ball.speed"),
        COUNTDOWN_WAITING("countdown.waiting"),
        COUNTDOWN_SELECTING("countdown.selecting"),
        COUNTDOWN_SORTING("countdown.sorting"),
        RESTART_TIME("restart.time"),
        RESTART_FIREWORKS("restart.fireworks"),
        SOUND_CLICK("sound.click"),
        SOUND_COUNTDOWN("sound.countdown"),
        SOUND_SPINNING("sound.spinning"),
        SOUND_SWAP_CHAIR("sound.swap-chair"),
        SOUND_SELECT("sound.select"),
        PROGRESS("progress"),
        PROGRESS_CHARACTER("progress-character"),
        SPINNING("spin-holograms.spinning"),
        WINNING_NUMBER("spin-holograms.winning-number"),
        SINGLE_ZERO("slots.single.zero"),
        SINGLE_RED("slots.single.red"),
        SINGLE_BLACK("slots.single.black"),
        LOW("slots.other.low"),
        HIGH("slots.other.high"),
        EVEN("slots.other.even"),
        ODD("slots.other.odd"),
        RED("slots.other.red"),
        BLACK("slots.other.black"),
        TYPE_EUROPEAN("type.european"),
        TYPE_AMERICAN("type.american"),
        SEARCH_TITLE("search.title"),
        SEARCH_TEXT("search.text"),
        CONFIRM_GUI_TITLE("confirm-gui.title"),
        CONFIRM_GUI_CONFIRM("confirm-gui.confirm"),
        CONFIRM_GUI_CANCEL("confirm-gui.cancel"),
        JOIN_HOLOGRAM("join-hologram"),
        SELECT_HOLOGRAM("select-hologram"),
        NOT_ENOUGH_MONEY_MATERIAL("not-enough-money.material"),
        NOT_ENOUGH_MONEY_DISPLAY_NAME("not-enough-money.display-name"),
        NOT_ENOUGH_MONEY_LORE("not-enough-money.lore"),
        SHOP_TITLE("shop.title"),
        GAME_MENU_TITLE("game-menu.title");

        private final String path;

        Config(String path) {
            this.path = path;
        }

        public String asString() {
            return RUtils.translate(plugin.getConfig().getString(path));
        }

        public List<String> asList() {
            return RUtils.translate(plugin.getConfig().getStringList(path));
        }

        public boolean asBoolean() {
            return plugin.getConfig().getBoolean(path);
        }

        public int asInt() {
            return plugin.getConfig().getInt(path);
        }

        public long asLong() {
            return plugin.getConfig().getLong(path);
        }

        public double asDouble() {
            return plugin.getConfig().getDouble(path);
        }

        public char asChar() {
            return asString().charAt(0);
        }
    }
}