package me.matsubara.roulette.runnable;

import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.data.Slot;
import me.matsubara.roulette.file.Configuration;
import me.matsubara.roulette.file.Messages;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.util.RUtils;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.util.PlayerAnimation;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;

public final class Selecting extends BukkitRunnable {

    private final Roulette plugin;
    private final Game game;

    private int time;

    public Selecting(Roulette plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
        this.time = Configuration.Config.COUNTDOWN_SELECTING.asInt();
    }

    @Override
    public void run() {
        if (time == 0) {
            game.setSpinning();

            // Check if the players selected a chip.
            Iterator<UUID> iterator = game.getPlayers().iterator();
            while (iterator.hasNext()) {
                // If somehow player is null (maybe disconnected), continue.
                Player player = Bukkit.getPlayer(iterator.next());
                if (player == null) continue;

                // If the player didn't select a chip, close inventory and remove from the game.
                if (!game.getSelected().containsKey(player.getUniqueId())) {
                    game.removePlayer(player, true);
                    RUtils.handleMessage(player, Messages.Message.OUT_OF_TIME.asString());
                    player.getOpenInventory().close();
                    iterator.remove();
                    continue;
                }

                // Show the bet to the player.
                Slot selected = game.getSelected().get(player.getUniqueId()).getKey();
                String numbers = selected.isDoubleZero() ? "[00]" : Arrays.toString(selected.getInts());
                for (String line : plugin.getMessages().getYourBet(RUtils.getSlotName(selected), numbers, selected.getChance(game.getType().isEuropean()))) {
                    player.sendMessage(line);
                }
            }

            // If there aren't players left in the game, return.
            if (game.getPlayers().isEmpty()) return;

            game.broadcast(plugin.getMessages().getRandomNPCMessage(game.getNPC(), "no-bets"));

            // Hide holograms to the players so everyone can see the spinning hologram.
            for (UUID uuid : game.getHolograms().keySet()) {
                Player owner = Bukkit.getPlayer(uuid);
                if (owner == null) continue;

                game.getHolograms().get(uuid).getVisibilityManager().hideTo(owner);
            }

            // Start sorting runnable.
            Sorting sort = new Sorting(game);
            sort.runTaskTimer(plugin, 0L, 1L);
            game.setSortingRunnable(sort);

            // Play NPC spin animation.
            PlayerAnimation.ARM_SWING.play((Player) game.getNPC().getEntity());
            game.getNPC().getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.HAND, null);
            cancel();
            return;
        }
        // Announce countdown.
        if (time % 5 == 0 || Arrays.asList(3, 2, 1).contains(time)) {
            Location where = game.getSpinHologram().getLocation();

            Validate.notNull(where.getWorld(), "World can't be null.");
            where.getWorld().playSound(where, Sound.valueOf(Configuration.Config.SOUND_COUNTDOWN.asString()), 1.0f, 1.0f);

            game.broadcast(Messages.Message.SPINNING.asString().replace("%seconds%", String.valueOf(time)));
        }
        time--;
    }
}