package cn.charlotte.pit.perk.type.bundles.beastmode;

import cn.charlotte.pit.event.PitStreakKillChangeEvent;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.item.listenerType.IPlayerInteract;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.perk.*;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@AutoRegister
public class AAABeefStreak extends TypedPerk<PerksBundlesEnum> implements Listener, Streakable, IPlayerInteract {
    public static final Map<UUID, Cooldown> beefEffectMap = new HashMap<>();

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "AAA级牛排";
    }

    @Override
    public Material getIcon() {
        return Material.MUTTON;
    }

    @Override
    public double requireCoins() {
        return 4000;
    }

    @Override
    public double requireRenown(int level) {
        return 0;
    }

    @Override
    public int requirePrestige() {
        return 0;
    }

    @Override
    public int requireLevel() {
        return 30;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> list = new ArrayList<>();
        list.add("&7此天赋每 &c7 连杀 &7触发一次.");
        list.add(" ");
        list.add("&7触发时:");
        list.add("  &a▶ &7给予一份 &eAAA级牛排");
        list.add("  &a▶ &7食用后给予");
        list.add("  &a▶ &7增加 &c10% 伤害加成&f(0:10)");
        list.add("  &a▶ &b速度I&f(0:10)");
        list.add("  &a▶ &3抗性I&f(0:10)");
        return list;
    }

    @Override
    public int getMaxLevel() {
        return 0;
    }

    @Override
    public PerkType getPerkType() {
        return PerkType.KILL_STREAK;
    }

    @Override
    public PerkGroup getPerkGroup() {
        return PerkGroup.KILL_STREAK;
    }


    @Override
    public void onPerkActive(Player player) {
    }

    @Override
    public void onPerkInactive(Player player) {
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onStreak(PitStreakKillChangeEvent event) {
        Player myself = Bukkit.getPlayer(event.getPlayerProfile().getPlayerUUID());
        if (myself == null || !myself.isOnline()) {
            return;
        }
        if (!PlayerUtil.hasEquipPerk(myself, this)) {
            return;
        }
        int streak = 7;
        if (Math.floor(event.getFrom()) % streak != 0 && Math.floor(event.getTo()) % streak == 0) {
            myself.getInventory().addItem(PitItems.BEEF.toItemStack());
        }
    }


    @Override
    public int getActiveStreak() {
        return 7;
    }

    @Override
    public void handleAttackEntity(int enchantLevel, Player attacker, Entity target, double damage,
                                   AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        Cooldown cooldown = beefEffectMap.get(attacker.getUniqueId());
        if (cooldown != null) {
            if (cooldown.hasExpired()) {
                beefEffectMap.remove(attacker.getUniqueId());
            } else {
                boostDamage.getAndAdd(0.1);
            }
        }
    }

    @Override
    public void handlePlayerInteract(Player player, ItemStack itemStack, EquipmentSlot hand, PlayerInteractEvent event) {
        Cooldown cooldown = beefEffectMap.get(player.getUniqueId());
        if(cooldown != null && !cooldown.hasExpired()){
            CC.send(player,"&c请你先等待牛排冷却结束再食用!");
            return;
        }
        PlayerUtil.takeOneItem(player, hand);
        PlayerUtil.addEffect(player, new PotionEffect(PotionEffectType.SPEED, 200, 0));
        PlayerUtil.addEffect(player, new PotionEffect(PotionEffectType.RESISTANCE, 200, 0));
        beefEffectMap.put(player.getUniqueId(), new Cooldown(4, TimeUnit.SECONDS));
    }
}