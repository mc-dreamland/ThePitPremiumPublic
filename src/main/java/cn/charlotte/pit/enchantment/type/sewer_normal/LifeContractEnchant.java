package cn.charlotte.pit.enchantment.type.sewer_normal;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.IActionDisplayEnchant;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.listener.ITickTask;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.cooldown.Cooldown;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@ArmorOnly
public class LifeContractEnchant extends AbstractEnchantment implements IActionDisplayEnchant, ITickTask {

    private static final Map<UUID, Cooldown> cooldowns = new HashMap<>();

    @Override
    public String getEnchantName() {
        return "生命契约";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "life_contract";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.SEWER_RARE;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        int absorbHearts = 3 * enchantLevel;
        int cooldown = 60 - enchantLevel * 10;

        return "&7当血量低于 &c" + enchantLevel + "❤ &7时:/s" +
                "  &e+" + absorbHearts + "❤ 伤害吸收/s" +
                "&7冷却时间: &f" + cooldown + "秒";
    }

    @Override
    public String getText(int level, Player player) {
        UUID uuid = player.getUniqueId();
        Cooldown cooldown = cooldowns.get(uuid);

        if (cooldown != null && !cooldown.hasExpired()) {
            return "&c冷却中: &f" + this.getCooldownActionText(cooldowns.getOrDefault(player.getUniqueId(), new Cooldown(0)));
        }
        return "&a&l√";
    }

    @Override
    public void handle(int enchantLevel, Player player) {
        UUID uuid = player.getUniqueId();
        Cooldown cooldown = cooldowns.getOrDefault(uuid, new Cooldown(0));

        double threshold = enchantLevel * 2;

        if (cooldown.hasExpired() && player.getHealth() <= threshold) {
            double absorptionToAdd = 3 * enchantLevel * 2; // 3❤/级 * 2点/❤
            double currentAbsorption = player.getAbsorptionAmount();
            double newAbsorption = currentAbsorption + absorptionToAdd;

            if (newAbsorption > 18) {
                newAbsorption = 18;
            }

            PlayerUtil.setAbsorption(player, newAbsorption);

            int cooldownSeconds = 60 - enchantLevel * 10;
            cooldowns.put(uuid, new Cooldown(cooldownSeconds, TimeUnit.SECONDS));

            player.sendMessage(CC.translate("&a&l生命契约激活! &e+" + (absorptionToAdd / 2) + "❤ 伤害吸收"));
        }
    }

    @Override
    public int loopTick(int enchantLevel) {
        return 20; // 每秒检查一次
    }
}