package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.AdvancedItemIdentity;
import cn.charlotte.pit.util.item.ItemBuilder;
import com.google.common.collect.Lists;
import de.tr7zw.nbtapi.NBT;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomNamePaper extends AbstractPitItem {

    private UUID uuid;
    private String customName;
    private boolean allowed;
    private long millis;

    public static boolean valid(String text) {
        String translated = ChatColor.translateAlternateColorCodes('&', text);
        translated = ChatColor.stripColor(translated);
        int length = translated.length();
        return length > 0 && length < 13;
    }

    public static CustomNamePaper createTestItem(String customName) {
        CustomNamePaper paper = new CustomNamePaper();
        paper.setUuid(UUID.randomUUID());
        paper.setCustomName(customName);
        paper.setAllowed(true);
        paper.setMillis(System.currentTimeMillis());
        return paper;
    }

    @Override
    public ItemStack toItemStack() {
        String itemDisplayName = getItemDisplayName();
        List<String> lore = Lists.newArrayList("&7死亡后保留");
        if (StringUtils.isNotEmpty(customName)) {
            itemDisplayName += " - " + customName;
            if (allowed) {
                lore.add("&f手持要重命名的物品在左手上");
                lore.add("&f再右键即可更名");
                lore.add("&7");
                lore.add("&f长度限制为12个字符");
                lore.add("&f支持颜色字符");
                lore.add("&f请规范用语否则将不予审核");
                lore.add("&f审核状态每10分钟更新一次");
            } else {
                lore.add("&f改名审核中");
                lore.add("&f右键查看审核状态");
            }
        } else {
            lore.add("&f右键开始改名");
        }
        ItemBuilder builder = new ItemBuilder(getItemDisplayMaterial())
                .composedName(itemDisplayName)
                .lore(lore)
                .deathDrop(false)
                .canSaveToEnderChest(true)
                .canDrop(false)
                .customName(customName)
                .canTrade(true)
                .changeNbt("millis", millis)
                .changeNbt("allowed", allowed)
                .internalName(getInternalName());
        if (uuid != null) {
            // The approval record must remain addressable even when item UUIDs are disabled.
            if (isCustomNameApplied()) builder.changeNbt("nameRequestId", uuid.toString());
            if (isUuidEnabled()) builder.uuid(uuid);
        }
        return builder.build();
    }

    @Override
    public void loadFromItemStack(ItemStack item) {
        NBT.get(item, tag -> {
            Optional.ofNullable(tag.getCompound("extra"))
                    .ifPresent(extra -> {
                        this.customName = extra.getString("customName");
                        String requestId = extra.hasTag("nameRequestId")
                                ? extra.getString("nameRequestId") : extra.getString("uuid");
                        this.uuid = AdvancedItemIdentity.isValidUuid(requestId) ? UUID.fromString(requestId) : null;
                        if (isCustomNameApplied() && uuid == null) {
                            throw new IllegalArgumentException("改名券缺少有效的审核编号");
                        }
                        this.allowed = extra.getBoolean("allowed");
                        this.millis = extra.getLong("millis");
                    });
        });
    }

    @Override
    public String getInternalName() {
        return "CustomNamePaper";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&f改名券";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.PAPER;
    }

    public boolean isCustomNameApplied() {
        return StringUtils.isNotEmpty(customName);
    }
}
