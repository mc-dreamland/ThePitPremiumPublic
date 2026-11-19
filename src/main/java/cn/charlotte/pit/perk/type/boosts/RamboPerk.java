package cn.charlotte.pit.perk.type.boosts;

import cn.charlotte.pit.perk.BotKillEffect;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksBoostsEnum;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.util.PlayerUtil;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/2/1 16:38
 */
@BotKillEffect(BotKillEffect.Kind.KILL_EFFECT)
public class RamboPerk extends TypedPerk<PerksBoostsEnum> {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "猛汉";
    }

    @Override
    public Material getIcon() {
        return Material.STICK;
    }

    @Override
    public double requireCoins() {
        return 6000;
    }

    @Override
    public double requireRenown(int level) {
        return 15;
    }

    @Override
    public int requirePrestige() {
        return 3;
    }

    @Override
    public int requireLevel() {
        return 70;
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
    public List<String> getDescription(Player player) {
        return Arrays.asList("&7携带时击杀&c无法获得治疗道具&7但恢复全部生命,", "&7且携带期间最大生命值 &c-2❤ &7.", "&c与天赋吸血鬼携带时,吸血鬼天赋失效.");
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public void onPerkActive(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        profile.getExtraMaxHealth().put(getInternalPerkName(), -4.0);
        player.setMaxHealth(profile.getMaxHealth());
    }

    @Override
    public void onPerkInactive(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        profile.getExtraMaxHealth().remove(getInternalPerkName());
        player.setMaxHealth(profile.getMaxHealth());

    }

    @Override
    public void handlePlayerKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        if ("rage_pit".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName())) {
            return;
        }
        myself.setHealth(myself.getMaxHealth());
        PlayerUtil.addEffect(myself, new PotionEffect(
                PotionEffectType.INSTANT_HEALTH, // 使用再生效果而不是INSTANT_HEALTH
                1,
                10,
                false,
                true
        ));
    }
}
