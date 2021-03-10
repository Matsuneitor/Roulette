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
import org.bukkit.Location;
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

public final class RUtilities {

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

    public static Location getCorrectLocation(Player player) {
        Location location = player.getTargetBlock(null, 5).getLocation();
        BlockFace face = getNextFace(faceFromYaw(player.getLocation().getYaw(), false));
        location.setDirection(face.getOppositeFace().getDirection());
        return location;
    }

    public static BlockFace[] getCorrectFacing(BlockFace face) {
        BlockFace first = getPreviousFace(face);
        BlockFace second = getPreviousFace(first);
        BlockFace third = getPreviousFace(second);

        return new BlockFace[]{first, first, first, first, second, second, third, third, third, third};
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

    public static String translate(String message) {
        if (message == null) return null;
        if (isNetherUpdate()) {
            Matcher matcher = pattern.matcher(ChatColor.translateAlternateColorCodes('&', message));
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(buffer, ChatColor.of(matcher.group(1)).toString());
            }
            return matcher.appendTail(buffer).toString();
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static List<String> translate(List<String> messages) {
        if (messages == null) return null;
        messages.replaceAll(RUtilities::translate);
        return messages;
    }

    public static void handleMessage(CommandSender sender, String message) {
        if (message.startsWith("[AB]")) {
            message = message.substring(4);
            if (sender instanceof Player) {
                ActionBar.sendActionBar(plugin, ((Player) sender), message, 50L);
                return;
            }
        }
        sender.sendMessage(message);
    }

    public static String getSlotName(Slot slot) {
        if (slot.isSingle()) {
            String number = slot.isDoubleZero() ? "00" : String.valueOf(slot.getInts()[0]);
            return slot.isRed() ? plugin.getConfiguration().getSingleRed(number) : slot.isBlack() ? plugin.getConfiguration().getSingleBlack(number) : plugin.getConfiguration().getZero(number);
        }
        if (slot.isColumn()) {
            return plugin.getConfiguration().getColumnOrDozen("column", slot.getColumn());
        }
        if (slot.isDozen()) {
            return plugin.getConfiguration().getColumnOrDozen("dozen", slot.getDozen());
        }
        if (slot.isLow()) {
            return plugin.getConfiguration().getLow();
        }
        if (slot.isEven()) {
            return plugin.getConfiguration().getEven();
        }
        if (slot.isHigh()) {
            return plugin.getConfiguration().getHigh();
        }
        if (slot.isOdd()) {
            return plugin.getConfiguration().getOdd();
        }
        return slot.isRed() ? plugin.getConfiguration().getNameRed() : plugin.getConfiguration().getNameBlack();
    }

    private static BlockFace getNextFace(BlockFace face) {
        int index = ArrayUtils.indexOf(AXIS, face) + 1;
        return AXIS[index > (AXIS.length - 1) ? 0 : index];
    }

    private static BlockFace getPreviousFace(BlockFace face) {
        int index = ArrayUtils.indexOf(AXIS, face) - 1;
        return AXIS[index < 0 ? (AXIS.length - 1) : index];
    }

    private static boolean isNetherUpdate() {
        return ReflectionUtils.VERSION.startsWith("v1_16_R");
    }
}