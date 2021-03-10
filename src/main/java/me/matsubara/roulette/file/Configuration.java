package me.matsubara.roulette.file;

import com.cryptomorin.xseries.XMaterial;
import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.util.RUtilities;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("ConstantConditions")
public final class Configuration {

    private final Roulette plugin;

    public Configuration(Roulette plugin) {
        this.plugin = plugin;
    }

    public boolean enableDebug() {
        return plugin.getConfig().getBoolean("debug");
    }

    public int getLookDistance() {
        return plugin.getConfig().getInt("look-distance");
    }

    public boolean swapChair() {
        return plugin.getConfig().getBoolean("swap-chair");
    }

    public boolean instantExplode() {
        return plugin.getConfig().getBoolean("instant-explode");
    }

    public boolean fixChairCamera() {
        return plugin.getConfig().getBoolean("fix-chair-camera");
    }

    public boolean npcLookAround() {
        return plugin.getConfig().getBoolean("npc-look-around");
    }

    public boolean npcImitate() {
        return plugin.getConfig().getBoolean("npc-imitate");
    }

    public boolean npcInvite() {
        return plugin.getConfig().getBoolean("npc-invite");
    }

    public long getInviteInterval() {
        return plugin.getConfig().getLong("invite-interval");
    }

    public long getLeaveConfirmInterval() {
        return plugin.getConfig().getLong("leave-confirm-interval");
    }

    public long getMoveChipInterval() {
        return plugin.getConfig().getLong("move-chip-interval");
    }

    public ItemStack getBall() {
        return XMaterial.matchXMaterial(plugin.getConfig().getString("croupier-ball")).map(XMaterial::parseItem).orElse(null);
    }

    public int getCountdownWaiting() {
        return plugin.getConfig().getInt("countdown.waiting");
    }

    public int getCountdownSelecting() {
        return plugin.getConfig().getInt("countdown.selecting");
    }

    public int getCountdownSorting() {
        return plugin.getConfig().getInt("countdown.sorting");
    }

    public int getRestartTime() {
        return plugin.getConfig().getInt("restart.time");
    }

    public int getFireworks() {
        return plugin.getConfig().getInt("restart.fireworks");
    }

    public long getPeriod() {
        return (long) (((double) getRestartTime() / getFireworks()) * 20L);
    }

    public String getClickSound() {
        return plugin.getConfig().getString("sound.click");
    }

    public String getCountdownSound() {
        return plugin.getConfig().getString("sound.countdown");
    }

    public String getSpinningSound() {
        return plugin.getConfig().getString("sound.spinning");
    }

    public String getSwapSound() {
        return plugin.getConfig().getString("sound.swap-chair");
    }

    public String getSelectSound() {
        return plugin.getConfig().getString("sound.select");
    }

    public String getProgress(int percent, String progress, String game) {
        return RUtilities.translate(plugin.getConfig().getString("progress")
                .replace("%percent%", String.valueOf(percent))
                .replace("%progress-bar%", progress)
                .replace("%game%", game));
    }

    public char getProgressChar() {
        return plugin.getConfig().getString("progress-character").charAt(0);
    }

    public String getSpinningHologram() {
        return RUtilities.translate(plugin.getConfig().getString("spin-holograms.spinning"));
    }

    public String getWinningHologram() {
        return RUtilities.translate(plugin.getConfig().getString("spin-holograms.winning-number"));
    }

    public String getZero(String zero) {
        return RUtilities.translate(plugin.getConfig().getString("slots.single.zero")
                .replace("%number%", zero));
    }

    public String getSingleRed(String number) {
        return RUtilities.translate(plugin.getConfig().getString("slots.single.red")
                .replace("%number%", number));
    }

    public String getSingleBlack(String number) {
        return RUtilities.translate(plugin.getConfig().getString("slots.single.black")
                .replace("%number%", number));
    }

    public String getColumnOrDozen(String type, int index) {
        return RUtilities.translate(plugin.getConfig().getString("slots." + type + "." + index));
    }

    public String getLow() {
        return RUtilities.translate(plugin.getConfig().getString("slots.other.low"));
    }

    public String getHigh() {
        return RUtilities.translate(plugin.getConfig().getString("slots.other.high"));
    }

    public String getEven() {
        return RUtilities.translate(plugin.getConfig().getString("slots.other.even"));
    }

    public String getOdd() {
        return RUtilities.translate(plugin.getConfig().getString("slots.other.odd"));
    }

    public String getNameRed() {
        return RUtilities.translate(plugin.getConfig().getString("slots.other.red"));
    }

    public String getNameBlack() {
        return RUtilities.translate(plugin.getConfig().getString("slots.other.black"));
    }

    public String getEuropeanType() {
        return RUtilities.translate(plugin.getConfig().getString("type.european"));
    }

    public String getAmericanType() {
        return RUtilities.translate(plugin.getConfig().getString("type.american"));
    }

    public List<String> getJoinHologram() {
        return RUtilities.translate(plugin.getConfig().getStringList("join-hologram"));
    }

    public List<String> getSelectHologram() {
        return RUtilities.translate(plugin.getConfig().getStringList("select-hologram"));
    }

    public String getNotEnoughMaterial() {
        return plugin.getConfig().getString("not-enough-money.material");
    }

    public String getNotEnoughDisplayName() {
        return RUtilities.translate(plugin.getConfig().getString("not-enough-money.display-name"));
    }

    public List<String> getNotEnoughLore() {
        return RUtilities.translate(plugin.getConfig().getStringList("not-enough-money.display-name"));
    }

    public String getShopTitle(int page, int max) {
        return RUtilities.translate(plugin.getConfig().getString("shop.title")
                .replace("%page%", String.valueOf(page))
                .replace("%max%", String.valueOf(max)));
    }

    public String getChipDisplayName(double price) {
        return getDisplayName("chip")
                .replace("%money%", plugin.getEconomy().format(price));
    }

    public List<String> getChipLore() {
        return getLore("chip");
    }

    public ItemStack getItem(String type, @Nullable String money) {
        Optional<XMaterial> material = XMaterial.matchXMaterial(getMaterial(type));
        if (!material.isPresent()) {
            return null;
        }
        ItemStack item = isSkull(type, material.get()) ? RUtilities.createHead(getUrl(type)) : material.get().parseItem();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((money != null) ? getDisplayName(type).replace("%money%", money) : getDisplayName(type));
            meta.setLore(getLore(type));
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isSkull(String type, XMaterial material) {
        return containsURL(type) && material.parseMaterial() == XMaterial.PLAYER_HEAD.parseMaterial();
    }

    private boolean containsURL(String type) {
        return plugin.getConfig().contains("shop." + type + ".url", false);
    }

    private String getUrl(String path) {
        return plugin.getConfig().getString("shop." + path + ".url");
    }

    private String getMaterial(String path) {
        return plugin.getConfig().getString("shop." + path + ".material");
    }

    public String getDisplayName(String path) {
        return RUtilities.translate(plugin.getConfig().getString("shop." + path + ".display-name"));
    }

    public List<String> getLore(String path) {
        return RUtilities.translate(plugin.getConfig().getStringList("shop." + path + ".lore"));
    }
}