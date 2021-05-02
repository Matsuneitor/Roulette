package me.matsubara.roulette.hologram;

import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.listener.holographic.TouchHandler;
import me.matsubara.roulette.util.CyclicPlaceholderReplacer;
import me.matsubara.roulette.util.RUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class Hologram_Holographic implements Hologram {

    private final Game game;

    private final com.gmail.filoghost.holographicdisplays.api.Hologram hologram;

    public Hologram_Holographic(Game game, Location location) {
        this.game = game;
        this.hologram = HologramsAPI.createHologram(game.getPlugin(), location);
        this.hologram.setAllowPlaceholders(true);
        registerRainbowPlaceholder();
    }

    @Override
    public void addLine(String line, boolean touchable) {
        TextLine text = hologram.appendTextLine(line);
        if (touchable) text.setTouchHandler(new TouchHandler(game.getPlugin(), game));
    }

    @Override
    public void setLine(int line, String text) {
        ((TextLine) hologram.getLine(line)).setText(text);
    }

    @Override
    public int size() {
        return hologram.size();
    }

    @Override
    public void clearLines() {
        hologram.clearLines();
    }

    @Override
    public boolean isDeleted() {
        return hologram == null || hologram.isDeleted();
    }

    @Override
    public void showTo(Player player) {
        hologram.getVisibilityManager().showTo(player);
    }

    @Override
    public void hideTo(Player player) {
        hologram.getVisibilityManager().hideTo(player);
    }

    @Override
    public void setVisibleByDefault(boolean visible) {
        hologram.getVisibilityManager().setVisibleByDefault(visible);
    }

    @Override
    public void resetVisibilityAll() {
        hologram.getVisibilityManager().resetVisibilityAll();
    }

    @Override
    public void teleport(Player player, Location location) {
        hologram.teleport(location);
    }

    @Override
    public Location getLocation() {
        return hologram.getLocation();
    }

    @Override
    public void delete() {
        if (hologram.isDeleted()) return;
        hologram.delete();
    }

    private void registerRainbowPlaceholder() {
        if (HologramsAPI.getRegisteredPlaceholders(game.getPlugin()).contains("&u")) return;

        HologramsAPI.registerPlaceholder(game.getPlugin(), "&u", 0.2d, new CyclicPlaceholderReplacer(RUtils.arrayToStrings(
                ChatColor.RED,
                ChatColor.GOLD,
                ChatColor.YELLOW,
                ChatColor.GREEN,
                ChatColor.AQUA,
                ChatColor.LIGHT_PURPLE)));
    }
}