package cn.charlotte.pit.util.item;

import cn.charlotte.pit.item.IMythicItem;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class MythicBundleUuidUtil {

    public static final int BUNDLE_SIZE = 10;
    private static final String UUID_KEY_PREFIX = "item_uuid_";

    private MythicBundleUuidUtil() {
    }

    public static void store(ItemBuilder bundleBuilder, int index, ItemStack item) {
        AdvancedItemIdentity.ensureUuid(item);
        String uuid = ItemUtil.getUUID(item);
        if (uuid != null && !uuid.isBlank()) {
            bundleBuilder.changeNbt(key(index), uuid);
        }
    }

    public static void restore(IMythicItem mythicItem, ItemStack bundle, int index) {
        String uuid = ItemUtil.getItemStringData(bundle, key(index));
        if (uuid == null || uuid.isBlank()) {
            return;
        }

        try {
            mythicItem.setUuid(UUID.fromString(uuid));
        } catch (IllegalArgumentException ignored) {
            // Legacy or malformed bundles fall back to a newly generated equipment UUID.
        }
    }

    public static String getStoredUuid(ItemStack bundle, int index) {
        return ItemUtil.getItemStringData(bundle, key(index));
    }

    public static String key(int index) {
        return UUID_KEY_PREFIX + index;
    }
}
