package cn.charlotte.pit.util.item;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/1 1:37
 */
public class ItemUtil {

    public static final NamespacedKey KEY_INTERNAL_NAME = new NamespacedKey("pit", "internal");

    public static String getUUID(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
            return null;
        }

        return NBT.get(item, nbt -> {
            if (!nbt.hasTag("extra")) {
                return null;
            }
            ReadableNBT extra = nbt.getCompound("extra");
            if (extra.hasTag("uuid")) {
                return extra.getString("uuid");
            }
            return null;
        });
    }


    public static boolean isIllegalItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
            return false;
        }

        String internalName = getInternalName(item);
        if (internalName == null) {
            return true;
        }
        return internalName.endsWith("_reward");
    }

    public static boolean canDrop(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        return NBT.get(item, nbt -> {
            if (!nbt.hasTag("extra")) {
                return false;
            }
            ReadableNBT extra = nbt.getCompound("extra");
            return extra.hasTag("tradeAllow") && extra.getBoolean("tradeAllow");
        });
    }

    public static boolean isHealingItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        return NBT.get(item, nbt -> {
            if (!nbt.hasTag("extra")) {
                return false;
            }
            ReadableNBT extra = nbt.getCompound("extra");
            return extra.hasTag("isHealingItem") && extra.getBoolean("isHealingItem");
        });
    }

    public static boolean canTrade(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        return NBT.get(item, nbt -> {
            if (!nbt.hasTag("extra")) {
                return false;
            }
            ReadableNBT extra = nbt.getCompound("extra");
            String internal = extra.hasTag("internal") ? extra.getString("internal") : null;
            return (extra.hasTag("canTrade") && extra.getBoolean("canTrade")) || (internal != null && internal.startsWith("mythic_"));
        });
//        NbtCompound tag = Utils.getCompound(item);
//        if (tag == null || !tag.containsKey("extra")) {
//            return false;
//        }
//        NbtCompound extra = tag.getCompound("extra");
//        return (extra.containsKey("canTrade") && extra.getByte("canTrade") != 0) || (extra.containsKey("internal") && extra.getString("internal").startsWith("mythic_"));
    }

    public static boolean canSaveEnderChest(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        return NBT.get(item, nbt -> {
            if (!nbt.hasTag("extra")) {
                return false;
            }
            ReadableNBT extra = nbt.getCompound("extra");
            return extra.hasTag("enderChest") && extra.getBoolean("enderChest");
        });
    }

    public static boolean isDefaultItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        return NBT.get(item, nbt -> {
            if (!nbt.hasTag("extra")) {
                return false;
            }
            ReadableNBT extra = nbt.getCompound("extra");
            return extra.hasTag("defaultItem") && extra.getBoolean("defaultItem");
        });
    }

    public static boolean isDeathDrop(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        return NBT.get(item, nbt -> {
            if (!nbt.hasTag("extra")) {
                return false;
            }
            ReadableNBT extra = nbt.getCompound("extra");

            if ("beast_mode_helmet".equals(extra.getString("internal"))) {
                return false;
            }

            return extra.hasTag("deathDrop") && extra.getBoolean("deathDrop");
        });
    }

    public static boolean isRemovedOnJoin(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        return NBT.get(item, nbt -> {
            if (!nbt.hasTag("extra")) {
                return false;
            }
            ReadableNBT extra = nbt.getCompound("extra");
            return extra.hasTag("removeOnJoin") && extra.getBoolean("removeOnJoin");
        });
    }

    /**
     * 因为该方法的特殊性所以返回值
     *
     * @return true(有key且vaule为true) || false(有key且value为false) || null(没有key)
     */
    public static Boolean isTrash(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        return NBT.get(item, nbt -> {
            if (!nbt.hasTag("extra")) {
                return null;
            }
            ReadableNBT extra = nbt.getCompound("extra");
            return extra.hasTag("canTrash") && extra.getBoolean("canTrash");
        });
    }

    @Nullable
    public static String getInternalName(@Nullable ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String internal = container.get(KEY_INTERNAL_NAME, PersistentDataType.STRING);
        if (internal != null) {
            return internal;
        }
        return NBT.get(item, nbt -> {
            if (!nbt.hasTag("extra")) {
                return null;
            }
            ReadableNBT extra = nbt.getCompound("extra");
            if (extra.hasTag("internal")) {
                String internal0 = extra.getString("internal");
                container.set(KEY_INTERNAL_NAME, PersistentDataType.STRING, internal0);
                item.setItemMeta(meta);
                return internal0;
            }
            return null;
        });
//        NbtCompound tag = Utils.getCompound(item);
//        if (tag == null || !tag.containsKey("extra")) {
//            return null;
//        }
//        NbtCompound extra = tag.getCompound("extra");
//        if (extra.containsKey("internal")) {
//            return extra.getString("internal");
//        }
//        return null;
    }

    public static String getInternalNameNotNull(@Nullable ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return "";
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String internal = container.get(KEY_INTERNAL_NAME, PersistentDataType.STRING);
        if (internal != null) {
            return internal;
        }
        return NBT.get(item, nbt -> {
            if (!nbt.hasTag("extra")) {
                return "";
            }
            ReadableNBT extra = nbt.getCompound("extra");
            if (extra.hasTag("internal")) {
                String internal0 = extra.getString("internal");
                container.set(KEY_INTERNAL_NAME, PersistentDataType.STRING, internal0);
                item.setItemMeta(meta);
                return internal0;
            }
            return "";
        });
    }
    public static boolean hasInternalName(@Nullable ItemStack item) {
        return getInternalName(item) != null;
    }

    public static Integer getItemIntData(ItemStack item, String key) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        return NBT.get(item, nbt -> {
            if (!nbt.hasTag("extra")) {
                return null;
            }
            ReadableNBT extra = nbt.getCompound("extra");
            if (extra.hasTag(key)) {
                return extra.getInteger(key);
            }
            return null;
        });
    }

    public static String getItemStringData(ItemStack item, String key) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        return NBT.get(item, nbt -> {
            if (!nbt.hasTag("extra")) {
                return null;
            }
            ReadableNBT extra = nbt.getCompound("extra");
            if (extra.hasTag(key)) {
                return extra.getString(key);
            }
            return null;
        });
    }

    public static Double readItemDoubleData(ItemStack item, String key) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        return NBT.get(item, nbt -> {
            if (!nbt.hasTag(key)) return null;
            return nbt.getDouble(key);
        });
    }
}
