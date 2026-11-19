package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/3/22 17:44
 */
public class SpireSword extends AbstractPitItem {
    private Material material;

    public SpireSword(Material material) {
        this.material = material;
    }

    @Override
    public String getInternalName() {
        return "spire_sword";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&c尖塔利刃";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return material;
    }

    @Override
    public ItemStack toItemStack() {
        final List<String> lore = new ArrayList<>();
        lore.add("&7事件物品");
        lore.add("");
        lore.addAll(this.getEnchantLore());
        lore.remove(lore.size() - 1);

        return new ItemBuilder(this.getItemDisplayMaterial())
                .name(this.getItemDisplayName())
                .internalName(this.getInternalName())
                .removeOnJoin(true)
                .deathDrop(true)
                .enchant(this.enchantments)
                .lore(lore)
                .buildWithUnbreakable();
    }

    @Override
    public void loadFromItemStack(ItemStack item) {
        this.material = item.getType();
    }
}
