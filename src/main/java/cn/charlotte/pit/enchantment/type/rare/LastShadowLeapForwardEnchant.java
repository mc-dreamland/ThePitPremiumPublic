package cn.charlotte.pit.enchantment.type.rare;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.IActionDisplayEnchant;
import cn.charlotte.pit.enchantment.param.item.WeaponOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.FuncsKt;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.time.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@AutoRegister
@WeaponOnly
public class LastShadowLeapForwardEnchant extends AbstractEnchantment implements Listener, IActionDisplayEnchant {

    private final Map<UUID, Cooldown> cooldownMap = new HashMap<>();

    @Override
    public String getEnchantName() {
        return "末影跃进";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "last_shadow_leap_forward";
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
        return "&7右键发动技能，向前突进一小段距离，之后自身获得&b速度" + enchantLevel + "&f（持续3秒）&7(" + getDuration(enchantLevel) + "秒冷却)";
    }

    private int getDuration(int enchantLevel) {
        return switch (enchantLevel) {
            case 1 -> 30;
            case 2 -> 28;
            default -> 20;
        };
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (PlayerUtil.shouldIgnoreEnchant(player)) {
            return;
        }

        IMythicItem mythicItem = FuncsKt.toMythicItem(player.getItemInHand());
        if (mythicItem == null) {
            return;
        }
        Integer level = mythicItem.getEnchantments().get(this);
        if (level == null) {
            return;
        }

        Cooldown cooldown = cooldownMap.get(player.getUniqueId());
        if (cooldown != null && !cooldown.hasExpired()) {
            return;
        }

        cooldownMap.put(player.getUniqueId(), new Cooldown(getDuration(level) * 1000L));

        Vector vector = player.getLocation().getDirection().clone().multiply(1.6);
        vector.setY(0.5);
        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> player.setVelocity(vector));
        PlayerUtil.addEffect(player, new PotionEffect(PotionEffectType.SPEED, 3 * 20, level - 1, true));
    }

    @Override
    public String getText(int level, Player player) {
        Cooldown cooldown = cooldownMap.getOrDefault(player.getUniqueId(), new Cooldown(0));
        if (cooldown.hasExpired()) {
            return "&a&l✔";
        }
        return "&c&l" + TimeUtil.millisToRoundedTime(cooldown.getRemaining()).replace(" ", "");
    }
}
