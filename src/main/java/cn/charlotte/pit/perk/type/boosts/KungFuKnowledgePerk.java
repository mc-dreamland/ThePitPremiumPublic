package cn.charlotte.pit.perk.type.boosts;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.bot.PitBotManager;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitDamageEvent;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksBoostsEnum;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.util.PlayerUtil;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@AutoRegister
public class KungFuKnowledgePerk extends TypedPerk<PerksBoostsEnum> implements Listener {

    // Do not charge equipment-enchant combos or PvP hit statistics by attacking Bots.
    private final Map<UUID, Integer> botMeleeHits = new HashMap<>();

    /* ───────── 基础信息 ───────── */
    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "功夫知识";
    }

    @Override
    public Material getIcon() {
        return Material.BEEF;
    }

    @Override
    public double requireCoins() {
        return 10000.0;
    }

    @Override
    public double requireRenown(int level) {
        return 40.0;
    }

    @Override
    public int requirePrestige() {
        return 9;
    }

    @Override
    public int requireLevel() {
        return 100;
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
        return Arrays.asList(
                "&7武器不再造成伤害",
                "&7拳头将会附带高额伤害",
                "&7每4次攻击将会给予你 &b速度 II&7(0:05)"
        );
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    /* ───────── 激活 / 失活 ───────── */
    @Override
    public void onPerkActive(Player player) {
        // 无额外逻辑
    }

    @Override
    public void onPerkInactive(Player player) {
        botMeleeHits.remove(player.getUniqueId());
    }

    @Override
    public void handleQuit(int level, Player player) {
        botMeleeHits.remove(player.getUniqueId());
    }

    /* ───────── 事件监听 ───────── */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(PitDamageEvent event) {
        // 只关心玩家造成的伤害
        if (!(event.getAttacker() instanceof Player damager)) return;

        if (!PlayerUtil.hasEquipPerk(damager, this)) return;

        if ("spire".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName())) {
            return;
        }

        event.setDamage(attackDamage(damager));
    }

    /** Shared by PvP and the isolated Bot chain, before Bot hit caps/prestige scaling. */
    public static double attackDamage(Player player) {
        if (player.getInventory().getItemInMainHand().getType() != Material.AIR) return 0;
        return PlayerUtil.isCritical(player) ? 8.0 : 7.3;
    }

    /* ───────── 自定义近战连击回调 ───────── */
    @Override
    public void handleAttackEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {

        int hits = PitBotManager.isBot(target)
                ? botMeleeHits.merge(attacker.getUniqueId(), 1, (previous, one) -> (previous + one) % 4)
                : PlayerProfile.getPlayerProfileByUuid(attacker.getUniqueId()).getMeleeHit();

        // 每第 4 次近战命中给予速度 II（5 秒）
        if (hits % 4 == 0) {
            PlayerUtil.addEffect(attacker, new PotionEffect(PotionEffectType.SPEED, 20 * 5, 1));
        }
    }
}
