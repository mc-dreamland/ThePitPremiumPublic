package cn.charlotte.pit.enchantment.type.rare;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.IActionDisplayEnchant;
import cn.charlotte.pit.enchantment.param.item.BowOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.chat.RomanUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.time.TimeUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Author: EmptyIrony
 * @Date: 2021/3/7 23:42
 */
@AutoRegister
@BowOnly
public class MegaLongBowEnchant extends AbstractEnchantment implements Listener, IActionDisplayEnchant {

    private static final HashMap<UUID, Cooldown> cooldown = new HashMap<>();

    @Override
    public String getEnchantName() {
        return "巨型长弓";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "mega_long_bow_enchant";
    }

    @Override
    public EnchantmentRarity getRarity() {
        return EnchantmentRarity.RARE;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return "&7射箭时无需蓄力即可让箭矢以最大蓄力状态射出,"
                + "/s&7同时为自身施加 &a跳跃提升 " + RomanUtil.convert(enchantLevel + 1) + " &f(00:02)"
                + "/s&7此附魔每秒只能触发一次.";
    }


    @EventHandler
    public void onInteract(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getForce() >= 2.8) return;
        if (PlayerUtil.isVenom(player) || PlayerUtil.isEquippingSomber(player)) return;
        org.bukkit.inventory.ItemStack itemInHand = event.getBow();
        if (itemInHand == null) return;
        final int level = this.getItemEnchantLevel(itemInHand);
        if (level == -1) {
            return;
        }
        if (player.isSneaking() && Utils.getEnchantLevel(player.getInventory().getLeggings(), "ender_bow_enchant") < 0) {
            return;
        }
        if (itemInHand.getType() == Material.BOW) {
            if (cooldown.getOrDefault(player.getUniqueId(), new Cooldown(0)).hasExpired()) {
                cooldown.put(player.getUniqueId(), new Cooldown(1, TimeUnit.SECONDS));
                PlayerUtil.addEffect(player, new PotionEffect(PotionEffectType.JUMP_BOOST, 2 * 20, level));
                event.setCancelled(true);
                final ServerPlayer ePlayer = ((CraftPlayer) player).getHandle();
                final ItemStack itemStack = CraftItemStack.asNMSCopy(itemInHand);
                final BowItem bow = (BowItem) itemStack.getItem();
                bow.releaseUsing(itemStack, ePlayer.level(), ePlayer, 0);
                player.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.ARROW, 1));
            }
        }
    }

    @Override
    public String getText(int level, Player player) {
        return cooldown.getOrDefault(player.getUniqueId(), new Cooldown(0)).hasExpired() ? "&a&l✔" : "&c&l" + TimeUtil.millisToRoundedTime(cooldown.get(player.getUniqueId()).getRemaining()).replace(" ", "");
    }
}
