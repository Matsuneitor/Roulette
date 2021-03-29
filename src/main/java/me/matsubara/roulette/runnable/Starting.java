package me.matsubara.roulette.runnable;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.file.Configuration;
import me.matsubara.roulette.file.Messages;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.gui.GUI;
import me.matsubara.roulette.trait.LookCloseModified;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.UUID;

public final class Starting extends BukkitRunnable {

    private final Roulette plugin;
    private final Game game;

    private int time;

    public Starting(Roulette plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
        this.time = Configuration.Config.COUNTDOWN_WAITING.asInt();
    }

    @Override
    public void run() {
        if (time == 0) {
            game.setSelecting();
            game.broadcast(plugin.getMessages().getRandomNPCMessage(game.getNPC(), "bets"));
            game.broadcast(Messages.Message.SELECT_BET.asString());

            // Open GUI to all the players in the game.
            for (UUID uuid : game.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) new GUI(plugin, player, game);
            }

            // Start selecting runnable.
            Selecting select = new Selecting(plugin, game);
            select.runTaskTimer(plugin, 0L, 20L);
            game.setSelectRunnable(select);

            // If the NPC is looking around, stop it.
            if (Configuration.Config.NPC_LOOK_AROUND.asBoolean()) {
                game.getNPC().getOrAddTrait(LookCloseModified.class).lookClose(false);
            }
            cancel();
            return;
        }
        // Announce countdown.
        if (time % 5 == 0 || Arrays.asList(3, 2, 1).contains(time)) {
            Location where = game.getSpinHologram().getLocation();

            Validate.notNull(where.getWorld(), "World can't be null.");
            where.getWorld().playSound(where, Sound.valueOf(Configuration.Config.SOUND_COUNTDOWN.asString()), 1.0f, 1.0f);

            game.broadcast(Messages.Message.STARTING.asString().replace("%seconds%", String.valueOf(time)));
        }
        time--;
    }
}