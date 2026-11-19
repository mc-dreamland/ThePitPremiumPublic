package cn.charlotte.pit.util;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.item.type.mythic.MythicBowItem;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.item.type.mythic.MythicSwordItem;
import cn.charlotte.pit.util.item.ItemUtil;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.DecimalFormat;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class FuncsKt {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###,###,###,###,###");

    private FuncsKt() {
    }

    public static DecimalFormat getDecimalFormat() {
        return DECIMAL_FORMAT;
    }

    public static int countItem(Inventory inventory, Predicate<ItemStack> matcher) {
        int amount = 0;
        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack != null && itemStack.getType() != Material.AIR && matcher.test(itemStack)) {
                amount += itemStack.getAmount();
            }
        }
        return amount;
    }

    public static Object getInstance(Class<?> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception ignored) {
            for (Field field : clazz.getFields()) {
                if (field.getType() == clazz && Modifier.isStatic(field.getModifiers())) {
                    try {
                        field.setAccessible(true);
                        return field.get(null);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        throw new IllegalStateException("Unable to get instance for " + clazz.getName());
    }

    public static boolean takeItem(Inventory inventory, Predicate<ItemStack> matcher) {
        return takeItem(inventory, 1, matcher);
    }

    public static boolean takeItem(Inventory inventory, int amount, Predicate<ItemStack> matcher) {
        int takeAmount = amount;
        ItemStack[] contents = inventory.getContents();
        for (int index = 0; index < contents.length; index++) {
            ItemStack itemStack = contents[index];
            if (isNotAir(itemStack) && matcher.test(itemStack)) {
                takeAmount -= itemStack.getAmount();
                if (takeAmount < 0) {
                    itemStack.setAmount(itemStack.getAmount() - (takeAmount + itemStack.getAmount()));
                    return true;
                }
                inventory.setItem(index, null);
                if (takeAmount == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isAir(Material material) {
        return material == null || material == Material.AIR || material.name().endsWith("_AIR");
    }

    public static boolean isNotAir(Material material) {
        return !isAir(material);
    }

    public static boolean isAir(ItemStack itemStack) {
        return itemStack == null || itemStack.getType() == Material.AIR || itemStack.getType().name().endsWith("_AIR");
    }

    public static boolean isNotAir(ItemStack itemStack) {
        return !isAir(itemStack);
    }

    public static BukkitRunnable submit(Consumer<BukkitRunnable> executor) {
        return submit(false, false, 0L, 0L, executor);
    }

    public static BukkitRunnable submit(boolean now, boolean async, long delay, long period, Consumer<BukkitRunnable> executor) {
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                executor.accept(this);
            }
        };

        if (now) {
            if (async) {
                runnable.runTaskAsynchronously(ThePit.getInstance());
            } else {
                runnable.runTask(ThePit.getInstance());
            }
        } else if (async) {
            if (period == 0L) {
                runnable.runTaskLaterAsynchronously(ThePit.getInstance(), delay);
            } else {
                runnable.runTaskTimerAsynchronously(ThePit.getInstance(), delay, period);
            }
        } else if (period == 0L) {
            runnable.runTaskLater(ThePit.getInstance(), delay);
        } else {
            runnable.runTaskTimer(ThePit.getInstance(), delay, period);
        }

        return runnable;
    }

    public static IMythicItem toMythicItem(ItemStack itemStack) {
        if (isAir(itemStack)) {
            return null;
        }

        String internalName = ItemUtil.getInternalName(itemStack);
        if (internalName == null) {
            return null;
        }

        IMythicItem mythicItem = switch (internalName) {
            case "mythic_sword" -> new MythicSwordItem();
            case "mythic_bow" -> new MythicBowItem();
            case "mythic_leggings" -> new MythicLeggingsItem();
            default -> null;
        };

        if (mythicItem != null) {
            mythicItem.loadFromItemStack(itemStack);
        }
        return mythicItem;
    }
}
