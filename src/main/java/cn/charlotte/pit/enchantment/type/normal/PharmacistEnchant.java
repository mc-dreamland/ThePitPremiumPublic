package cn.charlotte.pit.enchantment.type.normal;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.IActionDisplayEnchant;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.listener.IPlayerDamaged;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

@ArmorOnly
public class PharmacistEnchant extends AbstractEnchantment implements IPlayerDamaged, IActionDisplayEnchant { // 实现新接口
    private final Random random = new Random();

    @Override
    public String getEnchantName() {
        return "药剂师";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "pharmacist";
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
        if (enchantLevel == 1) {
            return "每受5次伤害随机获得：&b速度 I &f(1秒)";
        } else if (enchantLevel == 2) {
            return "每受5次伤害随机获得：&b速度 II &f(4秒) 或 &c生命恢复 I &f(4秒)";
        } else if (enchantLevel >= 3) {
            return "每受5次伤害随机获得：&b速度 II &f(9秒) 或 &c生命恢复 II &f(9秒) 或 &3抗性 I &f(3秒)";
        }
        return "";
    }

    @Override
    @PlayerOnly
    public void handlePlayerDamaged(int enchantLevel, Player myself, Entity attacker, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(myself.getUniqueId());
        if (profile.getHit() % 5 == 0) {
            switch (enchantLevel) {
                case 1:
                    PlayerUtil.addEffect(myself, new PotionEffect(
                            PotionEffectType.SPEED,
                            20,
                            0,
                            true,
                            true
                    ));
                    break;

                case 2:
                    if (random.nextBoolean()) {
                        PlayerUtil.addEffect(myself, new PotionEffect(
                                PotionEffectType.SPEED,
                                80,
                                1,
                                true,
                                true
                        ));
                    } else {
                        PlayerUtil.addEffect(myself, new PotionEffect(
                                PotionEffectType.REGENERATION,
                                80,
                                0,
                                true,
                                true
                        ));
                    }
                    break;

                case 3:
                    int choice = random.nextInt(3);
                    switch (choice) {
                        case 0:
                            PlayerUtil.addEffect(myself, new PotionEffect(
                                    PotionEffectType.SPEED,
                                    180,
                                    1,
                                    true,
                                    true
                            ));
                            break;
                        case 1:
                            PlayerUtil.addEffect(myself, new PotionEffect(
                                    PotionEffectType.REGENERATION,
                                    180,
                                    1,
                                    true,
                                    true
                            ));
                            break;
                        case 2:
                            PlayerUtil.addEffect(myself, new PotionEffect(
                                    PotionEffectType.RESISTANCE,
                                    60,
                                    0,
                                    true,
                                    true
                            ));
                            break;
                    }
                    break;
            }
        }
    }

    @Override
    public String getText(int level, Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        int hits = profile.getHit();
        int needed = 5 - (hits % 5);
        return (needed == 5) ? "&a&l✔" : "&e&l" + needed;
    }
}