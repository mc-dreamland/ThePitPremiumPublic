package cn.charlotte.pit.item.type.occultDoctrine;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.DoEnch;
import cn.charlotte.pit.enchantment.EnchData;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BrokenScroll extends AbstractPitItem implements DoEnch {

    private int level = 1;
    private List<String> list = new ArrayList<>(
            ThePit.getInstance()
                    .getEnchantmentFactor()
                    .getEnchantments()
                    .stream()
                    .map(AbstractEnchantment::getNbtName)
                    .toList().subList(0, level)
    );

    @Override
    public void ench(EnchData enchData) {

    }

    @Override
    public ItemStack toItemStack() {

        return new ItemBuilder(Material.PAPER)
                .name(getItemDisplayName())
                .internalName(getInternalName())
                .lore(
                        "",
                        "&7一卷破损严重的卷轴，勉强可以看到上面的描述",
                        " &7使用 &f神奇的墨水可以更改上面的内容",
                        ""
                )

                .build();
    }

    @Override
    public String getInternalName() {
        return "broken_scroll";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "破碎卷轴";
    }
}
