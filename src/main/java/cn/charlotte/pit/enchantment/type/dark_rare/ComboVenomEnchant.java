package cn.charlotte.pit.enchantment.type.dark_rare;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.buff.impl.VenomDeBuff;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.IActionDisplayEnchant;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.parm.listener.IAttackEntity;
import cn.charlotte.pit.parm.listener.IPlayerShootEntity;
import cn.charlotte.pit.parm.type.BowOnly;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: Misoryan
 * @Created_In: 2021/3/15 20:44
 */
@ArmorOnly
@AutoRegister
public class ComboVenomEnchant extends AbstractEnchantment implements IAttackEntity, IPlayerShootEntity, IActionDisplayEnchant, Listener {
    @Override
    public String getEnchantName() {
        return "强力击: 毒液";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 1;
    }

    @Override
    public String getNbtName() {
        return "combo_venom_enchant";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.DARK_RARE;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7每 &e4 &7次攻击对目标玩家&c&7施加 &c沉默 &f(00:12) &7与 &c阴影 &f(00:12) 状态," +
                "/s&7对自身施加 &c沉默 &f(00:24) &7与 &c阴影 &f(00:24) 状态,"
                + "/s&7状态 &c沉默 &7: 装备的所有神话物品失效"
                + "/s&7状态 &c阴影 &7: 攻击/受到攻击时,装备/使用的神话物品失效";
    }

    @Override
    @PlayerOnly
    public void handleAttackEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (PlayerProfile.getPlayerProfileByUuid(attacker.getUniqueId()).getMeleeHit() % 4 == 0) {
            Player targetPlayer = (Player) target;
//            new VenomDeBuff().setBuff(attacker, 1000L * 12, 1, true);
//            new VenomDeBuff().setBuff(attacker, 1000L * 24, 1, true);
            targetPlayer.setMetadata("combo_venom", new FixedMetadataValue(ThePit.getInstance(), System.currentTimeMillis() + 12 * 1000L));
            PotionEffect potionEffect = new PotionEffect(PotionEffectType.POISON, 12 * 20, 0);
            PlayerUtil.addEffect(targetPlayer, potionEffect);
            attacker.setMetadata("combo_venom", new FixedMetadataValue(ThePit.getInstance(), System.currentTimeMillis() + 24 * 1000L));
            potionEffect = new PotionEffect(PotionEffectType.POISON, 24 * 20, 0);
            PlayerUtil.addEffect(attacker, potionEffect);
        }
    }

    @Override
    @PlayerOnly
    @BowOnly
    public void handleShootEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (PlayerProfile.getPlayerProfileByUuid(attacker.getUniqueId()).getBowHit() % 4 == 0) {
            Player targetPlayer = (Player) target;
            targetPlayer.setMetadata("combo_venom", new FixedMetadataValue(ThePit.getInstance(), System.currentTimeMillis() + 12 * 1000L));
            PotionEffect potionEffect = new PotionEffect(PotionEffectType.POISON, 12 * 20, 0);
            PlayerUtil.addEffect(targetPlayer, potionEffect);
//            new VenomDeBuff().setBuff(targetPlayer, 1000L * 12, 1, true);
//            new VenomDeBuff().setBuff(attacker, 1000L * 24, 1, true);
            attacker.setMetadata("combo_venom", new FixedMetadataValue(ThePit.getInstance(), System.currentTimeMillis() + 24 * 1000L));
            potionEffect = new PotionEffect(PotionEffectType.POISON, 24 * 20, 0);
            PlayerUtil.addEffect(attacker, potionEffect);
        }
    }

    @Override
    public String getText(int level, Player player) {
        int hit = (player.getItemInHand() != null && player.getItemInHand().getType() == Material.BOW ? PlayerProfile.getPlayerProfileByUuid(player.getUniqueId()).getBowHit() : PlayerProfile.getPlayerProfileByUuid(player.getUniqueId()).getMeleeHit());
        return (hit % 4 == 0 ? "&a&l✔" : "&e&l" + (4 - hit % 4));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPoisonDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.POISON) {
            event.setCancelled(true);
        }
    }
}
