package cn.charlotte.pit.util.item;

import cn.charlotte.pit.config.ItemSettings;
import cn.charlotte.pit.util.Utils;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * 高级物品 UUID 的统一读写入口。
 */
public final class AdvancedItemIdentity {

    private AdvancedItemIdentity() {
    }

    /**
     * 为缺少合法 UUID 的高级物品补充 UUID，已有合法 UUID 保持不变。
     *
     * @return 是否修改了物品
     */
    public static boolean ensureUuid(ItemStack item) {
        if (!uuidEnabled(item) || isValidUuid(ItemUtil.getUUID(item))) {
            return false;
        }
        writeUuid(item, UUID.randomUUID());
        return true;
    }

    /**
     * 新发放的高级物品从缓存模板克隆后必须重新生成 UUID。
     *
     * @return 是否修改了物品
     */
    public static boolean regenerateUuid(ItemStack item) {
        if (!uuidEnabled(item)) {
            return false;
        }
        writeUuid(item, UUID.randomUUID());
        return true;
    }

    /**
     * 同一件高级物品发生升级或形态转换时，将原 UUID 继承到结果物品。
     */
    public static boolean inheritUuid(ItemStack source, ItemStack target) {
        if (!uuidEnabled(source) || !uuidEnabled(target)) {
            return false;
        }
        ensureUuid(source);
        UUID uuid = readUuid(source);
        if (uuid == null) {
            return false;
        }
        writeUuid(target, uuid);
        return true;
    }

    public static UUID readUuid(ItemStack item) {
        String value = ItemUtil.getUUID(item);
        if (!isValidUuid(value)) {
            return null;
        }
        return UUID.fromString(value);
    }

    public static boolean isValidUuid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            return UUID.fromString(value).toString().equalsIgnoreCase(value);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public static boolean uuidEnabled(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
            return false;
        }
        return ItemSettings.uuidEnabled(ItemUtil.getInternalName(item));
    }

    static void writeUuid(ItemStack item, UUID uuid) {
        NBT.modify(item, nbt -> {
            nbt.getOrCreateCompound("extra").setString("uuid", uuid.toString());
        });
        Utils.invalidateMythicItemCache(item);
    }
}
