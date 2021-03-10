package me.matsubara.roulette.runnable;

import com.cryptomorin.xseries.XSound;
import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.gui.GUI;
import me.matsubara.roulette.trait.LookCloseModified;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
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
        this.time = plugin.getConfiguration().getCountdownWaiting();
    }

    @Override
    public void run() {
        if (time == 0) {
            game.setSelecting();
            game.broadcast(plugin.getMessages().getSelectBet());

            // Open GUI to all the players in the game.
            for (UUID uuid : game.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) new GUI(plugin, player);
            }

            // Start selecting runnable.
            Selecting select = new Selecting(plugin, game);
            select.runTaskTimer(plugin, 0L, 20L);
            game.setSelectRunnable(select);

            // If the NPC is looking around, stop it.
            if (plugin.getConfiguration().npcLookAround()) {
                game.getNPC().getOrAddTrait(LookCloseModified.class).lookClose(false);
            }
            cancel();
            return;
        }
        // Announce countdown.
        if (time % 5 == 0 || Arrays.asList(3, 2, 1).contains(time)) {
            Validate.notNull(game.getLocation().getWorld(), "World can't be null.");
            XSound.play(game.getSpinHologram().getLocation(), plugin.getConfiguration().getCountdownSound());
            game.broadcast(plugin.getMessages().getStarting(time));
        }
        time--;
    }
}