package cn.charlotte.pit.enchantment.type.op;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.listener.ITickTask;
import cn.charlotte.pit.util.cooldown.Cooldown;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

public class RealManEnchant extends AbstractEnchantment implements ITickTask {

    public static boolean hasRealMan(Player player) {
        long expiresAt = player.getMetadata("real_man")
                .stream()
                .findFirst()
                .map(MetadataValue::asLong)
                .orElse(Long.MAX_VALUE);
        return expiresAt >= System.currentTimeMillis();
    }

    @Override
    public String getEnchantName() {
        return "真男人";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 1;
    }

    @Override
    public String getNbtName() {
        return "real_man";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.OP;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7当你的7格范围内全部人 &cbuff天赋连杀失效/s"
                + "           &7出范围外3秒效果消失";
    }

    @Override
    public void handle(int enchantLevel, Player player) {
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 7.0, 7.0, 7.0)) {
            if (entity instanceof Player target) {
                target.setMetadata(
                        "real_man",
                        new FixedMetadataValue(ThePit.getInstance(), System.currentTimeMillis() + 3_000L)
                );
            }
        }
    }

    @Override
    public int loopTick(int enchantLevel) {
        return 20;
    }
}
