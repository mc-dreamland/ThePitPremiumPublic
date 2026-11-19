package cn.charlotte.pit.enchantment.type.ragerare;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.IActionDisplayEnchant;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.parm.listener.IPlayerKilledEntity;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.cooldown.Cooldown;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@ArmorOnly
public final class KillAngelsEnchant extends AbstractEnchantment implements IPlayerKilledEntity, IActionDisplayEnchant {

    @Override
    public String getEnchantName() {
        return "杀戮天使";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "kill_angels";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.RAGE_RARE;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        // 计算每层加成百分比
        double bonusPerLayer = enchantLevel / 2.0;
        return "&7每当你连杀 &c5 &7位目标增加 &e1 &7层增益,/s" +
                "&7每层增益你的&b经验&7和&6硬币&7奖励 &b+/s" + "&7" +
                (bonusPerLayer % 1 == 0 ? (int) bonusPerLayer : String.format("%.1f", bonusPerLayer)) +
                "% &7(上限&e300层&7)";
    }

    @Override
    public void handlePlayerKilled(int enchantLevel, Player player, Entity target, ResourceManager resourceManager) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        double streakKills = profile.getStreakKills();
        double bonusPercentage = Math.min((int) (streakKills / 5), 300) * (enchantLevel / 2.0);

        if (bonusPercentage > 0) {
            double multiplier = bonusPercentage / 100.0;
            resourceManager.addExpModifier(multiplier, "杀戮天使");
            resourceManager.addGoldModifier(multiplier, "杀戮天使");
        }
    }

    @Override
    public String getText(int enchantLevel, @NotNull Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());

        int layers = Math.min((int) (profile.getStreakKills() / 5), 300);

        double currentBonus = layers * (enchantLevel / 2.0);

        String bonusText = String.format("&e加成: %.1f%%", currentBonus);
        String layerText = "&e层数: " + layers + "/300";
        return CC.translate("&f&k!! " + bonusText + " &f| " + layerText + " &f|");
    }
}