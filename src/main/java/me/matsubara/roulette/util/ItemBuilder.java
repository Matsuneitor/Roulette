package me.matsubara.roulette.util;

import org.apache.commons.lang.Validate;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings({"unused", "ConstantConditions", "UnusedReturnValue"})
public final class ItemBuilder implements Cloneable {

    private final ItemStack item;
    private final static PersistentDataType<Object, Object>[] DATA_TYPES = getDataTypes();

    private JavaPlugin plugin;

    public ItemBuilder(Material material) {
        item = new ItemStack(material);
    }

    public ItemBuilder(ItemStack item) {
        this.item = item;
    }

    public ItemBuilder(String url) {
        this.item = RUtils.createHead(url);
    }

    @Override
    public ItemBuilder clone() {
        ItemBuilder clone;
        try {
            return (ItemBuilder) super.clone();
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException("superclass messed up", exception);
        }
    }

    public ItemBuilder setType(Material type) {
        item.setType(type);
        return this;
    }

    public ItemBuilder setAmount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder setDamage(int damage) {
        ItemMeta meta = item.getItemMeta();
        ((Damageable) meta).setDamage(damage);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder setOwningPlayer(OfflinePlayer player) {
        try {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            item.setItemMeta(meta);
        } catch (ClassCastException ignore) {
        }
        return this;
    }

    public ItemBuilder setDisplayName(String displayName) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(RUtils.translate(displayName));
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        meta.setLore(RUtils.translate(lore));
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder setLeatherArmorMetaColor(Color color) {
        try {
            LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
            meta.setColor(color);
            item.setItemMeta(meta);
        } catch (ClassCastException ignore) {
        }
        return this;
    }

    public ItemBuilder addEnchantment(Enchantment enchantment, int level, boolean unsafe) {
        if (unsafe) {
            item.addUnsafeEnchantment(enchantment, level);
        } else {
            item.addEnchantment(enchantment, level);
        }
        return this;
    }

    public ItemBuilder removeEnchantment(Enchantment enchantment) {
        item.removeEnchantment(enchantment);
        return this;
    }

    public ItemBuilder addItemFlags(ItemFlag... flags) {
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(flags);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder removeItemFlags(ItemFlag... flags) {
        ItemMeta meta = item.getItemMeta();
        meta.removeItemFlags(flags);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder plugin(JavaPlugin plugin) {
        this.plugin = plugin;
        return this;
    }

    public <T, Z> ItemBuilder setKey(String key, Z value) {
        Validate.notNull(plugin, "JavaPlugin can't be null.");
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, key), getDataType(value.getClass()), value);
        item.setItemMeta(meta);
        return this;
    }

    @SuppressWarnings("unchecked")
    private static PersistentDataType<Object, Object>[] getDataTypes() {
        Field[] fields = PersistentDataType.class.getDeclaredFields();

        Set<PersistentDataType<?, ?>> results = new HashSet<>();
        for (Field field : fields) {
            if (!field.getType().equals(PersistentDataType.class)) continue;
            try {
                results.add((PersistentDataType<?, ?>) field.get(null));
            } catch (IllegalAccessException exception) {
                exception.printStackTrace();
            }
        }
        return (PersistentDataType<Object, Object>[]) results.toArray(new PersistentDataType<?, ?>[0]);
    }

    private PersistentDataType<Object, Object> getDataType(Class<?> clazz) {
        for (PersistentDataType<Object, Object> dataType : DATA_TYPES) {
            if (dataType.getPrimitiveType().equals(clazz)) return dataType;
        }
        return null;
    }

    public ItemStack build() {
        addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        return item;
    }
}