package me.matsubara.roulette.runnable;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.data.Slot;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.util.RUtilities;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.util.PlayerAnimation;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.UUID;

public final class Selecting extends BukkitRunnable {

    private final Roulette plugin;
    private final Game game;

    private int time;

    public Selecting(Roulette plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
        this.time = plugin.getConfiguration().getCountdownSelecting();
    }

    @Override
    public void run() {
        if (time == 0) {
            game.setSpinning();

            // Check if the players selected a chip.
            for (UUID uuid : game.getPlayers()) {
                Player player = Bukkit.getPlayer(uuid);

                // If somehow player is null (maybe disconnected), continue.
                if (player == null) continue;

                // If the player didn't select a chip, close inventory and remove from the game.
                if (!game.getSelected().containsKey(player.getUniqueId())) {
                    game.removePlayer(player, false);
                    RUtilities.handleMessage(player, plugin.getMessages().getOutOfTime());
                    player.getOpenInventory().close();
                    continue;
                }

                // Show the bet to the player.
                Slot selected = game.getSelected().get(player.getUniqueId()).getKey();
                String numbers = selected.isDoubleZero() ? "[00]" : Arrays.toString(selected.getInts());
                for (String line : plugin.getMessages().getYourBet(RUtilities.getSlotName(selected), numbers, selected.getChance(game.getType().isEuropean()))) {
                    player.sendMessage(line);
                }
            }

            // If there aren't players left in the game, return.
            if (game.getPlayers().isEmpty()) {
                return;
            }

            // Hide holograms to the players so everyone can see the spinning hologram.
            for (UUID uuid : game.getHolograms().keySet()) {
                Player owner = Bukkit.getPlayer(uuid);
                if (owner != null) {
                    game.getHolograms().get(uuid).getVisibilityManager().hideTo(owner);
                }
            }

            // Start sorting runnable.
            Sorting sort = new Sorting(plugin, game);
            sort.runTaskTimer(plugin, 0L, 1L);
            game.setSortingRunnable(sort);

            // Play NPC spin animation.
            PlayerAnimation.ARM_SWING.play((Player) game.getNPC().getEntity());
            game.getNPC().getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.HAND, XMaterial.AIR.parseItem());
            cancel();
            return;
        }
        // Announce countdown.
        if (time % 5 == 0 || Arrays.asList(3, 2, 1).contains(time)) {
            Validate.notNull(game.getLocation().getWorld(), "World can't be null.");
            XSound.play(game.getSpinHologram().getLocation(), plugin.getConfiguration().getCountdownSound());
            game.broadcast(plugin.getMessages().getSorting(time));
        }
        time--;
    }
}