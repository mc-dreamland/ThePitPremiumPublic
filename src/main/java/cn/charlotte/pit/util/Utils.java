package cn.charlotte.pit.util;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.item.IMythicItemStack;
import cn.charlotte.pit.item.MythicColor;
import cn.charlotte.pit.item.type.ExorcismArmor;
import cn.charlotte.pit.item.type.TurtleHelmet;
import cn.charlotte.pit.item.type.mythic.*;
import cn.charlotte.pit.item.type.occultDoctrine.BlackStone;
import cn.charlotte.pit.item.type.occultDoctrine.BountyQuest;
import cn.charlotte.pit.item.type.occultDoctrine.gleamStone;
import cn.charlotte.pit.item.type.occultDoctrine.greedGold;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.random.RandomUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import de.tr7zw.nbtapi.iface.ReadableNBTList;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

@Slf4j
public class Utils {

    private static final Cache<Object, Optional<IMythicItem>> MYTHIC_ITEMS_CACHE = CacheBuilder.newBuilder()
            .weakKeys()
            .recordStats()
            .build();
    /**
     * 返回-1为没有
     *
     * @param item
     * @param enchantName
     * @return
     */
//    public static int getEnchantLevel(ItemStack item, String enchantName) {
//        final IMythicItem mythicItem = getMythicItem(item);
//        if (mythicItem == null) {
//            return -1;
//        }
//
//        for (Map.Entry<AbstractEnchantment, Integer> entry : mythicItem.getEnchantments().entrySet()) {
//            if (entry.getKey().getNbtName().equals(enchantName)) {
//                return entry.getValue();
//            }
//        }
//
//        return -1;
//    }
//    @Nullable
//    public static NbtCompound getCompound(ItemStack item) {
//        if (!item.hasItemMeta()) {
//            return null;
//        }
//        item = MinecraftReflection.getBukkitItemStack(item);
//        return NbtFactory.fromItemOptional(item)
//                .map(NbtFactory::asCompound)
//                .orElse(null);
//    }

    /**
     * 返回-1为没有
     *
     * @param item
     * @param enchantName
     * @return
     */
    public static int getEnchantLevel(ItemStack item, String enchantName) {
        if (item == null || item.getType() == Material.AIR) {
            return -1;
        }
        return NBT.get(item, tag -> {
            ReadableNBT extra = tag.getCompound("extra");
            if (extra == null || !extra.hasTag("ench")) {
                return -1;
            }
            ReadableNBTList<String> list = extra.getStringList("ench");
            if (list == null || list.isEmpty()) {
                return -1;
            }

            for (int i = 0; i < list.size(); i++) {
                String string = list.get(i);
                String[] split = string.split(":");
                if (split.length != 2) {
                    return -1;
                }
                if (split[0].equals(enchantName)) {
                    return Integer.parseInt(split[1]);
                }
            }

            return -1;
        });
//        NbtCompound tag = getCompound(item);
//        if (tag == null || !tag.containsKey("extra")) {
//            return -1;
//        }
//        NbtCompound extra = tag.getCompound("extra");
//        if (!extra.containsKey("ench")) {
//            return -1;
//        }
//        NbtList<Object> ench = extra.getList("ench");
//        for (NbtBase<Object> let : ench.getValue()) {
//            String line = let.getValue().toString();
//            String[] split = line.split(":");
//            if (split.length != 2) {
//                continue;
//            }
//            if (split[0].equals(enchantName)) {
//                return Integer.parseInt(split[1]);
//            }
//        }
//        return -1;
    }


    /**
     * 超级高效的split方法。
     *
     * @param line string
     * @return a array of strings
     */
    public static String[] splitByCharAt(final String line, final char delimiter) {
        CharSequence[] temp = new CharSequence[(line.length() / 2) + 1];
        int wordCount = 0;
        int i = 0;
        int j = line.indexOf(delimiter); // first substring

        while (j >= 0) {
            temp[wordCount++] = line.substring(i, j);
            i = j + 1;
            j = line.indexOf(delimiter, i); // rest of substrings
        }

        temp[wordCount++] = line.substring(i); // last substring

        String[] result = new String[wordCount];
        System.arraycopy(temp, 0, result, 0, wordCount);

        return result;
    }

    /**
     * 手写算法
     *
     * @param ment
     * @param nbtTagList
     */

    public static void readEnchantments(Map<AbstractEnchantment, Integer> ment, ReadableNBTList<String> nbtTagList) {
        int size = nbtTagList.size();
        for (int i = 0; i < size; i++) {
            String s = nbtTagList.get(i);
            final int offset = 2;
            final int strLen = s.length();
            for (int length = strLen - offset; length > 0; length--) {
                char splitArg = s.charAt(length);
                boolean fastEqual = splitArg == ':'; //check equal
                if (fastEqual) { //nano respond 10x faster
                    int lengthUnsigned = length + 1;
                    int level;
                    if (lengthUnsigned + 1 == strLen) {
                        level = Character.getNumericValue(s.charAt(length + 1)); //read level as char
                    } else {
                        String substring = s.substring(length + 1, strLen);
                        level = Integer.parseInt(substring); //read Level as string
                    }
                    String substring = s.substring(0, length);
                    AbstractEnchantment enchantment = ThePit.getInstance()
                            .getEnchantmentFactor()
                            .getEnchantmentMap()
                            .get(substring);
                    if (enchantment == null) {
                        return;
                    }
                    ment.put(enchantment, level);
                }
            }
        }
    }

    public static boolean isAddon() {
        return checkClassExists("com.araykal.pit.Extend");
    }

    public static boolean checkClassExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }


    @SneakyThrows
    public static IMythicItem getMythicItem(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        // It's SAFE to cache MythicItem because it's IMMUTABLE
        if (item instanceof CraftItemStack l) {
            net.minecraft.world.item.ItemStack handle = l.handle;
            Optional<IMythicItem> cachedItem = MYTHIC_ITEMS_CACHE.getIfPresent(handle);
            if (cachedItem != null && cachedItem.isPresent()) {
                return cachedItem.get();
            }
            if (!item.hasItemMeta()) {
                return null;
            }
            return MYTHIC_ITEMS_CACHE.get(handle, () -> Optional.ofNullable(getMythicItem0(item)))
                    .orElseGet(() -> getMythicItem0(item));
        }
        if (!item.hasItemMeta()) {
            return null;
        }
        return getMythicItem0(item);
    }

    public static String mythicItemCacheStats() {
        return MYTHIC_ITEMS_CACHE.stats().toString();
    }

    public static void invalidateMythicItemCache(ItemStack item) {
        if (item instanceof CraftItemStack craftItemStack) {
            MYTHIC_ITEMS_CACHE.invalidate(craftItemStack.handle);
        }
    }

    @Nullable
    public static IMythicItem getMythicItem0(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;

        final String internalName = ItemUtil.getInternalName(item);
        IMythicItem mythicItem = null;
        switch (internalName) {
            case "mythic_sword" -> mythicItem = new MythicSwordItem();
            case "mythic_bow" -> mythicItem = new MythicBowItem();
            case "mythic_leggings" -> mythicItem = new MythicLeggingsItem();
            case "angel_chestplate" -> mythicItem = new AngelChestplate();
            case "armageddon_boots" -> mythicItem = new ArmageddonBoots();
            case "kings_helmet" -> mythicItem = new GoldenHelmet();
            case "turtle_helmet" -> mythicItem = new TurtleHelmet();
            case "lucky_chestplate" -> mythicItem = new LuckyChestplate();
            case "MythicShovel" -> mythicItem = new MythicShovelItem();
            case "exorcism_armor" -> mythicItem = new ExorcismArmor();
            case "black_stone" -> mythicItem = new BlackStone();
            case "bounty_quest" -> mythicItem = new BountyQuest();
            case "gleam_stone" -> mythicItem = new gleamStone();
            case "greed_gold" -> mythicItem = new greedGold();
            case "fly_boots" -> mythicItem = new FlyBoots();
            case null, default -> {
                return null;
            }
        }

        mythicItem.loadFromItemStack(item);

        return mythicItem;
    }

    public static boolean canUseGen(ItemStack item, boolean is) {
        if (item == null) {
            return false;
        }

        final IMythicItem mythicItem = FuncsKt.toMythicItem(item);
        if (mythicItem == null || !mythicItem.isEnchanted() || mythicItem.isBoostedByGem() || mythicItem.isBoostedByGlobalGem()) {
            return false;
        }

        if (mythicItem.getColor() == MythicColor.DARK) {
            return false;
        }

        for (Map.Entry<AbstractEnchantment, Integer> entry : mythicItem.getEnchantments().entrySet()) {
            if ((is == (entry.getKey().getRarity().getParentType() == EnchantmentRarity.RarityType.RARE)) && entry.getValue() < entry.getKey().getMaxEnchantLevel()) {
                return true;
            }
        }

        return false;
    }

    public static ItemStack subtractLive(ItemStack item) {
        if (item == null) {
            return null;
        }

        final IMythicItem mythicLeggings = MythicUtil.getMythicItem(item);
        if (mythicLeggings == null) return null;
        if (mythicLeggings.isEnchanted() || mythicLeggings instanceof IMythicItemStack) {
            if (mythicLeggings instanceof IMythicItemStack IMythicItem && IMythicItem.hasLive()) {
//                if (Util.IMythicItemBoostMapHas(mythicLeggings, PitItems.ETERNAL_HEART.getInternalName()) && RandomUtil.hasSuccessfullyByChance(0.5)) return item;
                if (mythicLeggings.eternal && RandomUtil.hasSuccessfullyByChance(0.5)) return item;
                if (mythicLeggings.getLive() <= 1) {
                    return new ItemStack(Material.AIR);
                } else {
                    mythicLeggings.setLive(mythicLeggings.getLive() - 1);
                    return mythicLeggings.toItemStack();
                }
            }
//            if (Util.IMythicItemBoostMapHas(mythicLeggings, PitItems.ETERNAL_HEART.getInternalName()) && RandomUtil.hasSuccessfullyByChance(0.5)) return item;
            if (mythicLeggings.eternal && RandomUtil.hasSuccessfullyByChance(0.5)) return item;
            if (mythicLeggings.getLive() <= 1) {
                return new ItemStack(Material.AIR);
            } else {
                mythicLeggings.setLive(mythicLeggings.getLive() - 1);
                return mythicLeggings.toItemStack();
            }
        }
        return item;
    }
}
