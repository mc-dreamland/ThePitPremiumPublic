package cn.charlotte.pit.perk.type.boosts;

import cn.charlotte.pit.perk.BotKillEffect;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.perk.*;
import cn.charlotte.pit.util.PlayerUtil;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@BotKillEffect(BotKillEffect.Kind.ASSIST_REWARD)
public class AssistantToTheStreakerPerk extends TypedPerk<PerksBoostsEnum> {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "连杀助理";
    }

    @Override
    public Material getIcon() {
        return Material.BIRCH_FENCE;
    }

    @Override
    public double requireCoins() {
        return 8000;
    }

    @Override
    public double requireRenown(int level) {
        return 15;
    }

    @Override
    public int requirePrestige() {
        return 5;
    }

    @Override
    public int requireLevel() {
        return 50;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7助攻时按照造成的伤害比例,获得连杀数.");
        if (PlayerUtil.isPlayerUnlockedPerk(player, PerksPrestigeEnum.PROMOTION)) {
            lines.add("&7助攻获得的&b经验&7以及&e金币 &f+ 30%");
            lines.add("&7每造成&c4次击杀&7则增加&e1连杀");
            lines.add("&7死亡时若已激活一个超级连杀且快捷栏里拥有一根羽毛,");
            lines.add("&7则有65%概率使羽毛不因此损失.");
        }
        return lines;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public PerkType getPerkType() {
        return PerkType.PERK;
    }

    @Override
    public PerkGroup getPerkGroup() {
        return PerkGroup.BOOST;
    }

    @Override
    public void onPerkActive(Player player) {

    }

    @Override
    public void onPerkInactive(Player player) {

    }

    @Override
    @PlayerOnly
    public void handlePlayerAssist(int enchantLevel, Player myself, Entity target, double damage, double finalDamage, ResourceManager resourceManager) {
        if (!PlayerUtil.isPlayerUnlockedPerk(myself, PerksPrestigeEnum.PROMOTION)) return;
        resourceManager.getExpModifiers().put("超进化", 1.3);
        resourceManager.getGoldModifiers().put("超进化", 1.3);
    }

    @PlayerOnly
    @Override
    public void handlePlayerKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        if (!PlayerUtil.isPlayerUnlockedPerk(myself, PerksPrestigeEnum.PROMOTION)) return;
        PlayerProfile profile = Util.getPitProfile(myself);
        if (profile.getKills() % 4 == 0) profile.setStreakKills(profile.getStreakKills() + 1);
    }
}
