package me.matsubara.roulette.util;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.cryptomorin.xseries.ReflectionUtils;
import com.cryptomorin.xseries.SkullUtils;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.messages.ActionBar;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.data.Part;
import me.matsubara.roulette.data.Slot;
import me.matsubara.roulette.file.Configuration;
import me.matsubara.roulette.file.Messages;
import me.matsubara.roulette.game.Game;
import me.matsubara.roulette.gui.GUIHolder;
import me.matsubara.roulette.gui.GUIType;
import me.matsubara.roulette.hologram.Hologram;
import me.matsubara.roulette.hologram.Hologram_CMI;
import me.matsubara.roulette.hologram.Hologram_Holographic;
import me.matsubara.roulette.npc.NPC;
import me.matsubara.roulette.npc.NPC_Citizens;
import me.matsubara.roulette.npc.NPC_NPCLib;
import net.citizensnpcs.api.CitizensAPI;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: This class is a mess, need a rewrite.
public final class RUtils {

    private final static Roulette PLUGIN = JavaPlugin.getPlugin(Roulette.class);
    private final static Pattern PATTERN = Pattern.compile("(&)?&#([0-9a-fA-F]{6})");

    public final static Color[] COLORS = getColors();

    private static Color[] getColors() {
        Field[] fields = Color.class.getDeclaredFields();

        List<Color> results = new ArrayList<>();
        for (Field field : fields) {
            if (!field.getType().equals(Color.class)) continue;
            try {
                results.add((Color) field.get(null));
            } catch (IllegalAccessException exception) {
                exception.printStackTrace();
            }
        }
        return results.toArray(new Color[0]);
    }

    private static final BlockFace[] AXIS = {
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST};

    private static final BlockFace[] RADIAL = {
            BlockFace.NORTH,
            BlockFace.NORTH_EAST,
            BlockFace.EAST,
            BlockFace.SOUTH_EAST,
            BlockFace.SOUTH,
            BlockFace.SOUTH_WEST,
            BlockFace.WEST,
            BlockFace.NORTH_WEST};

    public static Map<Slot, Part> SLOT_PART = createParallel();

    private static Object SNEAKING;
    private static Object STANDING;

    static {
        Class<?> ENTITY_POSE = ReflectionUtils.getNMSClass("EntityPose");

        try {
            @SuppressWarnings("ConstantConditions") Method valueOf = ENTITY_POSE.getMethod("valueOf", String.class);

            SNEAKING = valueOf.invoke(null, isVillageAndPillage() ? "SNEAKING" : "CROUCHING");
            STANDING = valueOf.invoke(null, "STANDING");
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    public static void setSneaking(net.citizensnpcs.api.npc.NPC npc, boolean sneaking) {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();

        PacketContainer packet = manager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, npc.getEntity().getEntityId());

        WrappedDataWatcher watcher = WrappedDataWatcher.getEntityWatcher(npc.getEntity()).deepClone();
        watcher.setObject(6, sneaking ? SNEAKING : STANDING);

        packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                manager.sendServerPacket(player, packet);
            } catch (InvocationTargetException exception) {
                exception.printStackTrace();
            }
        }
    }

    public static void openMainGUI(Player player, Game game) {
        String title = Configuration.Config.GAME_MENU_TITLE.asString().replace("%name%", game.getName());
        Inventory inventory = Bukkit.createInventory(new GUIHolder(null, GUIType.MAIN, game), 27, title);

        ItemStack background = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayName("&7").build();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, background);
        }

        ItemStack account = new ItemBuilder(XMaterial.PLAYER_HEAD.parseMaterial()).setLore(PLUGIN.getConfiguration().getAccountLore()).build();
        ItemStack noAccount = PLUGIN.getConfiguration().getItem("game-menu", "no-account", null);

        inventory.setItem(10, (game.getAccount() != null) ? new ItemBuilder(account)
                .setOwningPlayer(game.getAccount())
                .setDisplayName(PLUGIN.getConfiguration().getAccountDisplayName(game.getAccount().getName()))
                .build() : noAccount);

        int minAmount = game.getMinPlayers(), maxAmount = game.getMaxPlayers();

        ItemStack min = PLUGIN.getConfiguration().getItem("game-menu", "min-amount", null);
        inventory.setItem(11, new ItemBuilder(min).setAmount(minAmount).build());

        ItemStack max = PLUGIN.getConfiguration().getItem("game-menu", "max-amount", null);
        inventory.setItem(12, new ItemBuilder(max).setAmount(maxAmount).build());

        int timeSeconds = game.getStartTime();
        ItemStack time = PLUGIN.getConfiguration().getItem("game-menu", "start-time", null);
        inventory.setItem(13, new ItemBuilder(time)
                .setDisplayName(PLUGIN.getConfiguration().getStartTimeDisplayName(timeSeconds))
                .setAmount(timeSeconds).build());

        ItemStack laPartage = PLUGIN.getConfiguration().getItem("game-menu", "la-partage", null);
        inventory.setItem(14, laPartage);

        ItemStack enPrison = PLUGIN.getConfiguration().getItem("game-menu", "en-prison", null);
        inventory.setItem(15, enPrison);

        ItemStack surrender = PLUGIN.getConfiguration().getItem("game-menu", "surrender", null);
        inventory.setItem(16, surrender);

        ItemStack betAll = PLUGIN.getConfiguration().getItem("game-menu", "bet-all", null);

        String state = game.isBetAll() ? Configuration.Config.STATE_ENABLED.asString() : Configuration.Config.STATE_DISABLED.asString();

        ItemBuilder builder = new ItemBuilder(betAll)
                .setDisplayName(PLUGIN.getConfiguration().getDisplayName("game-menu", "bet-all").replace("%state%", state));

        // If bet-all is allowed in this game, add glow effect.
        if (game.isBetAll()) {
            builder.addEnchantment(Enchantment.ARROW_DAMAGE, 1, true)
                    .addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        inventory.setItem(18, builder.build());

        inventory.setItem(26, PLUGIN.getConfiguration().getItem("game-menu", "close", null));

        player.openInventory(inventory);
    }

    private static Map<Slot, Part> createParallel() {
        SLOT_PART = new HashMap<>();

        for (int i = 1; i < 37; i++) {
            SLOT_PART.put(Slot.valueOf("SINGLE_" + i), Part.valueOf("SLOT_" + i));
        }

        SLOT_PART.put(Slot.COLUMN_1, Part.SLOT_COLUMN_1);
        SLOT_PART.put(Slot.COLUMN_2, Part.SLOT_COLUMN_2);
        SLOT_PART.put(Slot.COLUMN_3, Part.SLOT_COLUMN_3);

        SLOT_PART.put(Slot.DOZEN_1, Part.SLOT_DOZEN_1);
        SLOT_PART.put(Slot.DOZEN_2, Part.SLOT_DOZEN_2);
        SLOT_PART.put(Slot.DOZEN_3, Part.SLOT_DOZEN_3);

        SLOT_PART.put(Slot.LOW, Part.SLOT_1_TO_18);
        SLOT_PART.put(Slot.EVEN, Part.SLOT_EVEN);

        SLOT_PART.put(Slot.RED, Part.SLOT_RED);
        SLOT_PART.put(Slot.BLACK, Part.SLOT_BLACK);

        SLOT_PART.put(Slot.ODD, Part.SLOT_ODD);
        SLOT_PART.put(Slot.HIGH, Part.SLOT_19_TO_36);

        return SLOT_PART;
    }

    public static NPC createNPC(boolean citizens, Game game, String name) {
        if (citizens) return new NPC_Citizens(null, game, name);
        return new NPC_NPCLib(game, name);
    }

    /**
     * Get already spawned NPC of citizens by UUID.
     */
    public static NPC getNPCByUniqueId(UUID uuid, Game game) {
        if (CitizensAPI.getNPCRegistry().getByUniqueId(uuid) == null) return null;
        return new NPC_Citizens(uuid, game, null);
    }

    public static Hologram createHologram(Game game, @Nullable String name, Location location) {
        return createHologram(game, name, location, false);
    }

    public static Hologram createHologram(Game game, @Nullable String name, Location location, boolean isJoin) {
        // If the given name is null, then is a HD hologram, since HD doesn't require a name.
        if (name == null) return new Hologram_Holographic(game, location);
        return new Hologram_CMI(game, name, location, isJoin);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean canJoin(Player player, Game game) {
        // Check if player is vanished using CMI.
        if (PLUGIN.hasDependency("CMI")) {
            CMIUser user = CMI.getInstance().getPlayerManager().getUser(player.getName());
            if (user != null && user.isVanished()) {
                handleMessage(player, Messages.Message.VANISH.asString());
                return false;
            }
        }

        // Check if player is vanished using EssentialsX.
        if (PLUGIN.hasDependency("Essentials")) {
            Essentials essentials = (Essentials) PLUGIN.getServer().getPluginManager().getPlugin("Essentials");
            if (essentials != null) {
                User user = essentials.getUser(player);
                if (user != null && user.isVanished()) {
                    handleMessage(player, Messages.Message.VANISH.asString());
                    return false;
                }
            }
        }

        // Check if player is vanished using SuperVanish, PremiumVanish, VanishNoPacket, etcetera.
        if (isPluginVanished(player)) {
            handleMessage(player, Messages.Message.VANISH.asString());
            return false;
        }

        // If the player is already in game, return. (Shouldn't happen)
        if (game.inGame(player)) {
            RUtils.handleMessage(player, Messages.Message.ALREADY_INGAME.asString());
            return false;
        }

        // If the game already started, return. (Shouldn't happen)
        if (!game.getState().isWaiting() && !game.getState().isCountdown()) {
            RUtils.handleMessage(player, Messages.Message.ALREADY_STARTED.asString());
            return false;
        }

        Double minAmount = PLUGIN.getChips().getMinAmount();
        if (minAmount == null) return false;

        // If the player doesn't have the minimum amount of money required, return.
        if (!PLUGIN.getEconomy().has(player, minAmount)) {
            RUtils.handleMessage(player, Messages.Message.MIN_REQUIRED.asString().replace("%money%", String.valueOf(minAmount)));
            return false;
        }

        return true;
    }

    public static boolean isPluginVanished(Player player) {
        Iterator<MetadataValue> iterator = player.getMetadata("vanished").iterator();

        MetadataValue meta;
        do {
            if (!iterator.hasNext()) {
                return false;
            }

            meta = iterator.next();
        } while (!meta.asBoolean());

        return true;
    }

    public static BlockFace[] getCorrectFacing(BlockFace face) {
        BlockFace first = getPreviousFace(face);
        BlockFace second = getPreviousFace(first);
        BlockFace third = getPreviousFace(second);

        return new BlockFace[]{first, first, first, first, second, second, third, third, third, third};
    }

    public static Vector getDirection(BlockFace face) {
        try {
            // Added in 1.13.
            return face.getDirection();
        } catch (NoSuchMethodError error) {
            int modX = face.getModX(), modY = face.getModY(), modZ = face.getModZ();
            Vector direction = new Vector(modX, modY, modZ);
            if (modX != 0 || modY != 0 || modZ != 0) direction.normalize();
            return direction;
        }
    }

    public static BlockFace faceFromYaw(float yaw, boolean useSubCardinal) {
        return useSubCardinal ? RADIAL[Math.round(yaw / 45f) & 0x7].getOppositeFace() : AXIS[Math.round(yaw / 90f) & 0x3].getOppositeFace();
    }

    public static Vector offsetVector(Vector vector, float yawDegrees, float pitchDegrees) {
        double yaw = Math.toRadians(-1 * (yawDegrees + 90)), pitch = Math.toRadians(-pitchDegrees);

        double cosYaw = Math.cos(yaw), cosPitch = Math.cos(pitch);

        double sinYaw = Math.sin(yaw), sinPitch = Math.sin(pitch);

        double initialX, initialY, initialZ, x, y, z;

        initialX = vector.getX();
        initialY = vector.getY();
        x = initialX * cosPitch - initialY * sinPitch;
        y = initialX * sinPitch + initialY * cosPitch;

        initialZ = vector.getZ();
        initialX = x;
        z = initialZ * cosYaw - initialX * sinYaw;
        x = initialZ * sinYaw + initialX * cosYaw;

        return new Vector(x, y, z);
    }

    public static String[] arrayToStrings(Object... array) {
        String[] result = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] != null ? array[i].toString() : null;
        }
        return result;
    }

    public static ItemStack createHead(String url) {
        url = "http://textures.minecraft.net/texture/" + url;

        ItemStack item = XMaterial.PLAYER_HEAD.parseItem();
        if (item == null) return null;

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return null;

        item.setItemMeta(SkullUtils.applySkin(meta, url));

        return item;
    }

    public static String oldTranslate(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String translate(String message) {
        Validate.notNull(message, "Message can't be null.");

        if (getMajorVersion() < 16) return oldTranslate(message);

        Matcher matcher = PATTERN.matcher(oldTranslate(message));
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(buffer, ChatColor.of(matcher.group(1)).toString());
        }

        return matcher.appendTail(buffer).toString();
    }

    public static List<String> translate(List<String> messages) {
        Validate.notNull(messages, "Messages can't be null.");

        messages.replaceAll(RUtils::translate);
        return messages;
    }

    public static void handleMessage(CommandSender sender, String message) {
        String newMessage = message.replace("[AB]", "");
        if (!message.startsWith("[AB]") || !(sender instanceof Player)) {
            sender.sendMessage(newMessage);
            return;
        }
        ActionBar.sendActionBar(PLUGIN, ((Player) sender), newMessage, 50L);
    }

    public static String getSlotName(Slot slot) {
        if (slot.isSingle()) {
            String number = slot.isDoubleZero() ? "00" : String.valueOf(slot.getInts()[0]);
            switch (slot.getColor()) {
                case RED:
                    return Configuration.Config.SINGLE_RED.asString().replace("%number%", number);
                case BLACK:
                    return Configuration.Config.SINGLE_BLACK.asString().replace("%number%", number);
                default:
                    return Configuration.Config.SINGLE_ZERO.asString().replace("%number%", number);
            }
        } else if (slot.isColumn() || slot.isDozen()) {
            boolean isColumn = slot.isColumn();
            return PLUGIN.getConfiguration().getColumnOrDozen(isColumn ? "column" : "dozen", isColumn ? slot.getColumn() : slot.getDozen());
        }
        switch (slot) {
            case LOW:
                return Configuration.Config.LOW.asString();
            case EVEN:
                return Configuration.Config.EVEN.asString();
            case ODD:
                return Configuration.Config.ODD.asString();
            case HIGH:
                return Configuration.Config.HIGH.asString();
            case RED:
                return Configuration.Config.RED.asString();
            default:
                return Configuration.Config.BLACK.asString();
        }
    }

    public static BlockFace getNextFace(BlockFace face) {
        int index = ArrayUtils.indexOf(AXIS, face) + 1;
        return AXIS[index > (AXIS.length - 1) ? 0 : index];
    }

    public static BlockFace getPreviousFace(BlockFace face) {
        int index = ArrayUtils.indexOf(AXIS, face) - 1;
        return AXIS[index < 0 ? (AXIS.length - 1) : index];
    }

    public static boolean isVillageAndPillage() {
        return getMajorVersion() == 14;
    }

    public static int getMajorVersion() {
        return Integer.parseInt(ReflectionUtils.VERSION.split("_")[1]);
    }
}