package me.matsubara.roulette.game;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.messages.ActionBar;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import com.google.common.base.Strings;
import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.data.Chip;
import me.matsubara.roulette.data.Part;
import me.matsubara.roulette.data.Slot;
import me.matsubara.roulette.listener.holographic.TouchHandler;
import me.matsubara.roulette.runnable.Selecting;
import me.matsubara.roulette.runnable.Sorting;
import me.matsubara.roulette.runnable.Starting;
import me.matsubara.roulette.trait.LookCloseModified;
import me.matsubara.roulette.util.RUtilities;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.milkbowl.vault.economy.EconomyResponse;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class Game {

    private final Roulette plugin;

    // Game settings.
    private final String name;
    private int minPlayers, maxPlayers;
    private final Location location;
    private boolean isDone;

    // Required sets.
    private final Set<UUID> players;
    private final Set<String> placeholders;

    // Required maps, model and mechanic related.
    private final Map<Part, ArmorStand> parts;
    private final Map<Slot, ArmorStand> slots;

    // Required maps, data related.
    private final Map<UUID, Map.Entry<Slot, ArmorStand>> selected;
    private final Map<UUID, Hologram> holograms;
    private final Map<UUID, Chip> chips;

    // Required list, to keep positions.
    private final List<ArmorStand> chairs;

    // Game runnables.
    private Starting start;
    private Selecting select;
    private Sorting sort;

    // NPC data.
    private NPC npc;
    private String npcName;
    private UUID npcUUID;

    // Internal game data.
    private final Hologram joinHologram;
    private final Hologram spinHologram;
    private final GameType type;
    private GameState state;
    private Slot winner;

    private final Color[] colors;
    private final BlockFace[] faces;

    // Static variables, needed for all games.
    public final static String PLACEHOLDER = "{%s-playing}";
    public final static Set<UUID> CREATING = new HashSet<>();
    public final static int[] TIMESTAMP = {1, 25, 50, 75, 100};

    public Game(Roulette plugin, String name, Location location, @Nullable String npcName, @Nullable UUID npcUUID, int minPlayers, int maxPlayers, GameType type) {
        this.plugin = plugin;

        this.name = name;
        this.minPlayers = (minPlayers < 1) ? 1 : Math.min(minPlayers, 10);
        this.maxPlayers = maxPlayers < minPlayers ? (minPlayers == 10 ? 10 : minPlayers + 1) : Math.min(maxPlayers, 10);
        this.location = location;
        this.isDone = false;

        this.players = new HashSet<>();
        this.placeholders = new HashSet<>();

        this.parts = new HashMap<>();
        this.slots = new HashMap<>();

        this.selected = new HashMap<>();
        this.holograms = new HashMap<>();
        this.chips = new HashMap<>();

        this.chairs = new ArrayList<>();

        this.npcName = npcName;
        this.npcUUID = npcUUID;

        String placeholder = String.format(PLACEHOLDER, name);
        HologramsAPI.registerPlaceholder(plugin, placeholder, 1.0d, () -> String.valueOf(players.size()));
        placeholders.add(placeholder);

        Location l_join = location.clone().add(RUtilities.offsetVector(new Vector(1.17d, 3.5d, -0.585d), location.getYaw(), location.getPitch()));
        this.joinHologram = HologramsAPI.createHologram(plugin, l_join);
        this.joinHologram.setAllowPlaceholders(true);

        Location l_spin = location.clone().add(RUtilities.offsetVector(new Vector(-0.88d, 3.235d, -0.875d), location.getYaw(), location.getPitch()));
        this.spinHologram = HologramsAPI.createHologram(plugin, l_spin);
        this.spinHologram.setAllowPlaceholders(true);

        // If the type is changed wrong from games.yml, set AMERICAN by default.
        this.type = type == null ? GameType.AMERICAN : type;
        this.state = GameState.WAITING;

        this.colors = getColors();
        this.faces = RUtilities.getCorrectFacing(RUtilities.faceFromYaw(location.getYaw(), false));
    }

    public void createGame(@Nullable Player creator, boolean shouldSave) {
        if (creator != null) CREATING.add(creator.getUniqueId());
        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                createGamePart(this, index, creator, shouldSave);
                index++;
            }

        }.runTaskTimer(plugin, 0L, 6L);
    }

    private void createGamePart(BukkitRunnable runnable, int index, @Nullable Player creator, boolean shouldSave) {
        if (index == Part.getSize(type.isEuropean()) - 1) {
            // Join hologram will appear when the whole table is ready.
            setupJoinHologram(String.format(PLACEHOLDER, name));

            // Send created message to the crator.
            if (creator != null && creator.isOnline()) {
                RUtilities.handleMessage(creator, plugin.getMessages().getCreate(name));
            }

            // Load NPC after, since Citizens load NPC's later (or that's what I heard).
            loadNPC();

            if (creator != null) CREATING.remove(creator.getUniqueId());
            if (shouldSave) plugin.getGames().saveGame(this);
            this.isDone = true;
            runnable.cancel();
        }

        Part part = Part.getValues(type.isEuropean())[index];

        Vector offset = new Vector(part.getOffsetX(), part.getOffsetY(), part.getOffsetZ());
        Location newLocation = location.clone().add(RUtilities.offsetVector(offset, location.getYaw(), location.getPitch()));

        // Set rotation of the chair, so the player looks to the front.
        if (isTopChair(part)) {
            newLocation.setDirection(faces[chairs.size()].getDirection());
        }

        // Rotate first spinner to make it look realistic.
        if (part.isSpinner() && part.isFirstSpinner()) {
            newLocation.setYaw(newLocation.getYaw() + 90.0f);
        }

        Validate.notNull(location.getWorld(), "World can't be null.");

        ArmorStand stand = location.getWorld().spawn(newLocation, ArmorStand.class, armorStand -> {
            armorStand.setAI(false);
            armorStand.setCollidable(false);
            armorStand.setCanPickupItems(false);
            armorStand.setVisible(false);
            armorStand.setGravity(false);
            armorStand.setPersistent(false);
            armorStand.setRemoveWhenFarAway(false);
            armorStand.setSilent(true);
            armorStand.setInvulnerable(true);
        });

        if (stand.getEquipment() == null) {
            return;
        }

        boolean isChair = false, isSlot = false;

        if (part.isSpinner() || part.isNPCTarget()) {
            stand.setSmall(true);
            stand.setVisible(part.isSpinner());
            stand.setBasePlate(!part.isSpinner());
        } else if (part.getMaterial() != null) {
            stand.getEquipment().setHelmet(new ItemStack(part.getMaterial()));
            switch (part.getMaterial()) {
                case SPRUCE_SLAB:
                    if (part.isChair()) {
                        stand.setHeadPose(new EulerAngle(Math.toRadians(0.0d), 0.0d, 0.0d));
                        isChair = true;
                        break;
                    }
                    boolean west = part.name().endsWith("WEST"), east = part.name().endsWith("EAST"), north = part.name().endsWith("NORTH");
                    double angle = west ? 90.0d : north ? 180.0d : east ? 270.0d : 0.0d;

                    stand.setHeadPose(new EulerAngle(Math.toRadians(270.0d), angle != 0.0d ? Math.toRadians(angle) : 0.0d, 0.0d));
                    break;
                case SPRUCE_PLANKS:
                    stand.setHeadPose(new EulerAngle(Math.toRadians(0.0d), 0.0d, 0.0d));
                    stand.setSmall(true);
                    stand.setMarker(true);
                    stand.setFireTicks(Integer.MAX_VALUE);
                    break;
                case RED_CARPET:
                    stand.setHeadPose(new EulerAngle(Math.toRadians(0.0d), 0.0d, 0.0d));
                    break;
            }
        } else if (part.isSlot() || part.isDecoration()) {
            stand.getEquipment().setItemInMainHand(part.isDecoration() ? RUtilities.createHead(part.getUrl()) : null);
            stand.setSmall(true);
            stand.setRightArmPose(new EulerAngle(Math.toRadians(315.0d), Math.toRadians(45.0d), 0.0d));
            isSlot = part.isSlot();
        } else {
            stand.setHeadPose(new EulerAngle(Math.toRadians(0.0d), 0.0d, 0.0d));
            stand.getEquipment().setHelmet(RUtilities.createHead(part.getUrl()));
        }

        stand.getPersistentDataContainer().set(new NamespacedKey(plugin, "fromRoulette"), PersistentDataType.STRING, name);

        if (isChair) {
            NamespacedKey chairKey = new NamespacedKey(plugin, "fromRouletteChair");
            stand.getPersistentDataContainer().set(chairKey, PersistentDataType.INTEGER, chairs.size());
            chairs.add(stand);
        } else if (isSlot) {
            slots.put(Slot.getValues(type.isEuropean())[slots.size()], stand);
        } else {
            parts.put(part, stand);
        }

        if (!plugin.getConfiguration().enableDebug()) {
            return;
        }

        index++;

        int size = Part.getSize(type.isEuropean()), percent = (int) ((int) Math.round((index * 100.0d / size) * 10.0d) / 10.0d);

        if (creator != null && creator.isOnline()) {
            String progress = getProgressBar(index, size, 25, plugin.getConfiguration().getProgressChar(), ChatColor.GREEN, ChatColor.GRAY);
            ActionBar.sendActionBar(creator, plugin.getConfiguration().getProgress(percent, progress, name));
        }

        if (ArrayUtils.contains(TIMESTAMP, percent)) {
            plugin.getLogger().info("The game " + name + " is being created: " + percent + "%");
            if (percent == 100) {
                plugin.getLogger().info("The game " + name + " has been created successfully.");
            }
        }
    }

    private void loadNPC() {
        // If UUID provided is null, then the NPC doesn't exist; if so, we create one, otherwise, we load it.
        if (npcUUID == null) {
            this.npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, npcName == null ? "" : RUtilities.translate(npcName));
            this.npcUUID = npc.getUniqueId();
        } else {
            this.npc = CitizensAPI.getNPCRegistry().getByUniqueId(npcUUID);
            if (this.npc == null) {
                this.npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, npcName == null ? "" : RUtilities.translate(npcName));
            } else {
                this.npcName = this.npc.getFullName();
            }
        }

        // Setup NPC location.
        Location l_npc = location.clone().add(RUtilities.offsetVector(new Vector(-1.275d, 1.0d, 0.55d), location.getYaw(), location.getPitch()));
        l_npc.setDirection(faces[0].getDirection());

        // Spawn the NPC if isn't spawned.
        if (!this.npc.isSpawned()) {
            this.npc.spawn(l_npc);
        }

        // Set item in hand.
        this.npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.HAND, plugin.getConfiguration().getBall());

        // Set NPC look around for players.
        if (plugin.getConfiguration().npcLookAround()) {
            this.npc.getOrAddTrait(LookCloseModified.class).setGame(this);
            this.npc.getOrAddTrait(LookCloseModified.class).setRealisticLooking(true);
            this.npc.getOrAddTrait(LookCloseModified.class).setRange(plugin.getConfiguration().getLookDistance());
        }

        // Hide NPC name if no name was supplied.
        if (npcName == null || npcName.equalsIgnoreCase("")) {
            this.npc.data().setPersistent(NPC.NAMEPLATE_VISIBLE_METADATA, false);
        }

        // Start LookCloseModified after spawning every armor stand.
        if (plugin.getConfiguration().npcLookAround()) {
            Game.this.npc.getOrAddTrait(LookCloseModified.class).lookClose(true);
        }
    }

    public String getProgressBar(int current, int max, int bars, char symbol, ChatColor completed, ChatColor notCompleted) {
        float percent = (float) current / max;
        int progressBars = (int) (bars * percent);

        return Strings.repeat("" + completed + symbol, progressBars) + Strings.repeat("" + notCompleted + symbol, bars - progressBars);
    }

    public void setupJoinHologram(String placeholder) {
        if (joinHologram.size() > 0) {
            joinHologram.clearLines();
        }
        List<String> lines = plugin.getConfiguration().getJoinHologram();
        for (String line : lines) {
            TextLine text = joinHologram.appendTextLine(line
                    .replaceAll("%name%", name)
                    .replaceAll("%playing%", placeholder)
                    .replaceAll("%max%", String.valueOf(maxPlayers))
                    .replaceAll("%type%", type.isEuropean() ? plugin.getConfiguration().getEuropeanType() : plugin.getConfiguration().getAmericanType()));
            if (lines.indexOf(line) == (lines.size() - 1)) {
                text.setTouchHandler(new TouchHandler(plugin, this));
            }
        }
    }

    public Color[] getColors() {
        Field[] fields = Color.class.getDeclaredFields();

        List<Color> results = new ArrayList<>();
        for (Field field : fields) {
            if (field.getType().equals(Color.class)) {
                try {
                    results.add((Color) field.get(null));
                } catch (IllegalAccessException exception) {
                    exception.printStackTrace();
                }
            }
        }
        return results.toArray(new Color[0]);
    }

    public boolean isTopChair(Part part) {
        return part.isMaterial() && part.getMaterial() == XMaterial.SPRUCE_SLAB.parseMaterial() && part.isChair();
    }

    public boolean spaceAvailable() {
        return (players.size() < maxPlayers);
    }

    public boolean canLoseMoney() {
        return state.isSelecting() || state.isSpinning();
    }

    public boolean inGame(Player player) {
        return players.contains(player.getUniqueId());
    }

    public int size() {
        return players.size();
    }

    public void addPlayer(Player player) {
        if (!spaceAvailable()) {
            return;
        }

        if (inGame(player)) {
            return;
        }

        joinHologram.getVisibilityManager().hideTo(player);
        players.add(player.getUniqueId());
        nextChair(player);

        if (players.size() == minPlayers) {
            start();
        }
    }

    public void removePlayer(Player player, boolean isRestart) {
        if (!inGame(player)) {
            return;
        }

        if (isRestart || players.size() == 1 || (state.isWaiting() || (state.isCountdown() && players.size() < minPlayers))) {
            joinHologram.getVisibilityManager().showTo(player);
        }

        // Hide player selected chip.
        handleChipDisplay(player.getUniqueId(), false);
        selected.remove(player.getUniqueId());

        removePlayerHologram(player);

        if (player.isInsideVehicle()) {
            player.leaveVehicle();
        }

        // Players are removed with a iterator in @restart, and we don't need to send a message to every player about it if restarting.
        if (!isRestart) {
            players.remove(player.getUniqueId());
            broadcast(plugin.getMessages().getLeaveMessage(player.getName(), size(), maxPlayers));
        }

        if (players.isEmpty() && !state.isWaiting() && !state.isEnding()) {
            restart();
        }
    }

    public void broadcast(String message) {
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                return;
            }
            RUtilities.handleMessage(player, RUtilities.translate(message));
        }
    }

    public void broadcast(List<String> messages) {
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                return;
            }
            for (String line : messages) {
                player.sendMessage(RUtilities.translate(line));
            }
        }
    }

    private void removePlayerHologram(Player player) {
        if (!holograms.containsKey(player.getUniqueId())) {
            return;
        }
        String placeholder = String.format("{%s-bet}", player.getName());
        HologramsAPI.unregisterPlaceholder(plugin, placeholder);
        holograms.get(player.getUniqueId()).delete();
        holograms.remove(player.getUniqueId());
    }

    public void previousChair(Player player) {
        if (!isChairAvailable()) {
            return;
        }

        if (player.getVehicle() == null) {
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, "fromRouletteChair");
        if (!player.getVehicle().getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) {
            return;
        }

        Integer ordinal = player.getVehicle().getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        if (ordinal == null) {
            return;
        }

        ArmorStand chair;
        do {
            ordinal--;
            if (ordinal < 0) {
                ordinal = chairs.size() - 1;
            }
            chair = chairs.get(ordinal);
        } while (!chair.getPassengers().isEmpty());

        Location location = player.getLocation().setDirection(chair.getLocation().getDirection());
        if (plugin.getConfiguration().fixChairCamera()) {
            player.teleport(location);
        }

        XSound.play(location, plugin.getConfiguration().getSwapSound());
        chair.addPassenger(player);
    }

    public void nextChair(Player player) {
        if (!isChairAvailable()) {
            return;
        }

        // If player isn't in a chair, put him on the first empty chair.
        if (!player.isInsideVehicle()) {
            for (ArmorStand chair : chairs) {
                if (!chair.getPassengers().isEmpty()) {
                    continue;
                }

                Location location = player.getLocation().clone().setDirection(chair.getLocation().getDirection());
                if (plugin.getConfiguration().fixChairCamera()) {
                    player.teleport(location);
                }

                XSound.play(location, plugin.getConfiguration().getSwapSound());
                chair.addPassenger(player);
                break;
            }
            return;
        }

        if (player.getVehicle() == null) {
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, "fromRouletteChair");
        if (!player.getVehicle().getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) {
            return;
        }

        Integer ordinal = player.getVehicle().getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        if (ordinal == null) {
            return;
        }

        ArmorStand chair;
        do {
            ordinal++;
            if (ordinal > chairs.size() - 1) {
                ordinal = 0;
            }
            chair = chairs.get(ordinal);
        } while (!chair.getPassengers().isEmpty());

        Location location = player.getLocation().setDirection(chair.getLocation().getDirection());
        if (plugin.getConfiguration().fixChairCamera()) {
            player.teleport(location);
        }

        XSound.play(location, plugin.getConfiguration().getSwapSound());
        chair.addPassenger(player);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isChairAvailable() {
        for (ArmorStand chair : chairs) {
            if (!chair.getPassengers().isEmpty()) {
                continue;
            }
            return true;
        }
        return false;
    }

    public void start() {
        if (!state.isWaiting()) {
            return;
        }

        start = new Starting(plugin, this);
        start.runTaskTimer(plugin, 0L, 20L);
        setCountdown();
    }

    public void restart() {
        if (state.isSpinning() || state.isEnding()) {
            npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.HAND, plugin.getConfiguration().getBall());
        }

        Iterator<UUID> iterator = players.iterator();
        while (iterator.hasNext()) {
            Player player = Bukkit.getPlayer(iterator.next());
            if (player == null) {
                continue;
            }
            removePlayer(player, true);
            iterator.remove();
        }

        cancelRunnables(start, select, sort);

        if (spinHologram.size() > 0) {
            spinHologram.clearLines();
        }

        if (plugin.getConfiguration().npcLookAround()) {
            this.npc.getOrAddTrait(LookCloseModified.class).lookClose(true);
        }

        // Set the game state to waiting after 1 second, to prevent unwanted messages.
        new BukkitRunnable() {

            @Override
            public void run() {
                setWaiting();
            }
        }.runTaskLater(plugin, 20L);
    }

    public void cancelRunnables(BukkitRunnable... runnables) {
        for (BukkitRunnable runnable : runnables) {
            if (runnable != null && !runnable.isCancelled()) {
                runnable.cancel();
            }
        }
    }

    public void checkWinner() {
        Set<Player> winners = new HashSet<>();

        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);

            if (player == null) {
                return;
            }

            Slot slot = selected.get(uuid).getKey();

            // If player selected slot is single number and the winner slot too, add him to the winners set.
            if ((slot.isSingle() && winner.isSingle() && slot.getInts()[0] == winner.getInts()[0]) || (slot.isDoubleZero() && winner.isDoubleZero())) {
                winners.add(player);
                continue;
            }

            // Compare numbers.
            for (int select : slot.getInts()) {
                if (select == getWinner().getInts()[0]) {
                    winners.add(player);
                }
            }
        }

        if (winners.isEmpty()) {
            broadcast(plugin.getMessages().getNoWinner());
            broadcast(plugin.getMessages().getRestart());
            plugin.getServer().getScheduler().runTaskLater(plugin, this::restart, plugin.getConfiguration().getRestartTime() * 20L);
            return;
        }

        String[] names = winners.stream().map(Player::getName).toArray(String[]::new);

        broadcast(plugin.getMessages().getWinners(names.length, Arrays.toString(names)));

        for (Player winner : winners) {
            Chip chip = chips.get(winner.getUniqueId());
            Slot slot = selected.get(winner.getUniqueId()).getKey();
            double price = chip.getPrice() * slot.getMultiplier();

            EconomyResponse response = plugin.getEconomy().depositPlayer(winner, price);
            if (response.transactionSuccess()) {
                RUtilities.handleMessage(winner, plugin.getMessages().getPrice(plugin.getEconomy().format(price), slot.getMultiplier()));
            }
        }

        if (plugin.getConfiguration().getFireworks() == 0) {
            broadcast(plugin.getMessages().getRestart());
            plugin.getServer().getScheduler().runTaskLater(plugin, this::restart, plugin.getConfiguration().getRestartTime() * 20L);
            return;
        }

        new BukkitRunnable() {
            int amount = 0;

            @Override
            public void run() {
                if (amount == plugin.getConfiguration().getFireworks()) {
                    restart();
                    cancel();
                }
                spawnFirework(joinHologram.getLocation());
                amount++;
            }
        }.runTaskTimer(plugin, 0L, plugin.getConfiguration().getPeriod());
        broadcast(plugin.getMessages().getRestart());
    }

    /**
     * Spawn random fireworks at certain location.
     *
     * @param location where to spawnn the fireworks.
     */
    private void spawnFirework(Location location) {
        Validate.notNull(location.getWorld(), "World can't be null.");
        ThreadLocalRandom random = ThreadLocalRandom.current();

        Firework firework = location.getWorld().spawn(location.clone().subtract(0.0d, 0.5d, 0.0d), Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        FireworkEffect.Builder builder = FireworkEffect.builder()
                .flicker(true)
                .trail(true)
                .withColor(colors[random.nextInt(colors.length)])
                .withFade(colors[random.nextInt(colors.length)]);

        FireworkEffect.Type[] types = FireworkEffect.Type.values();
        builder.with(types[random.nextInt(types.length)]);

        meta.addEffect(builder.build());
        meta.setPower(random.nextInt(1, 5));
        firework.setFireworkMeta(meta);

        if (plugin.getConfiguration().instantExplode()) {
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, firework::detonate, 1L);
        }
    }

    public void previousChip(UUID uuid) {
        if (!isSlotAvailable()) {
            return;
        }

        if (!chips.containsKey(uuid)) {
            return;
        }

        int ordinal = ArrayUtils.indexOf(Slot.getValues(type.isEuropean()), selected.get(uuid).getKey());

        Slot slot;
        do {
            ordinal--;
            if (ordinal < 0) {
                ordinal = Slot.getValues(type.isEuropean()).length - 1;
            }
            slot = Slot.getValues(type.isEuropean())[ordinal];
        } while (alreadySelected(slot));

        handleChipDisplay(uuid, false);
        selected.put(uuid, new AbstractMap.SimpleEntry<>(slot, slots.get(slot)));
        handleChipDisplay(uuid, true);
        showSelected(uuid);
    }

    public void nextChip(UUID uuid) {
        if (!isSlotAvailable()) {
            return;
        }

        if (!chips.containsKey(uuid)) {
            return;
        }

        if (!selected.containsKey(uuid)) {
            for (Slot slot : Slot.getValues(type.isEuropean())) {
                if (alreadySelected(slot)) {
                    continue;
                }
                selected.put(uuid, new AbstractMap.SimpleEntry<>(slot, slots.get(slot)));
                handleChipDisplay(uuid, true);
                break;
            }
            showSelected(uuid);
            return;
        }

        int ordinal = ArrayUtils.indexOf(Slot.getValues(type.isEuropean()), selected.get(uuid).getKey());

        Slot slot;
        do {
            ordinal++;
            if (ordinal > Slot.getValues(type.isEuropean()).length - 1) {
                ordinal = 0;
            }
            slot = Slot.getValues(type.isEuropean())[ordinal];
        } while (alreadySelected(slot));

        handleChipDisplay(uuid, false);
        selected.put(uuid, new AbstractMap.SimpleEntry<>(slot, slots.get(slot)));
        handleChipDisplay(uuid, true);
        showSelected(uuid);
    }

    public boolean alreadySelected(Slot slot) {
        for (UUID uuid : selected.keySet()) {
            if (selected.get(uuid).getKey() == slot) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isSlotAvailable() {
        for (Slot slot : Slot.getValues(type.isEuropean())) {
            if (alreadySelected(slot)) {
                continue;
            }
            return true;
        }
        return false;
    }

    public void handleChipDisplay(UUID uuid, boolean show) {
        if (!selected.containsKey(uuid)) {
            return;
        }

        if (show) {
            if (selected.get(uuid).getValue().getEquipment() != null) {
                Chip chip = chips.get(uuid);
                if (chip == null) {
                    return;
                }
                ArmorStand armorStand = selected.get(uuid).getValue();
                if (armorStand.getEquipment() == null) {
                    return;
                }
                armorStand.getEquipment().setItemInMainHand(RUtilities.createHead(chip.getUrl()));
            }
            return;
        }

        ArmorStand armorStand = selected.get(uuid).getValue();

        if (armorStand.getEquipment() != null) {
            armorStand.getEquipment().setItemInMainHand(null);
        }
    }

    private void showSelected(UUID uuid) {
        Vector offset = new Vector(0.22d, 1.15d, 0.41d);
        Location newLocation = selected.get(uuid).getValue().getLocation().clone().add(RUtilities.offsetVector(offset, location.getYaw(), location.getPitch()));

        Validate.notNull(newLocation.getWorld(), "World can't be null.");
        XSound.play(newLocation, plugin.getConfiguration().getSelectSound());

        if (holograms.containsKey(uuid)) {
            holograms.get(uuid).teleport(newLocation);
            return;
        }

        Player player = Bukkit.getPlayer(uuid);

        if (player == null) {
            return;
        }

        String placeholder = String.format("{%s-bet}", player.getName());
        placeholders.add(placeholder);
        HologramsAPI.registerPlaceholder(plugin, placeholder, 0.25d, () -> getPlayerBet(uuid));
        Hologram hologram = HologramsAPI.createHologram(plugin, newLocation);

        hologram.getVisibilityManager().setVisibleByDefault(false);
        hologram.getVisibilityManager().showTo(player);
        hologram.setAllowPlaceholders(true);

        for (String line : plugin.getConfiguration().getSelectHologram()) {
            hologram.appendTextLine(line
                    .replaceAll("%player%", player.getName())
                    .replaceAll("%bet%", String.format("{%s-bet}", player.getName()))
            );
        }
        holograms.put(uuid, hologram);
    }

    public String getPlayerBet(UUID uuid) {
        if (!selected.containsKey(uuid)) {
            return null;
        }

        Slot slot = selected.get(uuid).getKey();
        return RUtilities.getSlotName(slot);
    }

    public void delete(boolean removeNPC) {
        restart();

        placeholders.forEach(placeholder -> HologramsAPI.unregisterPlaceholder(plugin, placeholder));
        placeholders.clear();

        parts.forEach((part, stand) -> stand.remove());
        parts.clear();

        slots.values().forEach(Entity::remove);
        slots.clear();

        holograms.values().forEach(Hologram::delete);
        holograms.clear();

        chairs.forEach(Entity::remove);
        chairs.clear();

        joinHologram.delete();
        spinHologram.delete();

        npc.getOrAddTrait(LookCloseModified.class).lookClose(false);
        if (removeNPC) npc.destroy();
    }

    public Roulette getPlugin() {
        return plugin;
    }

    public String getName() {
        return name;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isDone() {
        return isDone;
    }

    public Set<UUID> getPlayers() {
        return players;
    }

    public Map<Part, ArmorStand> getParts() {
        return parts;
    }

    public Map<UUID, Map.Entry<Slot, ArmorStand>> getSelected() {
        return selected;
    }

    public Map<UUID, Hologram> getHolograms() {
        return holograms;
    }

    public Map<UUID, Chip> getChips() {
        return chips;
    }

    public NPC getNPC() {
        return npc;
    }

    public UUID getNPCUUID() {
        return npcUUID;
    }

    public Hologram getSpinHologram() {
        return spinHologram;
    }

    public GameType getType() {
        return type;
    }

    public GameState getState() {
        return state;
    }

    public Slot getWinner() {
        return winner;
    }

    public void setSelectRunnable(Selecting select) {
        this.select = select;
    }

    public void setSortingRunnable(Sorting sort) {
        this.sort = sort;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public void setWaiting() {
        state = GameState.WAITING;
    }

    public void setCountdown() {
        state = GameState.COUNTDOWN;
    }

    public void setSelecting() {
        state = GameState.SELECTING;
        // Hide the join hologram to every player.
        joinHologram.getVisibilityManager().setVisibleByDefault(false);
    }

    public void setSpinning() {
        state = GameState.SPINNING;
    }

    public void setEnding() {
        state = GameState.ENDING;
    }

    public void setWinner(Slot winner) {
        this.winner = winner;
    }
}