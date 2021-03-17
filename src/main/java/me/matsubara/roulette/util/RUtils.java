package me.matsubara.roulette.util;

import com.cryptomorin.xseries.ReflectionUtils;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.messages.ActionBar;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.matsubara.roulette.Roulette;
import me.matsubara.roulette.data.Slot;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RUtils {

    // Not the best way to access the plugin tbh.
    private final static Roulette plugin = JavaPlugin.getPlugin(Roulette.class);
    private final static Pattern pattern = Pattern.compile("(&)?&#([0-9a-fA-F]{6})");

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

        byte[] encodedData = Base64.getEncoder().encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());

        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        profile.getProperties().put("textures", new Property("textures", new String(encodedData)));

        try {
            Field field = meta.getClass().getDeclaredField("profile");
            field.setAccessible(true);
            field.set(meta, profile);
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }

        item.setItemMeta(meta);
        return item;
    }

    public static String oldTranslate(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String translate(String message) {
        Validate.notNull(message, "Message can't be null.");

        if (getMajorVersion() < 16) return oldTranslate(message);

        Matcher matcher = pattern.matcher(oldTranslate(message));
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
        ActionBar.sendActionBar(plugin, ((Player) sender), newMessage, 50L);
    }

    public static String getSlotName(Slot slot) {
        if (slot.isSingle()) {
            String number = slot.isDoubleZero() ? "00" : String.valueOf(slot.getInts()[0]);
            switch (slot.getColor()) {
                case RED:
                    return plugin.getConfiguration().getSingleRed(number);
                case BLACK:
                    return plugin.getConfiguration().getSingleBlack(number);
                default:
                    return plugin.getConfiguration().getZero(number);
            }
        } else if (slot.isColumn() || slot.isDozen()) {
            boolean isColumn = slot.isColumn();
            return plugin.getConfiguration().getColumnOrDozen(isColumn ? "column" : "dozen", isColumn ? slot.getColumn() : slot.getDozen());
        }
        switch (slot) {
            case LOW:
                return plugin.getConfiguration().getLow();
            case EVEN:
                return plugin.getConfiguration().getEven();
            case ODD:
                return plugin.getConfiguration().getOdd();
            case HIGH:
                return plugin.getConfiguration().getHigh();
            case RED:
                return plugin.getConfiguration().getNameRed();
            default:
                return plugin.getConfiguration().getNameBlack();
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

    public static int getMajorVersion() {
        return Integer.parseInt(ReflectionUtils.VERSION.split("_")[1]);
    }
}