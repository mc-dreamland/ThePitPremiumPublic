package cn.charlotte.pit.perk.type.shop;

import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksShopEnum;
import cn.charlotte.pit.perk.TypedPerk;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * @author Araykal
 * @since 2025-02-04
 */
public class BowBundleShopPerk extends TypedPerk<PerksShopEnum> {

    // ────── 基础信息 ──────
    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "神话之弓收纳箱";
    }

    @Override
    public Material getIcon() {
        return Material.CHEST_MINECART;   // 若使用 1.13+ API，请改为 Material.MINECART_CHEST
    }

    @Override
    public double requireCoins() {
        return 0.0;
    }

    @Override
    public double requireRenown(int level) {
        return 30.0;
    }

    @Override
    public int requirePrestige() {
        return 20;
    }

    @Override
    public int requireLevel() {
        return 0;
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
    public List<String> getDescription(Player player) {
        return Arrays.asList(
                "&7允许你在商店中购买神话之弓收纳箱.",
                "&7神话之弓收纳箱使用后会打包背包内",
                "&710条&c未附魔&7的神话之弓.",
                "&7并允许你随时将其取出."
        );
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    // ────── 激活/失活：当前无额外逻辑 ──────
    @Override
    public void onPerkActive(Player player) {
        // 无实现
    }

    @Override
    public void onPerkInactive(Player player) {
        // 无实现
    }
}
