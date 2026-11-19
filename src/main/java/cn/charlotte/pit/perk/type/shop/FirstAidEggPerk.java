package cn.charlotte.pit.perk.type.shop;

import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksShopEnum;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/3/17 16:58
 */
public class FirstAidEggPerk extends TypedPerk<PerksShopEnum> {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "商店升级: 温泉蛋";
    }

    @Override
    public Material getIcon() {
        return Material.AXOLOTL_SPAWN_EGG;
    }

    @Override //200 coins for the item
    public double requireCoins() {
        return 0;
    }

    @Override
    public double requireRenown(int level) {
        return 20;
    }

    @Override
    public int requirePrestige() {
        return 5;
    }

    @Override
    public int requireLevel() {
        return 0;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7允许你在商店中购买温泉蛋.");
        lines.add(" ");
        lines.add("&7温泉蛋会在使用后,恢复使用者 &c2.5❤ &7生命值并移除使用者的速度效果.");
        lines.add("&7&o30 秒内只能使用一次温泉蛋,击杀玩家可以减少5秒冷却时间.");
        return lines;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public PerkType getPerkType() {
        return PerkType.PERK;
    }

    @Override
    public PerkGroup getPerkGroup() {
        return PerkGroup.SHOP;
    }

    @Override
    public void onPerkActive(Player player) {

    }

    @Override
    public void onPerkInactive(Player player) {

    }

    @Override
    public ItemStack getIconWithNameAndLore(String name, List<String> lore, int durability, int amount) {
        return new ItemBuilder(Material.AXOLOTL_SPAWN_EGG)
                .name(name)
                .lore(lore)
                .amount(amount)
                .build();
    }
}
