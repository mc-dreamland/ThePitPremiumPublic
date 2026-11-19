package cn.charlotte.pit.enchantment.type.rare;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.IActionDisplayEnchant;
import cn.charlotte.pit.enchantment.param.item.BowOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.listener.IPlayerShootEntity;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@BowOnly
public class WitheredAndPiercingThroughTheHeartEnchant extends AbstractEnchantment implements IPlayerShootEntity, IActionDisplayEnchant {

    @Override
    public String getEnchantName() {
        return "枯萎穿心";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "withered_and_piercing_through_the_heart";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.RARE;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7第四次攻击对敌人施加禁锢(持续" + (enchantLevel * 0.5) + "秒)（禁锢：让敌人停止在原地)/s"
                + "并且自身获得" + (1 + enchantLevel) + "心额外生命值，" + enchantLevel + "秒的生命回复二";
    }

    @Override
    public void handleShootEntity(
            int enchantLevel,
            Player attacker,
            Entity target,
            double damage,
            AtomicDouble finalDamage,
            AtomicDouble boostDamage,
            AtomicBoolean cancel
    ) {
        if (!(target instanceof Player victim)) {
            return;
        }

        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(attacker.getUniqueId());
        if (profile.getBowHit() % 4 == 0) {
            List<MetadataValue> metadata = victim.getMetadata("lastThroughTheHeart");
            long lastTimeStamp = -1L;
            if (!metadata.isEmpty()) {
                lastTimeStamp = metadata.getFirst().asLong();
            }

            if (System.currentTimeMillis() - lastTimeStamp >= 10 * 1000L) {
                int durationTicks = (int) ((0.5 * enchantLevel) * 20);

                // 添加禁锢效果：高等级缓慢 + 跳跃负等级
                PlayerUtil.addEffect(victim, new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 10));
                PlayerUtil.addEffect(victim, new PotionEffect(PotionEffectType.JUMP_BOOST, durationTicks, -3));
                PlayerUtil.addEffect(attacker, new PotionEffect(PotionEffectType.REGENERATION, enchantLevel * 20, 1));
                victim.setMetadata("lastThroughTheHeart",
                        new FixedMetadataValue(ThePit.getInstance(), System.currentTimeMillis()));
            }

            attacker.playSound(attacker.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1f);
            victim.playSound(attacker.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1f);

            // 吸收之心（黄心）
            CraftPlayer craftAttacker = (CraftPlayer) attacker;
            float absorption = Math.min(((1f + enchantLevel) * 2) * 2, (1f + enchantLevel) * 2);
            craftAttacker.getHandle().setAbsorptionAmount(absorption);

            // 生命恢复 II
            PlayerUtil.addEffect(attacker, new PotionEffect(PotionEffectType.REGENERATION, enchantLevel * 20, 1));
        }
    }

    @Override
    public String getText(int level, Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        int hit;
        if (player.getInventory().getItemInMainHand().getType() == Material.BOW) {
            hit = profile.getBowHit();
        } else {
            hit = profile.getMeleeHit();
        }
        int require = 4;
        if (hit % require == 0) {
            return "&a&l✔";
        } else {
            return "&e&l" + (require - hit % require);
        }
    }
}
