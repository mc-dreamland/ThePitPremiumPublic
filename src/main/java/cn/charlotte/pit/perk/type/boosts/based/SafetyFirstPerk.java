package cn.charlotte.pit.perk.type.boosts.based;

import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksBoostsEnum;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/1 22:20
 */
public class SafetyFirstPerk extends TypedPerk<PerksBoostsEnum> {

    public static final ItemStack CHAINMAIL_HELMET = new ItemBuilder(Material.CHAINMAIL_HELMET).deathDrop(false).canDrop(false).canSaveToEnderChest(false).internalName("perk_safety_first").lore("&7天赋物品").buildWithUnbreakable();


    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "安全第一";
    }

    @Override
    public Material getIcon() {
        return Material.CHAINMAIL_HELMET;
    }

    @Override
    public double requireCoins() {
        return 3000;
    }

    @Override
    public double requireRenown(int level) {
        return 0;
    }

    @Override
    public int requirePrestige() {
        return 0;
    }

    @Override
    public int requireLevel() {
        return 30;
    }

    @Override
    public PerkType getPerkType() {
        return PerkType.PERK;
    }

    @Override
    public PerkGroup getPerkGroup() {
        return PerkGroup.BOOST;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7天赋携带期间额外获得以下物品:");
        lines.add("  &f▶ &7锁链头盔");
        return lines;
    }

    @Override
    public int getMaxLevel() {
        return 0;
    }

    @Override
    public void onPerkActive(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null || helmet.getType().equals(Material.AIR)) {
            player.getInventory().setHelmet(CHAINMAIL_HELMET.clone());
        } else {
            player.getInventory().addItem(CHAINMAIL_HELMET.clone());
        }
    }

    @Override
    public void onPerkInactive(Player player) {
        InventoryUtil.removeItemWithInternalName(player, "perk_safety_first");
    }

    @Override
    public void handleRespawn(int enchantLevel, Player player) {
        InventoryUtil.removeItemWithInternalName(player, "perk_safety_first");

        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null || helmet.getType().equals(Material.AIR)) {
            player.getInventory().setHelmet(CHAINMAIL_HELMET.clone());
        } else {
            player.getInventory().addItem(CHAINMAIL_HELMET.clone());
        }
    }
}
