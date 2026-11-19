package cn.charlotte.pit.enchantment.type.sewer_normal;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.parm.listener.IAttackEntity;
import cn.charlotte.pit.parm.listener.IPlayerKilledEntity;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.item.ItemUtil;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.concurrent.atomic.AtomicBoolean;

@ArmorOnly
public class MaliciousEnchant extends AbstractEnchantment implements IAttackEntity, IPlayerKilledEntity {
    @Override
    public String getEnchantName() {
        return "恶毒";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "malicious";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.SEWER_RARE;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        String Message = "";
        if (enchantLevel >= 3) {
            Message = "&7对方死后扣除对方 1 根&3时髦的羽毛";
        }
        return "&7如果对方身上有 &3时髦的羽毛 &7时/s" +
                "&7伤害增加 &c" + (5 + 5 * enchantLevel) + "%/s" + Message;
    }

    @Override
    public void handleAttackEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (!(target instanceof Player player)) return;

        if (countFeathers(player.getInventory()) > 0) {
            boostDamage.getAndAdd(0.05 * enchantLevel + 0.05);
        }
    }

    @Override
    @PlayerOnly
    public void handlePlayerKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        if (!(target instanceof Player victim)) return;
        if (enchantLevel == 3) {
            deductFeathers(victim);
        }
    }

    private int countFeathers(PlayerInventory inventory) {
        int total = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR &&
                    "funky_feather".equals(ItemUtil.getInternalName(item))) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private void deductFeathers(Player victim) {

        int remaining = 1;

        for (int i = 0; i < 36 && remaining > 0; i++) {
            ItemStack item = victim.getInventory().getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            String internalName = ItemUtil.getInternalName(item);
            if (!"funky_feather".equals(internalName)) continue;

            int currentAmount = item.getAmount();
            int deductAmount = Math.min(remaining, currentAmount);

            if (deductAmount >= currentAmount) {
                victim.getInventory().setItem(i, new ItemStack(Material.AIR));
                remaining -= currentAmount;
            } else {
                item.setAmount(currentAmount - deductAmount);
                victim.getInventory().setItem(i, item);
                remaining -= deductAmount;
            }
        }

        int deducted = 1 - remaining;
        if (deducted > 0) {
            victim.sendMessage(CC.translate("&2恶毒 &7你的 &3时髦的羽毛 &7因为&2恶毒&7所以被扣除了 &b1根"));
        }
    }
}