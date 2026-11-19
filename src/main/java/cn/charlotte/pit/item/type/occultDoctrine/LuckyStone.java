package cn.charlotte.pit.item.type.occultDoctrine;

import cn.charlotte.pit.enchantment.DoEnch;
import cn.charlotte.pit.enchantment.EnchData;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.MythicColor;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.random.RandomUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class LuckyStone extends AbstractPitItem implements DoEnch {

    @Override
    public String getInternalName() {
        return "lucky_stone";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&a幸运石";
    }

    @Override
    public ItemStack toItemStack() {
        double lucky = RandomUtil.random.nextInt(5) + 1 + (int) (Math.random() * 10) % 10;
        return new ItemBuilder(Material.STONE)
                .internalName(getInternalName())
                .canTrade(true)
                .changeNbt("lucky", lucky)
                .canSaveToEnderChest(true)
                .canDrop(false)
                .deathDrop(false)
                .removeOnJoin(false)
                .lore("", "&7表面圆润光滑的小石子,", String.format("&7在神话之井中使用,增加此次附魔稀有的概率 &a+%s%%", lucky),
                        "&7仅限非暗黑神话装备 &aI 阶 → II 阶",
                        "&7升级完成后消耗一颗，不保证出现稀有附魔")
                .name("&a幸运石")
                .build();
    }

    @Override
    public void loadFromItemStack(ItemStack item) {
        super.toItemStack();
    }

    @Override
    public void ench(EnchData enchData) {
        // 幸运石只参与生成 T2 装备时的稀有附魔判定；其他阶段保留物品，避免被无效消耗。
        if (enchData.getMythicItem().getTier() != 2) {
            return;
        }
        enchData.getArgs().put("rare", ItemUtil.readItemDoubleData(InventoryUtil.deserializeItemStack(enchData.getProfile().getAttachedEnchant()), "lucky"));
        enchData.getProfile().setAttachedEnchant(null);

    }

    public static boolean canUse(MythicColor color, int currentTier) {
        return color != null && color != MythicColor.DARK && currentTier == 1;
    }
}
