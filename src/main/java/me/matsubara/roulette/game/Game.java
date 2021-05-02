package me.matsubara.roulette.game;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.messages.ActionBar;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.google.common.base.Strings;
import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.data.Chip;
import me.matsubara.roulette.data.Part;
import me.matsubara.roulette.data.Slot;
import me.matsubara.roulette.file.Configuration;
import me.matsubara.roulette.file.Messages;
import me.matsubara.roulette.file.winner.Winner;
import me.matsubara.roulette.hologram.Hologram;
import me.matsubara.roulette.npc.NPC;
import me.matsubara.roulette.runnable.Selecting;
import me.matsubara.roulette.runnable.Sorting;
import me.matsubara.roulette.runnable.Starting;
import me.matsubara.roulette.util.RUtils;
import net.milkbowl.vault.economy.EconomyResponse;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class Game {

    private final Roulette plugin;

    // Static variables, needed for all games.
    public final static String HD_PLAYING = "{%s-playing}", HD_BET = "{%s-bet}", HD_WINNER = "{%s-winner}";
    public final static String CMI_PLAYING = "%%roulette_count_%s%%", CMI_BET = "%%roulette_selected%%", CMI_WINNER = "%%roulette_winner_%s%%";
    public final static Set<UUID> CREATING = new HashSet<>();
    public final static int[] TIMESTAMP = {1, 25, 50, 75, 100};

    // Game settings.
    private final String name;
    private int minPlayers, maxPlayers;
    private final Location location;
    private final GameData data;
    // Creation properties.
    private boolean isDone;

    private boolean betAll;
    private int startTime;

    // Required sets.
    private final Set<UUID> players;
    private final Set<String> placeholders;

    private final Set<Slot> disabled;

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
    private OfflinePlayer account;
    private GameState state;
    private Slot winner;

    private final BlockFace[] faces;
    private final Integer task;

    public Game(Roulette plugin, GameData data) {
        this.plugin = plugin;

        this.name = data.getName();
        setLimitPlayers(data.getMinPlayers(), data.getMaxPlayers());
        this.location = data.getLocation();

        this.data = data;

        this.isDone = false;

        this.betAll = data.isBetAll();
        setStartTime(data.getStartTime());

        this.players = new HashSet<>();
        this.placeholders = new HashSet<>();

        this.disabled = new HashSet<>();

        for (String slot : Configuration.Config.DISABLED_SLOTS.asList()) {
            try {
                this.disabled.add(Slot.valueOf(slot));
            } catch (IllegalArgumentException exception) {
                exception.printStackTrace();
            }
        }

        this.parts = new HashMap<>();
        this.slots = new HashMap<>();

        this.selected = new HashMap<>();
        this.holograms = new HashMap<>();
        this.chips = new HashMap<>();

        this.chairs = new ArrayList<>();

        this.npc = null;
        this.npcName = data.getNPCName();
        this.npcUUID = data.getNPCUUID();

        String joinName = Roulette.USE_HOLOGRAPHIC ? null : "join-hologram-" + name;
        Location join = location.clone().add(RUtils.offsetVector(new Vector(1.78125d, 3.5d, -0.59375d), location.getYaw(), location.getPitch()));
        this.joinHologram = RUtils.createHologram(this, joinName, join, true);

        // Register the placeholder for the join hologram (if using HD).
        String placeholder = String.format(HD_PLAYING, name);
        if (Roulette.USE_HOLOGRAPHIC) {
            HologramsAPI.registerPlaceholder(plugin, placeholder, 0.15d, () -> String.valueOf(players.size()));
        }
        placeholders.add(placeholder);

        // Register the placeholder for the spin hologram (if using HD).
        placeholder = String.format(HD_WINNER, name);
        if (Roulette.USE_HOLOGRAPHIC) {
            HologramsAPI.registerPlaceholder(plugin, placeholder, 0.15d, () -> {
                if (winner == null) return "";
                return RUtils.getSlotName(winner);
            });
        }
        placeholders.add(placeholder);

        String spinName = Roulette.USE_HOLOGRAPHIC ? null : "spin-hologram-" + name;
        Location spin = location.clone().add(RUtils.offsetVector(new Vector(-0.88d, 3.235d, -0.875d), location.getYaw(), location.getPitch()));
        this.spinHologram = RUtils.createHologram(this, spinName, spin);

        // If the type is changed wrong from games.yml, set AMERICAN by default.
        this.type = (data.getType() == null) ? GameType.AMERICAN : data.getType();

        OfflinePlayer player = (data.getAccount() == null) ? null : Bukkit.getOfflinePlayer(data.getAccount());
        this.account = (player != null && player.hasPlayedBefore()) ? player : null;

        this.state = GameState.WAITING;

        this.faces = RUtils.getCorrectFacing(RUtils.faceFromYaw(location.getYaw(), false));

        Player creator = Bukkit.getPlayer(data.getCreator());
        if (creator != null) CREATING.add(creator.getUniqueId());

        this.task = new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                createGamePart(index, creator);
                index++;
            }
        }.runTaskTimer(plugin, 0L, 0L).getTaskId();
    }

    @SuppressWarnings({"deprecation", "ConstantConditions"})
    private void createGamePart(int index, @Nullable Player creator) {
        if (index == 0) plugin.getGames().saveGame(this);

        int size = Part.getValues(this).length;
        // int size = Part.getSize(type.isEuropean());

        if (index == size - 1) {
            // Join hologram will appear when the whole table is ready.
            setupJoinHologram(String.format(Roulette.USE_HOLOGRAPHIC ? HD_PLAYING : CMI_PLAYING, name));

            // Send created message to the crator.
            if (creator != null && creator.isOnline()) {
                RUtils.handleMessage(creator, Messages.Message.CREATE.asString().replace("%name%", name));
            }

            // Load NPC after, since Citizens load NPC's later (or that's what I heard).
            loadNPC();

            if (creator != null) CREATING.remove(creator.getUniqueId());

            plugin.getGames().saveGame(this);
            plugin.getGames().createNextName();

            this.isDone = true;

            if (task == null) return;

            BukkitScheduler scheduler = plugin.getServer().getScheduler();
            if (scheduler.isQueued(task) || scheduler.isCurrentlyRunning(task)) {
                plugin.getServer().getScheduler().cancelTask(task);
            }
        }

        Part part = Part.getValues(this)[index];

        Vector offset = new Vector(part.getOffsetX(), part.getOffsetY(), part.getOffsetZ());

        if (part == Part.NPC_TARGET && !Roulette.USE_CITIZENS) offset = offset.setY(1.0d);

        Location newLocation = location.clone().add(RUtils.offsetVector(offset, location.getYaw(), location.getPitch()));

        // If the chunk of the location isn't loaded, load.
        if (!newLocation.getChunk().isLoaded()) newLocation.getChunk().load();

        // Set rotation of the chair, so the player looks to the front.
        if (isTopChair(part)) newLocation.setDirection(RUtils.getDirection(faces[chairs.size()]));

        // Rotate first spinner to make it look realistic.
        if (part.isSpinner() && part.isFirstSpinner()) newLocation.setYaw(newLocation.getYaw() + 90.0f);

        Validate.notNull(location.getWorld(), "World can't be null.");

        //ArmorStand stand = location.getWorld().spawn(newLocation, ArmorStand.class, this::modifyArmorStand);
        ArmorStand stand = location.getWorld().spawn(newLocation, ArmorStand.class, this::modifyArmorStand);

        if (stand.getEquipment() == null) return;

        boolean isChair = false, isSlot = false;

        if (part.isSpinner() || part.isNPCTarget()) {
            stand.setSmall(true);
            stand.setVisible(part.isSpinner());
            stand.setBasePlate(!part.isSpinner());
        } else if (part.isMaterial()) {
            stand.getEquipment().setHelmet(part.getXMaterial().parseItem());
            switch (part.getXMaterial()) {
                case END_ROD:
                    if (!part.isBall()) break;
                    stand.getEquipment().setHelmet(null);
                    stand.setHeadPose(new EulerAngle(Math.toRadians(300.0d), 0.0d, 0.0d));
                    break;
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
            stand.getEquipment().setItemInHand(part.isDecoration() ? RUtils.createHead(part.getUrl()) : null);
            stand.setSmall(true);
            stand.setRightArmPose(new EulerAngle(Math.toRadians(315.0d), Math.toRadians(45.0d), 0.0d));
            isSlot = part.isSlot();
        } else {
            stand.setHeadPose(new EulerAngle(Math.toRadians(0.0d), 0.0d, 0.0d));
            stand.getEquipment().setHelmet(RUtils.createHead(part.getUrl()));
        }

        PersistentDataContainer container = stand.getPersistentDataContainer();

        if (isChair) {
            container.set(new NamespacedKey(plugin, "fromRouletteChair"), PersistentDataType.INTEGER, chairs.size());
            chairs.add(stand);
        } else if (isSlot) {
            slots.put(Slot.getValues(this)[slots.size()], stand);
        } else {
            parts.put(part, stand);
        }

        if (!Configuration.Config.DEBUG.asBoolean()) return;

        int percent = (int) ((int) Math.round(((index + 1) * 100.0d / size) * 10.0d) / 10.0d);

        if (creator != null && creator.isOnline()) {
            String progress = getProgressBar(index + 1, size, 30, Configuration.Config.PROGRESS_CHARACTER.asChar(), ChatColor.GREEN, ChatColor.GRAY);
            ActionBar.sendActionBar(creator, Configuration.Config.PROGRESS.asString()
                    .replace("%percent%", String.valueOf(percent))
                    .replace("%progress-bar%", progress)
                    .replace("%game%", name)
                    .replace("%left%", String.valueOf(plugin.getGames().getGameDatas().size())));
        }

        if (!ArrayUtils.contains(TIMESTAMP, percent)) return;

        plugin.getLogger().info(String.format("The game \"%s\" is being created: %s%%", name, percent));

        if (percent == 100) {
            plugin.getLogger().info(String.format("The game \"%s\" has been created successfully.", name));
        }
    }

    private void modifyArmorStand(ArmorStand stand) {
        // Save our identity key to the armor stands when being spawned, to prevent clear lag deleting them.
        PersistentDataContainer container = stand.getPersistentDataContainer();
        container.set(new NamespacedKey(plugin, "fromRoulette"), PersistentDataType.STRING, name);

        stand.setAI(false);
        stand.setCustomName("roulette-armor-stand");
        stand.setCustomNameVisible(false);
        stand.setCollidable(false);
        stand.setInvulnerable(true);
        if (RUtils.getMajorVersion() != 12) stand.setPersistent(true);
        stand.setRemoveWhenFarAway(false);
        stand.setSilent(true);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setArms(false);
        stand.setBasePlate(false);
    }

    private void loadNPC() {
        // If UUID provided is null, then the NPC doesn't exist; if so, we create one, otherwise, we load it.
        npcName = (npcName == null) ? "" : RUtils.translate(npcName);

        boolean citizens = Roulette.USE_CITIZENS;

        if (citizens) {
            if (npcUUID != null) npc = RUtils.getNPCByUniqueId(npcUUID, this);
        }

        if (npc == null) npc = RUtils.createNPC(citizens, this, npcName);

        npcName = npc.getFullName();
        npcUUID = npc.getUniqueId();

        // Setup NPC location.
        Location where = location.clone().add(RUtils.offsetVector(new Vector(-1.275d, 1.0d, 0.55d), location.getYaw(), location.getPitch()));
        where.setDirection(RUtils.getDirection(faces[0]));

        // Spawn the NPC if isn't spawned.
        npc.spawn(where);

        // Set item in hand.
        npc.setItemInHand(plugin.getConfiguration().getBall());

        // Set NPC look around for players.
        if (Configuration.Config.NPC_LOOK_AROUND.asBoolean()) {
            npc.lookAround(npc, true);
        }

        // Hide NPC name if no name was supplied.
        if (npcName == null || npcName.equalsIgnoreCase("")) {
            npc.hideName();
        }
    }

    public String getProgressBar(int current, int max, int bars, char symbol, ChatColor completed, ChatColor notCompleted) {
        float percent = (float) current / max;
        int totalBars = (int) (bars * percent);

        return Strings.repeat("" + completed + symbol, totalBars) + Strings.repeat("" + notCompleted + symbol, bars - totalBars);
    }

    public void setupJoinHologram(String placeholder) {
        if (joinHologram.isDeleted()) return;
        if (joinHologram.size() > 0) joinHologram.clearLines();

        String type = this.type.isEuropean() ? Configuration.Config.TYPE_EUROPEAN.asString() : Configuration.Config.TYPE_AMERICAN.asString();

        List<String> lines = Configuration.Config.JOIN_HOLOGRAM.asList();
        for (String line : lines) {

            String text = line
                    .replaceAll("%name%", name)
                    .replaceAll("%playing%", placeholder)
                    .replaceAll("%max%", String.valueOf(maxPlayers))
                    .replaceAll("%type%", type);

            boolean touchable = lines.indexOf(line) == (lines.size() - 1);
            joinHologram.addLine(text, touchable);
        }
    }

    public boolean isTopChair(Part part) {
        return part.getXMaterial() != null && part.getXMaterial() == XMaterial.SPRUCE_SLAB && part.isChair();
    }

    public boolean spaceAvailable() {
        return (players.size() < maxPlayers);
    }

    public boolean canLoseMoney() {
        return state.isSelecting() || state.isSpinning();
    }

    public boolean inGame(Player player) {
        // Somehow gives StackOverFlowError with HashSet#contains().
        for (UUID uuid : players) {
            if (uuid.equals(player.getUniqueId())) return true;
        }
        return false;
    }

    public int size() {
        return players.size();
    }

    public void addPlayer(Player player) {
        if (!spaceAvailable()) return;
        if (inGame(player)) return;

        joinHologram.hideTo(player);
        players.add(player.getUniqueId());
        nextChair(player);

        if (players.size() == minPlayers) start();
    }

    public void removePlayer(Player player, boolean isRestart) {
        if (!inGame(player)) return;

        if (isRestart || players.size() == 1 || (state.isWaiting() || (state.isCountdown() && players.size() < minPlayers))) {
            joinHologram.showTo(player);
        }

        // Hide player selected chip.
        handleChipDisplay(player.getUniqueId(), false);
        selected.remove(player.getUniqueId());

        removePlayerHologram(player);

        if (player.isInsideVehicle()) player.leaveVehicle();

        // Players are removed with a iterator in @restart, and we don't need to send a message to every player about it if restarting.
        if (!isRestart) {
            players.remove(player.getUniqueId());
            broadcast(plugin.getMessages().getLeaveMessage(player.getName(), size(), maxPlayers));
        }

        if (players.isEmpty() && !state.isWaiting() && !state.isEnding()) restart();
        if (players.size() < minPlayers && state.isCountdown()) restart();
    }

    public void broadcast(String message) {
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) return;

            RUtils.handleMessage(player, RUtils.translate(message));
        }
    }

    public void broadcast(List<String> messages) {
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) return;

            for (String line : messages) {
                player.sendMessage(RUtils.translate(line));
            }
        }
    }

    private void removePlayerHologram(Player player) {
        if (!holograms.containsKey(player.getUniqueId())) return;

        // Unregister the placeholder for HD.
        String placeholder = String.format(HD_BET, player.getName());
        if (Roulette.USE_HOLOGRAPHIC) HologramsAPI.unregisterPlaceholder(plugin, placeholder);

        holograms.get(player.getUniqueId()).delete();
        holograms.remove(player.getUniqueId());
    }

    public void previousChair(Player player) {
        if (!isChairAvailable()) return;
        if (player.getVehicle() == null) return;

        PersistentDataContainer container = player.getVehicle().getPersistentDataContainer();

        NamespacedKey key = new NamespacedKey(plugin, "fromRouletteChair");
        if (!container.has(key, PersistentDataType.INTEGER)) return;

        Integer ordinal = container.get(key, PersistentDataType.INTEGER);
        if (ordinal == null) return;

        ArmorStand chair;
        do {
            ordinal--;
            if (ordinal < 0) ordinal = chairs.size() - 1;
            chair = chairs.get(ordinal);
        } while (hasPassengers(chair));

        Location location = player.getLocation().setDirection(chair.getLocation().getDirection());

        if (Configuration.Config.FIX_CHAIR_CAMERA.asBoolean()) player.teleport(location);

        Validate.notNull(location.getWorld(), "World can't be null.");
        location.getWorld().playSound(location, Sound.valueOf(Configuration.Config.SOUND_SWAP_CHAIR.asString()), 1.0f, 1.0f);
        addPassenger(chair, player);
    }

    public void nextChair(Player player) {
        if (!isChairAvailable()) return;

        // If player isn't in a chair, put him on the first empty chair.
        if (!player.isInsideVehicle()) {
            for (ArmorStand chair : chairs) {
                if (hasPassengers(chair)) continue;

                Location location = player.getLocation().clone().setDirection(chair.getLocation().getDirection());
                if (Configuration.Config.FIX_CHAIR_CAMERA.asBoolean()) player.teleport(location);

                Validate.notNull(location.getWorld(), "World can't be null.");
                location.getWorld().playSound(location, Sound.valueOf(Configuration.Config.SOUND_SWAP_CHAIR.asString()), 1.0f, 1.0f);
                addPassenger(chair, player);
                break;
            }
            return;
        }

        if (player.getVehicle() == null) return;

        PersistentDataContainer container = player.getVehicle().getPersistentDataContainer();

        NamespacedKey key = new NamespacedKey(plugin, "fromRouletteChair");
        if (!container.has(key, PersistentDataType.INTEGER)) return;

        Integer ordinal = container.get(key, PersistentDataType.INTEGER);
        if (ordinal == null) return;

        ArmorStand chair;
        do {
            ordinal++;
            if (ordinal > chairs.size() - 1) ordinal = 0;
            chair = chairs.get(ordinal);
        } while (hasPassengers(chair));

        Location location = player.getLocation().setDirection(chair.getLocation().getDirection());

        if (Configuration.Config.FIX_CHAIR_CAMERA.asBoolean()) player.teleport(location);

        Validate.notNull(location.getWorld(), "World can't be null.");
        location.getWorld().playSound(location, Sound.valueOf(Configuration.Config.SOUND_SWAP_CHAIR.asString()), 1.0f, 1.0f);
        addPassenger(chair, player);
    }

    @SuppressWarnings("deprecation")
    private boolean hasPassengers(Entity entity) {
        try {
            return !entity.getPassengers().isEmpty();
        } catch (NoSuchMethodError error) {
            return entity.getPassenger() != null;
        }
    }

    @SuppressWarnings({"deprecation", "UnusedReturnValue"})
    private boolean addPassenger(Entity vehicle, Entity passenger) {
        try {
            return vehicle.addPassenger(passenger);
        } catch (NoSuchMethodError error) {
            return vehicle.setPassenger(passenger);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isChairAvailable() {
        for (ArmorStand chair : chairs) {
            if (hasPassengers(chair)) continue;
            return true;
        }
        return false;
    }

    public void start() {
        if (!state.isWaiting()) return;
        start = new Starting(plugin, this);
        start.runTaskTimer(plugin, 0L, 20L);
        setCountdown();
    }

    public void restart() {
        if (state == GameState.WAITING && players.isEmpty()) return;
        if (npc != null && (state.isSpinning() || state.isEnding())) {
            npc.setItemInHand(plugin.getConfiguration().getBall());
        }

        if (parts.get(Part.BALL) != null) {
            //noinspection ConstantConditions, 100% sure won't happen.
            parts.get(Part.BALL).getEquipment().setHelmet(null);
            parts.get(Part.BALL).setHeadPose(new EulerAngle(Math.toRadians(300.0d), 0.0d, 0.0d));
        }

        Iterator<UUID> iterator = players.iterator();
        while (iterator.hasNext()) {
            Player player = Bukkit.getPlayer(iterator.next());
            if (player == null) continue;

            removePlayer(player, true);
            iterator.remove();
        }

        cancelRunnables(start, select, sort);

        if (spinHologram.size() > 0) spinHologram.clearLines();

        if (npc != null && Configuration.Config.NPC_LOOK_AROUND.asBoolean()) {
            npc.lookAround(npc, true);
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
            try {
                // BukkitRunnable#isCancelled() doesn't exist in every version.
                if (runnable != null && !runnable.isCancelled()) runnable.cancel();
            } catch (NoSuchMethodError error) {
                // If throws the IllegalState exception, is cancelled/not scheduled yet.
                try {
                    runnable.cancel();
                } catch (IllegalStateException ignore) {
                }
            }
        }
    }

    public void checkWinner() {
        Set<Player> winners = new HashSet<>();

        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) return;

            Slot slot = selected.get(uuid).getKey();

            // If player selected slot is single number and the winner slot too, add him to the winners set.
            if ((slot.isSingle() && winner.isSingle() && slot.getInts()[0] == winner.getInts()[0]) || (slot.isDoubleZero() && winner.isDoubleZero())) {
                winners.add(player);
                continue;
            }

            // Compare numbers.
            for (int select : slot.getInts()) {
                if (select == getWinner().getInts()[0]) winners.add(player);
            }
        }

        // Send money to the account of the game.
        if (account != null) {
            double total = 0;

            for (UUID uuid : players) {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);

                if (winners.contains(offline.getPlayer())) continue;
                if (account.isOnline() && account.equals(offline)) continue;

                Chip chip = chips.get(uuid);

                EconomyResponse response = plugin.getEconomy().depositPlayer(account, chip.getPrice());
                if (!response.transactionSuccess()) {
                    plugin.getLogger().info(String.format("It wasn't possible to deposit $%s to %s.", chip.getPrice(), account.getName()));
                    continue;
                }
                total += chip.getPrice();
            }

            if (account.isOnline()) {
                RUtils.handleMessage(account.getPlayer(), Messages.Message.RECEIVED.asString()
                        .replace("%money%", String.valueOf(total))
                        .replace("%name%", name));
            }
        }

        if (winners.isEmpty()) {
            broadcast(Messages.Message.NO_WINNER.asString().replace("%winner%", RUtils.getSlotName(winner)));
            broadcast(Messages.Message.RESTART.asString());
            restartRunnable(true);
            return;
        }

        String[] names = winners.stream().map(Player::getName).toArray(String[]::new);

        npc.playAnimation(NPC.Animation.HURT);
        broadcast(plugin.getMessages().getRandomNPCMessage(npc, "winner"));
        broadcast(plugin.getMessages().getWinners(names.length, Arrays.toString(names)));

        for (Player winner : winners) {
            Chip chip = chips.get(winner.getUniqueId());
            Slot slot = selected.get(winner.getUniqueId()).getKey();
            double price = chip.getPrice() * slot.getMultiplier();

            EconomyResponse response = plugin.getEconomy().depositPlayer(winner, price);
            if (!response.transactionSuccess()) {
                plugin.getLogger().info(String.format("It wasn't possible to deposit $%s to %s.", chip.getPrice(), account.getName()));
                continue;
            }

            RUtils.handleMessage(winner, plugin.getMessages().getPrice(plugin.getEconomy().format(price), slot.getMultiplier()));

            Winner win = plugin.getWinners().getByUniqueId(winner.getUniqueId());
            if (win == null) {
                win = new Winner(winner.getUniqueId());
            }
            // Null for mapId, we'll change it in the next update.
            win.add(null, price);

            plugin.getWinners().saveWinner(win);

            // if (!Configuration.Config.MAP_IMAGE.asBoolean()) continue; TODO: (NEXT UPDATE)
        }

        if (Configuration.Config.RESTART_FIREWORKS.asInt() == 0) {
            broadcast(Messages.Message.RESTART.asString());
            restartRunnable(false);
            return;
        }

        new BukkitRunnable() {
            int amount = 0;

            @Override
            public void run() {
                if (amount == Configuration.Config.RESTART_FIREWORKS.asInt()) {
                    restart();
                    cancel();
                }
                spawnFirework(joinHologram.getLocation());
                npc.playAnimation(NPC.Animation.HURT);
                amount++;
            }
        }.runTaskTimer(plugin, 0L, plugin.getConfiguration().getPeriod());

        broadcast(Messages.Message.RESTART.asString());
    }

    private void restartRunnable(boolean jump) {
        new BukkitRunnable() {
            int time = 0;

            @Override
            public void run() {
                if (time == Configuration.Config.RESTART_TIME.asInt()) {
                    restart();
                    cancel();
                }
                // If @npc-reaction is true, the NPC will jump until the game ends.
                if (Configuration.Config.NPC_REACTION.asBoolean()) {
                    if (jump) npc.jump();
                    else npc.playAnimation(NPC.Animation.HURT);
                }
                time++;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void spawnFirework(Location location) {
        Validate.notNull(location.getWorld(), "World can't be null.");
        ThreadLocalRandom random = ThreadLocalRandom.current();

        Firework firework = location.getWorld().spawn(location.clone().subtract(0.0d, 0.5d, 0.0d), Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        firework.setMetadata("isRoulette", new FixedMetadataValue(plugin, true));

        FireworkEffect.Builder builder = FireworkEffect.builder()
                .flicker(true)
                .trail(true)
                .withColor(RUtils.COLORS[random.nextInt(RUtils.COLORS.length)])
                .withFade(RUtils.COLORS[random.nextInt(RUtils.COLORS.length)]);

        FireworkEffect.Type[] types = FireworkEffect.Type.values();
        builder.with(types[random.nextInt(types.length)]);

        meta.addEffect(builder.build());
        meta.setPower(random.nextInt(1, 5));
        firework.setFireworkMeta(meta);

        if (Configuration.Config.INSTANT_EXPLODE.asBoolean()) {
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, firework::detonate, 1L);
        }
    }

    public void previousChip(UUID uuid) {
        if (!isSlotAvailable()) return;
        if (!chips.containsKey(uuid)) return;

        int ordinal = ArrayUtils.indexOf(Slot.getValues(this), selected.get(uuid).getKey());

        Slot slot;
        do {
            ordinal--;
            if (ordinal < 0) ordinal = Slot.getValues(this).length - 1;
            slot = Slot.getValues(this)[ordinal];
        } while (alreadySelected(slot));

        handleChipDisplay(uuid, false);
        selected.put(uuid, new AbstractMap.SimpleEntry<>(slot, slots.get(slot)));
        handleChipDisplay(uuid, true);
        showSelected(uuid);
    }

    public void nextChip(UUID uuid) {
        if (!isSlotAvailable()) return;
        if (!chips.containsKey(uuid)) return;

        if (!selected.containsKey(uuid)) {
            for (Slot slot : Slot.getValues(this)) {
                if (alreadySelected(slot)) continue;

                selected.put(uuid, new AbstractMap.SimpleEntry<>(slot, slots.get(slot)));
                handleChipDisplay(uuid, true);
                break;
            }
            showSelected(uuid);
            return;
        }

        int ordinal = ArrayUtils.indexOf(Slot.getValues(this), selected.get(uuid).getKey());

        Slot slot;
        do {
            ordinal++;
            if (ordinal > Slot.getValues(this).length - 1) ordinal = 0;
            slot = Slot.getValues(this)[ordinal];
        } while (alreadySelected(slot));

        handleChipDisplay(uuid, false);
        selected.put(uuid, new AbstractMap.SimpleEntry<>(slot, slots.get(slot)));
        handleChipDisplay(uuid, true);
        showSelected(uuid);
    }

    public boolean alreadySelected(Slot slot) {
        for (UUID uuid : selected.keySet()) {
            if (selected.get(uuid).getKey() == slot) return true;
        }
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isSlotAvailable() {
        for (Slot slot : Slot.getValues(this)) {
            if (alreadySelected(slot)) continue;
            return true;
        }
        return false;
    }

    public void handleChipDisplay(UUID uuid, boolean show) {
        if (!selected.containsKey(uuid)) return;

        if (show) {
            if (selected.get(uuid).getValue().getEquipment() == null) return;

            Chip chip = chips.get(uuid);
            if (chip == null) return;

            ArmorStand armorStand = selected.get(uuid).getValue();
            if (armorStand.getEquipment() == null) return;

            armorStand.getEquipment().setItemInMainHand(RUtils.createHead(chip.getUrl()));
            return;
        }

        ArmorStand armorStand = selected.get(uuid).getValue();

        if (armorStand.getEquipment() != null) {
            armorStand.getEquipment().setItemInMainHand(null);
        }
    }

    private void showSelected(UUID uuid) {
        boolean isHD = Roulette.USE_HOLOGRAPHIC;

        Vector offset = RUtils.offsetVector(new Vector(0.22d, 1.15d, 0.41d), location.getYaw(), location.getPitch());
        Location where = selected.get(uuid).getValue().getLocation().clone().add(offset);

        Validate.notNull(where.getWorld(), "World can't be null.");
        where.getWorld().playSound(where, Sound.valueOf(Configuration.Config.SOUND_SELECT.asString()), 1.0f, 1.0f);

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        if (holograms.containsKey(uuid)) {
            holograms.get(uuid).teleport(player, where);
            if (!isHD) {
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> hideToAllExcept(player, holograms.get(uuid)), 2L);
            }
            return;
        }

        String hologramName = Roulette.USE_HOLOGRAPHIC ? null : "selected-hologram-" + name + "-" + player.getName();
        Hologram hologram = RUtils.createHologram(this, hologramName, where);
        if (isHD) {
            hologram.setVisibleByDefault(false);
            hologram.showTo(player);
        }

        // Register the placeholder for the personal hologram (if using HD).
        String placeholder = isHD ? String.format(HD_BET, player.getName()) : CMI_BET;
        if (isHD) {
            HologramsAPI.registerPlaceholder(plugin, placeholder, 0.15d, () -> getPlayerBet(uuid));
        }
        placeholders.add(placeholder);

        for (String line : Configuration.Config.SELECT_HOLOGRAM.asList()) {
            hologram.addLine(line
                    .replaceAll("%player%", player.getName())
                    .replaceAll("%bet%", placeholder), false);
        }

        // If is CMI, we delay a task 2 ticks and hide the hologram to every player.
        if (!isHD) {
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> hideToAllExcept(player, hologram), 2L);
        }

        holograms.put(uuid, hologram);
    }

    private void hideToAllExcept(Player player, Hologram hologram) {
        for (Player online : player.getWorld().getPlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) continue;
            hologram.hideTo(online);
        }
    }

    public Slot getPlayerSlot(UUID uuid) {
        if (!selected.containsKey(uuid)) return null;
        return selected.get(uuid).getKey();
    }

    public String getPlayerBet(UUID uuid) {
        Slot slot = getPlayerSlot(uuid);
        return (slot != null) ? RUtils.getSlotName(slot) : null;
    }

    public void delete(boolean removeNPC, boolean isIterator, boolean isGlobalReload) {
        restart();

        if (Roulette.USE_HOLOGRAPHIC) {
            placeholders.forEach(placeholder -> HologramsAPI.unregisterPlaceholder(plugin, placeholder));
            placeholders.clear();
        }

        parts.forEach((part, stand) -> stand.remove());
        parts.clear();

        slots.values().forEach(Entity::remove);
        slots.clear();

        holograms.values().forEach(Hologram::delete);
        holograms.clear();

        chairs.forEach(Entity::remove);
        chairs.clear();

        deleteHolograms(joinHologram, spinHologram);

        if (npc != null) {
            npc.lookAround(npc, false);
            if (removeNPC) npc.destroy();
        }

        // If is global reload (/reload) we only want to delete the game from the server, not from games.yml.
        if (isGlobalReload) return;

        plugin.getGames().deleteGame(this, isIterator);
    }

    private void deleteHolograms(Hologram... holograms) {
        for (Hologram hologram : holograms) {
            if (hologram != null && !hologram.isDeleted()) hologram.delete();
        }
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

    /**
     * NOTE: You should only use it for get the UUID of the creator or the NPc, since the others settings can be different after a while.
     */
    public GameData getData() {
        return data;
    }

    public boolean isDone() {
        return isDone;
    }

    public boolean isBetAll() {
        return betAll;
    }

    public void setBetAll(boolean betAll) {
        this.betAll = betAll;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = (startTime < 5) ? 5 : (startTime > 60) ? 60 : (int) (5 * (Math.ceil(Math.abs(startTime / 5))));
    }

    public Integer getTask() {
        return task;
    }

    public Set<UUID> getPlayers() {
        return players;
    }

    public Set<Slot> getDisabled() {
        return disabled;
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

    public OfflinePlayer getAccount() {
        return account;
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
        // TODO: TESTING
        this.minPlayers = (minPlayers < 1) ? 1 : Math.min(minPlayers, 10);
        //this.minPlayers = (minPlayers < 1) ? ((minPlayers == maxPlayers) ? maxPlayers : minPlayers) : Math.min(minPlayers, 10);
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = (maxPlayers < minPlayers) ? ((minPlayers == 10) ? 10 : minPlayers) : Math.min(maxPlayers, 10);
    }

    public void setLimitPlayers(int minPlayers, int maxPlayers) {
        setMinPlayers(minPlayers);
        setMaxPlayers(maxPlayers);
    }

    public void setWaiting() {
        state = GameState.WAITING;
        joinHologram.setVisibleByDefault(true);
        joinHologram.resetVisibilityAll();
    }

    public void setCountdown() {
        state = GameState.COUNTDOWN;
    }

    public void setSelecting() {
        state = GameState.SELECTING;
        joinHologram.setVisibleByDefault(false);
        joinHologram.resetVisibilityAll();
    }

    public void setAccount(OfflinePlayer account) {
        this.account = account;
        plugin.getGames().saveGame(this);
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