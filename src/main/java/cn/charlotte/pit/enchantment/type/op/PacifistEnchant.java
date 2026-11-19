package cn.charlotte.pit.enchantment.type.op;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.parm.listener.IPlayerDamaged;
import cn.charlotte.pit.parm.listener.IPlayerRespawn;
import cn.charlotte.pit.parm.listener.ITickTask;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

public class PacifistEnchant extends AbstractEnchantment implements IPlayerDamaged, ITickTask, IPlayerRespawn {

    @Override
    public String getEnchantName() {
        return "和平主义者";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 1;
    }

    @Override
    public String getNbtName() {
        return "pacifistEnchant";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.OP;
    }

    @Nullable
    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7OP的力量!/s&dPlayer:天坑神秘人";
    }

    @Override
    public void handlePlayerDamaged(int enchantLevel, Player myself, Entity attacker, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        finalDamage.set(0);
        PlayerUtil.addEffect(myself, new PotionEffect(PotionEffectType.REGENERATION, 20 * 5, 9));
    }

    @Override
    public void handle(int enchantLevel, Player player) {
        player.setAllowFlight(true);
    }

    @Override
    public int loopTick(int enchantLevel) {
        return 20;
    }

    @Override
    public void handleRespawn(int enchantLevel, Player myself) {
        Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
            myself.getInventory().addItem(PitItems.SMALL_MYTHIC_REPAIR_KIT.toItemStack());
        }, 10L);
    }
}
