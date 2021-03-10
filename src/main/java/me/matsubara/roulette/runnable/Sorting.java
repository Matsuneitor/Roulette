package me.matsubara.roulette.runnable;

import com.cryptomorin.xseries.XSound;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.data.Slot;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.util.RUtilities;
import net.citizensnpcs.util.PlayerAnimation;
import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

public final class Sorting extends BukkitRunnable {

    private final Roulette plugin;
    private final Game game;
    private final boolean isEuropean;
    private final Slot[] slots;

    // Loop variables.
    private int time;

    public Sorting(Roulette plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
        this.isEuropean = game.getType().isEuropean();

        this.slots = new Slot[isEuropean ? 37 : 38];

        this.time = plugin.getConfiguration().getCountdownSorting() * 20;

        game.broadcast(plugin.getMessages().getSpinningStart());
        System.arraycopy(Slot.getValues(isEuropean), 0, slots, 0, isEuropean ? 37 : 38);
    }

    @Override
    public void run() {
        if (time == 0) {
            ((TextLine) game.getSpinHologram().getLine(0)).setText(plugin.getConfiguration().getWinningHologram());

            // Stop NPC animation, check if there're winners and stop.
            PlayerAnimation.STOP_SNEAKING.play((Player) game.getNPC().getEntity());
            game.checkWinner();
            game.setEnding();
            cancel();
            return;
        }

        // Select a random number.
        int which = ThreadLocalRandom.current().nextInt(0, isEuropean ? 37 : 38);

        // If the spin hologram is empty, create the lines, else update them.
        if (game.getSpinHologram().size() == 0) {
            game.getSpinHologram().appendTextLine(plugin.getConfiguration().getSpinningHologram());
            game.getSpinHologram().appendTextLine(RUtilities.getSlotName(slots[which]));
        } else {
            Validate.notNull(game.getLocation().getWorld(), "World can't be null.");
            ((TextLine) game.getSpinHologram().getLine(1)).setText(RUtilities.getSlotName(slots[which]));
            XSound.play(game.getSpinHologram().getLocation(), plugin.getConfiguration().getSpinningSound());
            game.setWinner(slots[which]);
        }
        time--;
    }
}