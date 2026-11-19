package cn.charlotte.pit.enchantment.type.ragerare;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.listener.IPlayerDamaged;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@ArmorOnly
public class MicroscopicAntiGravityEnchant extends AbstractEnchantment implements IPlayerDamaged {

    private static final Map<Player, PlayerRecord> PLAYER_RECORDS = new WeakHashMap<>();

    @Override
    public String getEnchantName() {
        return "微观反重力";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "microscopic_anti_gravity";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.RAGE_RARE;
    }

    @Nullable
    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7若你在 &b3s &7内连续在空中受到 &e2 &7次攻击,则给予&c生命恢复 " + getEffect(enchantLevel + 2) + " &f(0:02) &7并且获得 &b速度 " + getEffect(enchantLevel) + " &7效果/s&7(最高持续 " + enchantLevel + "秒)。";
    }

    private String getEffect(int enchantLevel) {
        return switch (enchantLevel) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> "未知";
        };
    }

    @Override
    @PlayerOnly
    public void handlePlayerDamaged(int enchantLevel, Player myself, Entity attacker, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        // 获取当前游戏刻（防止同一tick内重复处理）
        int currentTick = Bukkit.getServer().getCurrentTick();

        PlayerRecord record = PLAYER_RECORDS.computeIfAbsent(myself, k -> new PlayerRecord());

        if (record.lastProcessedTick == currentTick) {
            return;
        }
        record.lastProcessedTick = currentTick;

        if (myself.isOnGround()) {
            record.reset();
            return;
        }

        long currentTime = System.currentTimeMillis();

        if (currentTime - record.lastAirHitTime <= 3000) {
            record.consecutiveHits++;
        } else {
            record.consecutiveHits = 1;
        }

        record.lastAirHitTime = currentTime;

        if (record.consecutiveHits >= 2) {
            int regenLevel = Math.min(enchantLevel + 2, 5) - 1;
            int speedLevel = enchantLevel - 1;
            PlayerUtil.addEffect(myself, new PotionEffect(
                    PotionEffectType.REGENERATION,
                    40,
                    regenLevel,
                    true,
                    true
            ));
            PlayerUtil.addEffect(myself, new PotionEffect(
                    PotionEffectType.SPEED,
                    enchantLevel * 20,
                    speedLevel,
                    true,
                    true
            ));
            record.consecutiveHits = 0;
        }
    }

    private static class PlayerRecord {
        long lastAirHitTime;
        int consecutiveHits;
        int lastProcessedTick;

        PlayerRecord() {
            reset();
        }

        void reset() {
            lastAirHitTime = 0;
            consecutiveHits = 0;
            lastProcessedTick = -1;
        }
    }
}