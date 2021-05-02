package me.matsubara.roulette.hologram;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMILocation;
import com.Zrips.CMI.Modules.Holograms.CMIHologram;
import me.matsubara.roulette.game.Game;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;

public final class Hologram_CMI implements Hologram {

    private final CMIHologram hologram;

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    public Hologram_CMI(Game game, String name, Location location, boolean isJoin) {
        this.hologram = new CMIHologram(name, new CMILocation(location));

        // Update placeholders every 0.15 seconds.
        this.hologram.setUpdateIntervalSec(0.15d);

        // Set te show range to 64 (4 chunks).
        this.hologram.setShowRange(64);

        // Set the update range to 64 (4 chunks).
        this.hologram.setUpdateRange(64);

        if (isJoin) {
            // Set hologram interactable, so we can click it.
            this.hologram.setInteractable(true);

            // Set the commands to be executed when interacting with the hologram.
            this.hologram.setCommands(Arrays.asList("asConsole! roulette join [playerName] " + game.getName()));
        }

        // Hide particles by default.
        this.hologram.setShowParticle(false);

        // Add hologram to the hologram manager.
        CMI.getInstance().getHologramManager().addHologram(this.hologram);

        // Update hologram so it's shown to the players.
        this.hologram.update();
    }

    @Override
    public void addLine(String line, boolean touchable) {
        hologram.addLine(line);
        hologram.refresh();
    }

    @Override
    public void setLine(int line, String text) {
        hologram.setLine(line, text);
        hologram.refresh();
    }

    @Override
    public int size() {
        return hologram.getLines().size();
    }

    @Override
    public void clearLines() {
        hologram.setLines(new ArrayList<>());
        hologram.refresh();
    }

    @Override
    public boolean isDeleted() {
        return hologram == null;
    }

    @Override
    public void showTo(Player player) {
        hologram.addLastHoloInRange(player.getUniqueId());
        hologram.addLastHoloInRangeExtra(player.getUniqueId());
    }

    @Override
    public void hideTo(Player player) {
        hologram.removeLastHoloInRange(player.getUniqueId());
        hologram.removeLastHoloInRangeExtra(player.getUniqueId());
    }

    @Override
    public void setVisibleByDefault(boolean visible) {
        // Instead of using the hide methods (they work like shit), we're just enabling/disabling the hologram.

        if (!visible) {
            hologram.disable();
            return;
        }

        hologram.enable();
    }

    @Override
    public void resetVisibilityAll() {
        // Not needed for CMI.
        assert true;
    }

    @Override
    public void teleport(Player player, Location location) {
        Vector offset = new Vector(0.0d, 0.25d, 0.0d);
        hologram.superficialMoveTo(player, location.clone().subtract(offset));
    }

    @Override
    public Location getLocation() {
        return hologram.getLocation().getBukkitLoc();
    }

    @Override
    public void delete() {
        // Remove hologram from the hologram manager.
        if (hologram == null) return;
        setVisibleByDefault(true);
        hologram.remove();
    }
}