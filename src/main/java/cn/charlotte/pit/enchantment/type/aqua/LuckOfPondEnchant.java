package cn.charlotte.pit.enchantment.type.aqua;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.FuncsKt;
import cn.charlotte.pit.util.cooldown.Cooldown;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

@ArmorOnly
@AutoRegister
public class LuckOfPondEnchant extends AbstractEnchantment implements Listener {

    @EventHandler
    private void onFish(PlayerFishEvent event) {
        IMythicItem mythicItem = FuncsKt.toMythicItem(event.getPlayer().getInventory().getLeggings());
        if (mythicItem != null && mythicItem.getEnchantments().keySet().stream().anyMatch(enchant -> "luck_of_pond".equals(enchant.getNbtName()))) {
            int chance = event.getHook().getMinWaitTime();
            event.getHook().setMaxLureTime(Math.min(chance, 1));
        }
    }

    @Override
    public String getEnchantName() {
        return "幸运之池";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 1;
    }

    @Override
    public String getNbtName() {
        return "luck_of_pond";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.FISH_NORMAL;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7钓鱼时鱼咬钩的速度提升 &a10%";
    }
}
