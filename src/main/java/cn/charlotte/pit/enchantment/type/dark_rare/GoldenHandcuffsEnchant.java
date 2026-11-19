package cn.charlotte.pit.enchantment.type.dark_rare;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.parm.listener.IAttackEntity;
import cn.charlotte.pit.parm.listener.IPlayerKilledEntity;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: Misoryan
 * @Created_In: 2021/3/15 20:43
 */
@ArmorOnly
public class GoldenHandcuffsEnchant extends AbstractEnchantment implements IPlayerKilledEntity, IAttackEntity {
    @Override
    public String getEnchantName() {
        return "金手铐";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 1;
    }

    @Override
    public String getNbtName() {
        return "golden_handcuffs_enchant";
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
        return "&7每当攻击的对方拥有 200 赏金时 +&c1%伤害/s&7同时击杀对方后获得的赏金数量 &6+400% &7,但获得的一半赏金将于房间内全部玩家平分.";
    }

    @PlayerOnly
    @Override
    public void handlePlayerKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        Player Bekiller = (Player) target;
        PlayerProfile profile2 = PlayerProfile.getPlayerProfileByUuid(Bekiller.getUniqueId());
        if (profile2.getBounty() > 0.0) {
            resourceManager.addGoldModifier(3.0, "金手铐");
            double Bounty;
            Bounty = profile2.getBounty() * 2.0 / Bukkit.getOnlinePlayers().size() - 1;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().equals(myself.getName())) {
                    continue;
                }
                PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
                profile.setCoins(profile.getCoins() + Bounty);
                profile.grindCoins(Bounty);
                player.sendMessage(CC.translate("&6&l金手铐 &7你获得了 &e" + Bounty + "g &7赏金"));
            }
        }
    }

    @Override
    public void handleAttackEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(target.getUniqueId());
        if (profile != null) {
            boostDamage.getAndAdd(0.01 * (Math.ceil((double) profile.getBounty() / 200)));
        }
    }
}
