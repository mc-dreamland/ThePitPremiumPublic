package cn.charlotte.pit.item.type.occultDoctrine;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.DoEnch;
import cn.charlotte.pit.enchantment.EnchData;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.stream.Collectors;

/**
 * 相位书
 * 吸引某些附魔
 */
@Slf4j
public class phaseBook extends AbstractPitItem implements DoEnch {

    @Override
    public ItemStack toItemStack() {
        AbstractEnchantment enchantment = ThePit.getInstance().getEnchantmentFactor().getEnchantments()
                .stream()
                .filter(e1 -> Util.matchesAny(e1.getRarity(), EnchantmentRarity.RARE, EnchantmentRarity.NORMAL))
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> list.isEmpty() ? null : list.get(Util.random.nextInt(list.size()))
                ));
        double probability = (double) ((int) ((Util.random.nextDouble(enchantment.getRarity() == EnchantmentRarity.RARE ? 0.009 : 0.099) + 0.001) * 1000)) / 1000;
        return new ItemBuilder(Material.BOOK)
                .name(getItemDisplayName())
                .internalName(getInternalName())
                .removeOnJoin(false)
                .canTrade(true)
                .canDrop(false)
                .deathDrop(false)
                .changeNbt(getInternalName(), enchantment.getNbtName())
                .changeNbt("probability", probability)
                .canSaveToEnderChest(true)
                .lore(
                        "",
                        "&7一本奇怪的书?似乎蕴藏着某种魔力",
                        "",
                        "&7在 &e神话之井 &7中使用,增加定向附魔概率",
                        String.format("&7定向附魔: &9%s", enchantment.getEnchantName()),
                        String.format("&7获得定向附魔概率: &e%s%%", Util.s0_0.format(probability * 100)),
                        ""
                )
                .shiny()
                .build()
                ;
    }

    @Override
    public String getInternalName() {
        return "phase_book";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&b相位书";
    }

    @Override
    public void ench(EnchData enchData) {
        if (enchData.getMythicItem().isBoostedByPhaseBook()) return;
        enchData.getArgs().put("phase_book", ItemUtil.getItemStringData(InventoryUtil.deserializeItemStack(enchData.getProfile().getAttachedEnchant()), "phase_book"));
        String probStr = ItemUtil.getItemStringData(InventoryUtil.deserializeItemStack(enchData.getProfile().getAttachedEnchant()), "probability");
        double probability;
        if (probStr == null || probStr.trim().isEmpty()) {
            probability = 0.01;
        } else {
            try {
                probability = Double.parseDouble(probStr);
            } catch (NumberFormatException e) {
                probability = 0.01;
            }
        }
        enchData.getArgs().put("probability", probability);
        enchData.getProfile().setAttachedEnchant(null);
    }
}
