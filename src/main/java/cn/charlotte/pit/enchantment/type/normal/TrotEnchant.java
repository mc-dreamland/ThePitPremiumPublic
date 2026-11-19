package cn.charlotte.pit.enchantment.type.normal;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@ArmorOnly
public class TrotEnchant extends AbstractEnchantment {
    //    private static final TrotEnchant trotEnchant = new TrotEnchant();
    public static final UUID ATTRIBUTE_MODIFIER_UUID = UUID.nameUUIDFromBytes("TrotEnchant".getBytes(StandardCharsets.UTF_8));

    @Override
    public String getEnchantName() {
        return "疾跑";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "Trot";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.NORMAL;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7穿戴时行走速度提升 &b" + (enchantLevel == 3 ? "20%" : (enchantLevel == 2 ? "10%" : "5%"));
    }

    @Override
    public void onBuild(ItemBuilder builder, int level) {
        double value = switch (level) {
            case 1 -> 0.005;
            case 2 -> 0.01;
            case 3 -> 0.02;
            default -> 0;
        };
        AttributeModifier modifier = new AttributeModifier(ATTRIBUTE_MODIFIER_UUID,
                "TrotEnchant",
                value,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.LEGS);
        builder.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, modifier);
    }

    //    @Override
//    public void handleUpdateLocation(Player player, Location location, Location locationtoo) {
//        ItemStack leggings = player.getInventory().getLeggings();
//        if (leggings != null && "mythic_leggings".equals(ItemUtil.getInternalName(leggings))
//                && trotEnchant.isItemHasEnchant(leggings) && !PlayerUtil.isVenom(player)) {
//            int level = trotEnchant.getItemEnchantLevel(leggings);
//            if (level == 1) {
//                player.setWalkSpeed(0.21f);
//            } else if (level == 2) {
//                player.setWalkSpeed(0.22f);
//            } else if (level == 3) {
//                player.setWalkSpeed(0.24f);
//            }
//        } else {
//            if (player.getWalkSpeed() != 0.2f) {
//                player.setWalkSpeed(0.2f);
//            }
//        }
//    }

//    @Override
//    public void handleUpdateRotation(Player player, Location location, Location location1) {
//    }
}
