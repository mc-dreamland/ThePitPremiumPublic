package cn.charlotte.pit.enchantment.type.dark_normal;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.parm.listener.IPlayerKilledEntity;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.List;

@ArmorOnly
public class GrimReaperEnchant extends AbstractEnchantment implements IPlayerKilledEntity {

    @Override
    public String getEnchantName() {
        return "死神";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 1;
    }

    @Override
    public String getNbtName() {
        return "grim_reaper_enchant";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.DARK_NORMAL;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7击杀玩家时释放冲击波,对周围8格内所有玩家造成 &c2❤ &7普通伤害"
                + "/s&c击杀获得的奖励 -80%";
    }

    @Override
    @PlayerOnly
    public void handlePlayerKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        // 如果目标是被死神附魔造成的溅射伤害击杀，跳过处理，防止递归触发
        if (target.hasMetadata("grim_reaper_damage")) {
            return;
        }

        resourceManager.addGoldModifier(0.2, "死神");
        resourceManager.addExpModifier(0.2, "死神");

        if (!(target instanceof Player targetPlayer)) {
            return;
        }

        List<Player> nearbyPlayers = PlayerUtil.getNearbyPlayers(myself.getLocation(), 8.0, 3.0, 8.0);

        for (Player player : nearbyPlayers) {
            if (player.equals(myself)) {
                player.setMetadata("grim_reaper_master", new FixedMetadataValue(ThePit.getInstance(), true));
                continue;
            }
            if (player.equals(targetPlayer)) continue;

            player.setMetadata("grim_reaper_damage", new FixedMetadataValue(ThePit.getInstance(), true));
            player.damage(4.0, myself);
        }

        Bukkit.getScheduler().runTaskLaterAsynchronously(ThePit.getInstance(), () -> {
            for (Player player : nearbyPlayers) {
                player.removeMetadata("grim_reaper_master", ThePit.getInstance());
                player.removeMetadata("grim_reaper_damage", ThePit.getInstance());
            }
        }, 2L);
    }
}
