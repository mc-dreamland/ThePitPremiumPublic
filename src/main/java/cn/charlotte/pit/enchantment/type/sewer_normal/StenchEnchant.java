package cn.charlotte.pit.enchantment.type.sewer_normal;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.listener.IAttackEntity;
import cn.charlotte.pit.parm.listener.IPlayerDamaged;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.concurrent.atomic.AtomicBoolean;

@ArmorOnly
public class StenchEnchant extends AbstractEnchantment implements IAttackEntity, IPlayerDamaged {


    private static final Color RAGE_COLOR = Color.fromRGB(107, 0, 0);
    private static final Color DARK_GREEN_COLOR = Color.fromRGB(131, 195, 125);

    @Override
    public String getEnchantName() {
        return "恶臭";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "stenchEnchant";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.SEWER_NORMAL;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7如果攻击对象穿着的裤子不是暴怒之甲或者下水道之甲/s" +
                "&7则增加 &c+" + (enchantLevel * 5) + "%伤害" +
                "&7且受到伤害 &9-" + (enchantLevel * 3) + "%";
    }

    @Override
    @PlayerOnly
    public void handleAttackEntity(int enchantLevel, Player attacker, Entity target,
                                   double damage, AtomicDouble finalDamage,
                                   AtomicDouble boostDamage, AtomicBoolean cancel) {

        if (!(target instanceof Player targetPlayer)) {
            return;
        }

        if (isWearingSpecialLeggings(targetPlayer)) {
            double damageBonus = enchantLevel * 0.05;
            boostDamage.set(boostDamage.get() + damageBonus);
        }
    }

    @Override
    @PlayerOnly
    public void handlePlayerDamaged(int enchantLevel, Player myself, Entity attacker,
                                    double damage, AtomicDouble finalDamage,
                                    AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (isWearingSpecialLeggings((Player) attacker)) {
            double damageReduction = enchantLevel * 0.03;
            boostDamage.set(boostDamage.get() - damageReduction);
        }

    }

    private boolean isWearingSpecialLeggings(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack leggings = inventory.getLeggings();
        if (leggings == null || leggings.getType() != Material.LEATHER_LEGGINGS) {
            return true;
        }
        if (!(leggings.getItemMeta() instanceof LeatherArmorMeta meta)) {
            return true;
        }

        Color leggingsColor = meta.getColor();

        return !leggingsColor.equals(RAGE_COLOR) && !leggingsColor.equals(DARK_GREEN_COLOR);
    }
}