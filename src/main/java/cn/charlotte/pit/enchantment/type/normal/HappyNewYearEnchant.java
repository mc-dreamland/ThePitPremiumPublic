package cn.charlotte.pit.enchantment.type.normal;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.IActionDisplayEnchant;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.listener.IPlayerDamaged;
import cn.charlotte.pit.util.chat.RomanUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@ArmorOnly
public class HappyNewYearEnchant extends AbstractEnchantment implements IPlayerDamaged, IActionDisplayEnchant {

    private final Map<UUID, Cooldown> cooldown = new HashMap<>();

    @Override
    public String getEnchantName() {
        return "新年快乐";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "happy_new_year";
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
        int[] heal = getHeal(enchantLevel);
        return "&7受到攻击时获得 &f生命恢复 "
                + RomanUtil.convert(heal[0] + 1)
                + " &c"
                + heal[1]
                + "秒&7 (20秒冷却)";
    }

    private int[] getHeal(int enchantLevel) {
        return switch (enchantLevel) {
            case 1 -> new int[]{0, 4};
            case 2 -> new int[]{0, 8};
            case 3 -> new int[]{1, 5};
            default -> new int[]{0, 0};
        };
    }

    @Override
    public void handlePlayerDamaged(
            int enchantLevel,
            Player myself,
            Entity attacker,
            double damage,
            AtomicDouble finalDamage,
            AtomicDouble boostDamage,
            AtomicBoolean cancel
    ) {
        cooldown.putIfAbsent(myself.getUniqueId(), new Cooldown(0));
        if (cooldown.get(myself.getUniqueId()).hasExpired()) {
            cooldown.put(myself.getUniqueId(), new Cooldown(20L, TimeUnit.SECONDS));
            int[] heal = getHeal(enchantLevel);
            myself.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION,
                    heal[1] * 20,
                    heal[0],
                    true
            ));
        }
    }

    @Override
    public String getText(int level, Player player) {
        return getCooldownActionText(cooldown.getOrDefault(player.getUniqueId(), new Cooldown(0)));
    }
}
