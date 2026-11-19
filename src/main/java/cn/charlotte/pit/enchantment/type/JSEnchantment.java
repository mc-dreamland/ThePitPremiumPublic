package cn.charlotte.pit.enchantment.type;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.parm.listener.IAttackEntity;
import cn.charlotte.pit.parm.listener.IPlayerAssist;
import cn.charlotte.pit.parm.listener.IPlayerBeKilledByEntity;
import cn.charlotte.pit.parm.listener.IPlayerDamaged;
import cn.charlotte.pit.parm.listener.IPlayerKilledEntity;
import cn.charlotte.pit.parm.listener.IPlayerShootEntity;
import cn.charlotte.pit.util.cooldown.Cooldown;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import javax.script.ScriptEngine;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class JSEnchantment extends AbstractEnchantment implements
        IAttackEntity,
        IPlayerDamaged,
        IPlayerKilledEntity,
        IPlayerShootEntity,
        IPlayerAssist,
        IPlayerBeKilledByEntity {

    @FunctionalInterface
    public interface CombatHandler {
        void handle(int enchantLevel, Player player, Entity target, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel);
    }

    @FunctionalInterface
    public interface ResourceHandler {
        void handle(int enchantLevel, Player player, Entity target, ResourceManager resourceManager);
    }

    @FunctionalInterface
    public interface AssistHandler {
        void handle(int enchantLevel, Player player, Entity target, double damage, double finalDamage, ResourceManager resourceManager);
    }

    private final ScriptEngine engine;
    private final String enchantName0;
    private final String internalName0;
    private final EnchantmentRarity rarity0;
    private final List<String> desc;
    private final CombatHandler attackFunc;
    private final ResourceHandler playerKilledFunc;
    private final CombatHandler shootFunc;
    private final AssistHandler playerAssistFunc;
    private final ResourceHandler playerBeKilledFunc;
    private final CombatHandler playerDamagedFunc;

    public JSEnchantment(
            ScriptEngine engine,
            String enchantName0,
            String internalName0,
            EnchantmentRarity rarity0,
            List<String> desc,
            Set<String> allowedItems,
            CombatHandler attackFunc,
            ResourceHandler playerKilledFunc,
            CombatHandler shootFunc,
            AssistHandler playerAssistFunc,
            ResourceHandler playerBeKilledFunc,
            CombatHandler playerDamagedFunc
    ) {
        this.engine = engine;
        this.enchantName0 = enchantName0;
        this.internalName0 = internalName0;
        this.rarity0 = rarity0;
        this.desc = desc;
        this.attackFunc = attackFunc;
        this.playerKilledFunc = playerKilledFunc;
        this.shootFunc = shootFunc;
        this.playerAssistFunc = playerAssistFunc;
        this.playerBeKilledFunc = playerBeKilledFunc;
        this.playerDamagedFunc = playerDamagedFunc;

        if (allowedItems.contains("WEAPON")) {
            weaponOnly = true;
        }
        if (allowedItems.contains("ARMOR")) {
            armorOnly = true;
        }
        if (allowedItems.contains("BOW")) {
            bowOnly = true;
        }
        if (allowedItems.contains("ROD")) {
            rodOnly = true;
        }
    }

    public ScriptEngine getEngine() {
        return engine;
    }

    public String getEnchantName0() {
        return enchantName0;
    }

    public String getInternalName0() {
        return internalName0;
    }

    public EnchantmentRarity getRarity0() {
        return rarity0;
    }

    @Override
    public String getEnchantName() {
        return enchantName0;
    }

    @Override
    public int getMaxEnchantLevel() {
        return 3;
    }

    @Override
    public String getNbtName() {
        return internalName0;
    }

    @Override
    public EnchantmentRarity getRarity() {
        return rarity0;
    }

    @Override
    public Cooldown getCooldown() {
        return null;
    }

    @Override
    public String getUsefulnessLore(int enchantLevel) {
        return String.join("/s", desc);
    }

    @Override
    public void handleAttackEntity(
            int enchantLevel,
            Player attacker,
            Entity target,
            double damage,
            AtomicDouble finalDamage,
            AtomicDouble boostDamage,
            AtomicBoolean cancel
    ) {
        if (attackFunc != null) {
            attackFunc.handle(enchantLevel, attacker, target, damage, finalDamage, boostDamage, cancel);
        }
    }

    @Override
    public void handlePlayerKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        if (playerKilledFunc != null) {
            playerKilledFunc.handle(enchantLevel, myself, target, resourceManager);
        }
    }

    @Override
    public void handleShootEntity(
            int enchantLevel,
            Player attacker,
            Entity target,
            double damage,
            AtomicDouble finalDamage,
            AtomicDouble boostDamage,
            AtomicBoolean cancel
    ) {
        if (shootFunc != null) {
            shootFunc.handle(enchantLevel, attacker, target, damage, finalDamage, boostDamage, cancel);
        }
    }

    @Override
    public void handlePlayerAssist(
            int enchantLevel,
            Player myself,
            Entity target,
            double damage,
            double finalDamage,
            ResourceManager resourceManager
    ) {
        if (playerAssistFunc != null) {
            playerAssistFunc.handle(enchantLevel, myself, target, damage, finalDamage, resourceManager);
        }
    }

    @Override
    public void handlePlayerBeKilledByEntity(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        if (playerBeKilledFunc != null) {
            playerBeKilledFunc.handle(enchantLevel, myself, target, resourceManager);
        }
    }

    @Override
    public void handlePlayerDamaged(
            int enchantLevel,
            Player myself,
            Entity attacker,
            double damage,
            AtomicDouble finalDamage,
            AtomicDouble boostDamage,
            AtomicBoolean cancel
    ) {
        if (playerDamagedFunc != null) {
            playerDamagedFunc.handle(enchantLevel, myself, attacker, damage, finalDamage, boostDamage, cancel);
        }
    }
}
