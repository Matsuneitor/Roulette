package me.matsubara.roulette.runnable;

import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import me.matsubara.roulette.data.Part;
import me.matsubara.roulette.data.Slot;
import me.matsubara.roulette.file.Configuration;
import me.matsubara.roulette.file.Messages;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.util.RUtils;
import net.citizensnpcs.util.PlayerAnimation;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

public final class Sorting extends BukkitRunnable {

    private final Game game;
    private final ArmorStand ball;
    private final boolean isEuropean;
    private final Slot[] slots;

    // Loop variables.
    private int time;

    public Sorting(Game game) {
        this.game = game;
        this.ball = game.getParts().get(Part.BALL);
        //noinspection ConstantConditions, 100% sure won't happen.
        this.ball.getEquipment().setHelmet(new ItemStack(Part.BALL.getMaterial()));
        this.isEuropean = game.getType().isEuropean();

        this.slots = new Slot[isEuropean ? 37 : 38];

        this.time = Configuration.Config.COUNTDOWN_SORTING.asInt() * 20;

        game.broadcast(Messages.Message.SPINNING_START.asString());
        System.arraycopy(Slot.getValues(isEuropean), 0, slots, 0, isEuropean ? 37 : 38);
    }

    @Override
    public void run() {
        if (time == 0) {
            ((TextLine) game.getSpinHologram().getLine(0)).setText(Configuration.Config.WINNING_NUMBER.asString());

            // Stop NPC animation, check if there're winners and stop.
            PlayerAnimation.STOP_SNEAKING.play((Player) game.getNPC().getEntity());
            game.checkWinner();
            game.setEnding();
            cancel();
            return;
        }

        // Spin ball.
        Location location = ball.getLocation();
        location.setYaw(location.getYaw() + (float) Configuration.Config.CROUPIER_BALL_SPEED.asDouble());
        ball.teleport(location);

        // Select a random number.
        int which = ThreadLocalRandom.current().nextInt(0, isEuropean ? 37 : 38);

        // If the spin hologram is empty, create the lines, else update them.
        if (game.getSpinHologram().size() == 0) {
            game.getSpinHologram().appendTextLine(Configuration.Config.SPINNING.asString());
            game.getSpinHologram().appendTextLine(RUtils.getSlotName(slots[which]));
        } else {
            Validate.notNull(game.getLocation().getWorld(), "World can't be null.");
            ((TextLine) game.getSpinHologram().getLine(1)).setText(RUtils.getSlotName(slots[which]));

            Location where = game.getSpinHologram().getLocation();

            Validate.notNull(where.getWorld(), "World can't be null.");
            where.getWorld().playSound(where, Sound.valueOf(Configuration.Config.SOUND_SPINNING.asString()), 1.0f, 1.0f);
            game.setWinner(slots[which]);
        }
        time--;
    }
}