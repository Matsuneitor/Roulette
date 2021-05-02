package me.matsubara.roulette.hologram;

import org.bukkit.Location;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public interface Hologram {

    /**
     * Add a new line to the hologram.
     */
    void addLine(String line, boolean touchable);

    /**
     * Replace the line at a specific index.
     */
    void setLine(int line, String text);

    /**
     * Checks if the hologram is deleted.
     */
    boolean isDeleted();

    /**
     * Returns the amount of lines of the hologram.
     */
    int size();

    /**
     * Remove all the lines of the hologram.
     */
    void clearLines();

    /**
     * Show the hologram to certain player.
     */
    void showTo(Player player);

    /**
     * Hide the hologram to certain player.
     */
    void hideTo(Player player);

    /**
     * Set the visible state of the hologram.
     */
    void setVisibleByDefault(boolean visible);

    /**
     * Reset the visibility of the hologram to all players.
     */
    void resetVisibilityAll();

    /**
     * Teleport the hologram to certain location.
     */
    void teleport(Player player, Location location);

    /**
     * Returns the location of the hologram.
     */
    Location getLocation();

    /**
     * Delete the hologram.
     */
    void delete();
}