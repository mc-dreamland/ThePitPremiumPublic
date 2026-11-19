package cn.charlotte.pit.career.impl;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.career.AbstractCareer;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.manager.ResourceManager;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SwordsMan extends AbstractCareer {
    @Override
    public String getDisplayName(int level, int direction) {
        return "剑士";
    }

    @Override
    public String getInternalName() {
        return "swords_man";
    }

    @Override
    public int requirePrestige(int level, int direction) {
        return 1;
    }

    @Override
    public int requireCoin(int level, int direction) {
        return 10000;
    }

    @Override
    public Material getIcon(int level, int direction) {
        switch (level) {
            case 0, 1 -> {
                return Material.WOODEN_SWORD;
            }
            case 2 -> {
                return Material.STONE_SWORD;
            }
            case 3 -> {
                return Material.IRON_SWORD;
            }
            case 4 -> {
                return Material.DIAMOND_SWORD;
            }
            case 5 -> {
                return Material.NETHERITE_SWORD;
            }
            default -> {
                return Material.BARRIER;
            }
        }
    }

    @Override
    public Map<itemStackType, ItemStack> getItemStack(int level, int direction) {
        return Map.of(
                itemStackType.DEFAULT, PitItems.CAREER_SWORDSMAN_DEFAULT_ONE.toItemStack()
        );
    }

    @Override
    public int requireLevel() {
        return 0;
    }

    @Override
    public List<String> getDescription(Player player, int level, int direction) {
        return switch (level) {
            case 0 -> List.of("&7111");
            case 1 -> List.of("&7最底层的剑士,旅途的开始");
            case 2 -> List.of("&7经过努力后晋升的剑士");
            default -> List.of("");
        };
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public void toEnable(int level, int direction) {

    }

    @Override
    public void toDeprecated(int level, int direction) {

    }

    @Override
    public int getDirection(int level, int direction) {
        return 0;
    }

    @Override
    public int directionLevel(int level, int direction) {
        return 0;
    }

    @Override
    public int requireRenown(int level, int direction) {
        return level * 5;
    }

    @Override
    public boolean getNextPlayer(int level, int direction, Player player) {
        return true;
    }

    @Override
    public boolean requirePlayer(int level, int direction, Player player) {
        return true;
    }

    @Override
    public void handlePlayerDamaged(int level, int description, Player myself, Entity attacker, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (level >= 2) {
            boostDamage.set(boostDamage.get() + 0.03 * level);
            if (level < 4 && Util.getPitProfile(myself).getMeleeHit() % 3 != 0) return;
            boostDamage.addAndGet(0.05);
        }
    }

    @Override
    public void handlePlayerKilled(int level, int description, Player myself, Entity target, ResourceManager resourceManager) {
        if (level >= 3) resourceManager.addGoldModifier(0.05 * (level - 2), getDisplayName(level, description));
    }
}
