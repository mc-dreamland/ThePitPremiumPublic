package cn.charlotte.pit.enchantment.type.rare;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.param.item.ArmorOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.parm.listener.IAttackEntity;
import cn.charlotte.pit.parm.listener.IPlayerBeKilledByEntity;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@ArmorOnly
public class UnrestrainedEnchant extends AbstractEnchantment implements IPlayerBeKilledByEntity, IAttackEntity {

    private final Map<UUID, AttackData> map = new HashMap<>();

    @Override
    public String getEnchantName() {
        return "无拘无束";
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return "unrestrained";
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
        return "&7当你被某名玩家杀死时，你下次复活/s时对该玩家伤害额外增加&c24%&7(最高叠加至6层)";
    }

    @Override
    public void handleAttackEntity(int enchantLevel, Player attacker, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        AttackData attackData = map.get(target.getUniqueId());
        if (attackData == null) {
            return;
        }
        boostDamage.addAndGet(attackData.amount * 0.24);
    }

    @Override
    public void handlePlayerBeKilledByEntity(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        AttackData data = map.computeIfAbsent(target.getUniqueId(), AttackData::new);
        data.amount = Math.min(6, data.amount + 1);
    }

    public static class AttackData {

        private final UUID uuid;
        private int amount;

        public AttackData(UUID uuid) {
            this.uuid = uuid;
        }

        public UUID getUuid() {
            return uuid;
        }

        public int getAmount() {
            return amount;
        }
    }
}
