package me.matsubara.roulette.runnable;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.data.Part;
import me.matsubara.roulette.data.Slot;
import me.matsubara.roulette.file.Configuration;
import me.matsubara.roulette.file.Messages;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.npc.NPC;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
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
        this.ball.getEquipment().setHelmet(Part.BALL.getXMaterial().parseItem());
        this.isEuropean = game.getType().isEuropean();

        this.slots = new Slot[isEuropean ? 37 : 38];

        this.time = Configuration.Config.COUNTDOWN_SORTING.asInt() * 20;

        game.broadcast(Messages.Message.SPINNING_START.asString());
        System.arraycopy(Slot.getValues(game), 0, slots, 0, isEuropean ? 37 : 38);
    }

    @Override
    public void run() {
        if (time == 0) {
            game.getSpinHologram().setLine(0, Configuration.Config.WINNING_NUMBER.asString());

            // Stop NPC animation, check if there're winners and stop.
            game.getNPC().setPose(NPC.Pose.STANDING);
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
        game.setWinner(slots[which]);

        // If the spin hologram is empty, create the lines, else update them.
        if (game.getSpinHologram().size() == 0) {
            game.getSpinHologram().addLine(Configuration.Config.SPINNING.asString(), false);
            //game.getSpinHologram().addLine(RUtils.getSlotName(slots[which]), false);
            game.getSpinHologram().addLine(String.format(Roulette.USE_HOLOGRAPHIC ? Game.HD_WINNER : Game.CMI_WINNER, game.getName()), false);
        } else {
            Validate.notNull(game.getLocation().getWorld(), "World can't be null.");
            //game.getSpinHologram().setLine(1, RUtils.getSlotName(slots[which]));

            Location where = game.getSpinHologram().getLocation();

            Validate.notNull(where.getWorld(), "World can't be null.");
            where.getWorld().playSound(where, Sound.valueOf(Configuration.Config.SOUND_SPINNING.asString()), 1.0f, 1.0f);
        }
        time--;
    }
}