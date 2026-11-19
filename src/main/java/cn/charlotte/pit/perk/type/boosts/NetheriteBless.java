package cn.charlotte.pit.perk.type.boosts;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksBoostsEnum;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.random.RandomUtil;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static org.bukkit.inventory.EquipmentSlot.FEET;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/12 19:27
 */
public class NetheriteBless extends TypedPerk<PerksBoostsEnum> {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "下界祝福";
    }

    @Override
    public Material getIcon() {
        return Material.NETHERITE_INGOT;
    }

    @Override
    public double requireCoins() {
        return 10000;
    }

    @Override
    public double requireRenown(int level) {
        return 15;
    }

    @Override
    public int requirePrestige() {
        return 8;
    }

    @Override
    public int requireLevel() {
        return 90;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7击杀时有 &b5% &7的几率");
        lines.add("&7将你的钻石头或鞋子升级为下界合金护甲");
        return lines;
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
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public void onPerkActive(Player player) {

    }

    @Override
    public void onPerkInactive(Player player) {
        InventoryUtil.removeItemWithInternalName(player, "netherite_bless");
    }


    @PlayerOnly
    @Override
    public void handlePlayerKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        if ("rage_pit".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName())) {
            return;
        }

        if (RandomUtil.hasSuccessfullyByChance(0.05)) {
            ItemStack item = myself.getInventory().getItem(FEET);
            if (item.getType().isAir() || item.getType().toString().contains("DIAMOND")) {
                return;
            }

            String internalName = ItemUtil.getInternalName(item);
            if (internalName == null || internalName.equalsIgnoreCase("lucky_diamond") || internalName.equalsIgnoreCase("shopItem")) {
                String name = "";
                Material newMat = switch (item.getType()) {
                    case DIAMOND_HELMET -> {
                        name = "钻石头盔";
                        yield Material.NETHERITE_HELMET;
                    }
                    case DIAMOND_CHESTPLATE -> {
                        name = "钻石胸甲";
                        yield Material.NETHERITE_CHESTPLATE;
                    }
                    case DIAMOND_LEGGINGS -> {
                        name = "钻石护腿";
                        yield Material.NETHERITE_LEGGINGS;
                    }
                    case DIAMOND_BOOTS -> {
                        name = "钻石靴子";
                        yield Material.NETHERITE_BOOTS;
                    }
                    default -> null;
                };
                if (newMat == null) {
                    return;
                }
                ItemStack itemStack = new ItemBuilder(newMat).internalName("netherite_bless").canDrop(false).deathDrop(true).canSaveToEnderChest(false).buildWithUnbreakable();
                myself.getInventory().setItem(FEET, itemStack);
                myself.sendMessage("§b§l下界祝福! §7您的 " + name + " 装备已升级为下届合金类型!");
            }
        }
    }
}
