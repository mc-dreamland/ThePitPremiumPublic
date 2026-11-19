package cn.charlotte.pit.enchantment.type.rare;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.IActionDisplayEnchant;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.enchantment.param.item.WeaponOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.enchantment.type.ragerare.ThinkOfThePeopleEnchant;
import cn.charlotte.pit.item.type.mythic.MythicBowItem;
import cn.charlotte.pit.item.type.mythic.MythicSwordItem;
import cn.charlotte.pit.parm.listener.IAttackEntity;
import cn.charlotte.pit.parm.listener.IPlayerShootEntity;
import cn.charlotte.pit.parm.type.BowOnly;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/17 18:35
 */
@WeaponOnly
public class ComboStrikeEnchant extends AbstractEnchantment implements IAttackEntity, IPlayerShootEntity, IActionDisplayEnchant {

    private static final ThinkOfThePeopleEnchant thinkOfThePeople = new ThinkOfThePeopleEnchant();
    private final DecimalFormat numFormat = new DecimalFormat("0.0");

    public static double getDamage(int enchantLevel) {
        return switch (enchantLevel) {
            case 1 -> 1.0;
            case 2 -> 1.5;
            case 3 -> 2.0;
            default -> 0.0;
        };
    }

    @Override
    public String getEnchantName() {
        return "强力击: 闪电";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "lightning_strike";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.RARE;
    }

    @Override
    public Cooldown getCooldown() {
        return new Cooldown(3, TimeUnit.SECONDS);
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        int hits = (enchantLevel > 1) ? 4 : 5;
        return "&7每 &e" + hits + " &7次击中目标额外召唤一道闪电攻击你的敌人,/s&7造成 &c"
                + numFormat.format(getDamage(enchantLevel))
                + "❤ &b雷电&f(真实)&7伤害"
                + (enchantLevel >= 3 ? "&7,且闪电攻击目标/s&7每穿着一件&b钻石装备&7,闪电额外造成 &c1❤ &b雷电&f(真实)&7伤害" : "");
    }

    @Override
    @PlayerOnly
    public void handleAttackEntity(
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

        if (attacker.getInventory().getItemInMainHand().getType() != new MythicSwordItem().getItemDisplayMaterial()) {
            return;
        }

        for (Player nearbyPlayers : PlayerUtil.getNearbyPlayers(victim.getLocation(), 10.0)) {
            if (nearbyPlayers.getInventory().getLeggings() != null
                    && thinkOfThePeople.isItemHasEnchant(nearbyPlayers.getInventory().getLeggings())) {
                int level = thinkOfThePeople.getItemEnchantLevel(nearbyPlayers.getInventory().getLeggings());
                if (level > 1) {
                    boostDamage.getAndAdd(level * -0.1 + 0.1);
                }
                return;
            }
        }

        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(attacker.getUniqueId());
        if (profile.getMeleeHit() % ((enchantLevel > 1) ? 4 : 5) == 0) {
            finalDamage.set(finalDamage.get() + 2 * getDamage(enchantLevel));
            PlayerUtil.playThunderEffect(target.getLocation());

            PlayerProfile victimProfile = PlayerProfile.getPlayerProfileByUuid(victim.getUniqueId());
            CC.send(MessageType.MISC, attacker, "&b&l闪电! &7你的闪电击中了 " + victimProfile.getFormattedName() + " &7!");

            if (enchantLevel >= 3) {
                double extra = 0.0;
                for (var armorContent : victim.getInventory().getArmorContents()) {
                    if (armorContent != null && armorContent.getType().name().contains("DIAMOND")) {
                        extra++;
                    }
                }
                finalDamage.set(finalDamage.get() + extra * 2);
                if (profile.isDebug()) {
                    attacker.sendMessage("&f真实伤害|强力击：闪电|" + (2.0 + extra * 2));
                }
            }
        }
    }

    @Override
    @PlayerOnly
    @BowOnly
    public void handleShootEntity(
            int enchantLevel,
            Player attacker,
            Entity target,
            double damage,
            AtomicDouble finalDamage,
            AtomicDouble boostDamage,
            AtomicBoolean cancel
    ) {
        if (!(target instanceof Player victim)) return;

        if (attacker.getInventory().getItemInMainHand().getType() != new MythicBowItem().getItemDisplayMaterial()) {
            return;
        }

        for (Player nearbyPlayers : PlayerUtil.getNearbyPlayers(victim.getLocation(), 10.0)) {
            if (nearbyPlayers.getInventory().getLeggings() != null
                    && thinkOfThePeople.isItemHasEnchant(nearbyPlayers.getInventory().getLeggings())) {
                int level = thinkOfThePeople.getItemEnchantLevel(nearbyPlayers.getInventory().getLeggings());
                if (level > 1) {
                    boostDamage.getAndAdd(level * -0.1 + 0.1);
                }
                return;
            }
        }

        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(attacker.getUniqueId());
        if (profile.getMeleeHit() % ((enchantLevel > 1) ? 4 : 5) == 0) {
            finalDamage.set(finalDamage.get() + 2 * getDamage(enchantLevel));
            PlayerUtil.playThunderEffect(target.getLocation());

            PlayerProfile victimProfile = PlayerProfile.getPlayerProfileByUuid(victim.getUniqueId());
            CC.send(MessageType.MISC, attacker, "&b&l闪电! &7你的闪电击中了 " + victimProfile.getFormattedName() + " &7!");

            if (enchantLevel >= 3) {
                double extra = 0.0;
                for (var armorContent : victim.getInventory().getArmorContents()) {
                    if (armorContent != null && armorContent.getType().name().contains("DIAMOND")) {
                        extra++;
                    }
                }
                finalDamage.set(finalDamage.get() + extra * 2);
            }
        }
    }

    @Override
    public String getText(int level, Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        int hit = (player.getItemInHand() != null
                && player.getInventory().getItemInMainHand().getType() == Material.BOW)
                ? profile.getBowHit() : profile.getMeleeHit();

        int hitsRequired = (level > 1) ? 4 : 5;
        if (hit % hitsRequired == 0) {
            return "&a&l✔";
        } else {
            return "&e&l" + (hitsRequired - (hit % hitsRequired));
        }
    }
}
