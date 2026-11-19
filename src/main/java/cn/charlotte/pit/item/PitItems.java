package cn.charlotte.pit.item;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.config.ItemSettings;
import cn.charlotte.pit.activity.newyear.item.CommemorativeCoin;
import cn.charlotte.pit.activity.newyear.item.NewYearLuckyBag;
import cn.charlotte.pit.career.careeritem.swordsman.DefaultSwordOne;
import cn.charlotte.pit.item.listenerType.IPlayerInteract;
import cn.charlotte.pit.item.type.*;
import cn.charlotte.pit.item.type.mythic.*;
import cn.charlotte.pit.item.type.occultDoctrine.*;
import cn.charlotte.pit.item.type.swordBundleEmpty;
import cn.charlotte.pit.map.kingsquests.item.*;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.MythicUtil;
import cn.charlotte.pit.util.item.AdvancedItemIdentity;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public enum PitItems {
    //IMythicItem
    MAGIC_FISHING_ROD(MagicFishingRod.class),
    MYTHIC_BOW(MythicBowItem.class),
    MYTHIC_LEGGING(MythicLeggingsItem.class),
    MYTHIC_SWORD(MythicSwordItem.class),
    ANGEL_CHEST_PLATE(AngelChestplate.class),
    ARMAGEDDON_BOOTS(ArmageddonBoots.class),


    //AbstractPitItem
    BAKED_POTATO(BakedPotato.class),
    BEEF(Beef.class),
    BOUNTY_SOLVENT_POTION(BountySolventPotion.class),
    BROKEN_SOUL(BrokenSoul.class),
    CHUNK_OF_VILE(ChunkOfVileItem.class),
    CUSTOM_NAME_PAPER(CustomNamePaper.class),
    NAME_REVEAL_EYE(NameRevealEye.class),
    DIMENSIONAL_CACTUS(DimensionalCactus.class),
    FISH_1(Fish_1.class),
    FISH_2(Fish_2.class),
    FISH_3(Fish_3.class),
    FISH_4(Fish_4.class),
    FISH_5(Fish_5.class),
    FISH_6(Fish_6.class),
    FOUR_IN_A_ROW_GADGET(FourInARowGadget.class),
    FUNKY_FEATHER(FunkyFeather.class),
    GAIN_BANNER(GainBanner.class),
    GLOBAL_ATTENTION_GEM(GlobalAttentionGem.class),
    GOLDEN_HELMET(GoldenHelmet.class),
    GRIM_REAPER(GrimReaper.class),
    JEWEL_SWORD(JewelSword.class),
    JUMP_BOOST_POTION(JumpBoostPotion.class),
    KILL_COUNTER_GEM(KillCounterGem.class),
    LUCKY_CHESTPLATE(LuckyChestplate.class),
    LUCKY_GEM(LuckyGem.class),
    MILK_BUCKET(MilkBucket.class),
    CAKE_CHERRY(CakeCherry.class),
    COOKIES(Cookies.class),
    MUSICAL_RUNE(MusicalRune.class),
    MYTHIC_REPAIRKIT(MythicRepairKit.class),
    PIT_CACTUS(PitCactus.class),
    RUBBIN(Rubbin.class),
    SACROSANCT_CACTUS(SacrosanctCactus.class),
    SMALL_MYTHIC_REPAIR_KIT(SmallMythicRepairKit.class),
    LUCKY_LOTTERY(LuckyLottery.class),
    SPIRE_LEATHER_CHESTPLATE(SpireArmor.class, Material.LEATHER_CHESTPLATE),
    SPIRE_CHAINMAIL_LEGGINGS(SpireArmor.class, Material.CHAINMAIL_LEGGINGS),
    SPIRE_IRON_CHESTPLATE(SpireArmor.class, Material.IRON_CHESTPLATE),
    SPIRE_IRON_BOOTS(SpireArmor.class, Material.IRON_BOOTS),
    SPIRE_DIAMOND_CHESTPLATE(SpireArmor.class, Material.DIAMOND_CHESTPLATE),
    SPIRE_IRON_LEGGINGS(SpireArmor.class, Material.IRON_LEGGINGS),
    SPIRE_DIAMOND_BOOTS(SpireArmor.class, Material.DIAMOND_BOOTS),

    SPIRE_WOODEN_SWORD(SpireSword.class, Material.WOODEN_SWORD),
    SPIRE_IRON_SWORD(SpireSword.class, Material.IRON_SWORD),
    SPIRE_DIAMOND_SWORD(SpireSword.class, Material.DIAMOND_SWORD),

    SUPER_PACKAGE(SuperPackage.class),
    TACTICAL_INSERTIONS(TacticalInsertions.class),
    TOTALLY_LEGIT_GEM(TotallyLegitGem.class),
    UBER_DROP(UberDrop.class),
    ACTION_GIFT_BOX(ActionGiftBox.class),

    //kings_quests_items
    CHERRY(Cherry.class),
    HIGH_GRADE_EGGS(HighGradeEggs.class),
    MINI_CAKE(MiniCake.class),
    PACKAGED_BALE(PackagedBale.class),
    SUGAR(Sugar.class),
    WHEAT(Wheat.class),
    YUMMY_BREAD(YummyBread.class),
    MAGNET(Magnet.class),
    RANDOMMYTHIC(mythicRandomVoucher.class),
    FISHING_DIA_CHESTPLATE(new ItemBuilder(Material.DIAMOND_CHESTPLATE)
            .internalName("fishing_dia_armor")
            .removeOnJoin(false)
            .deathDrop(true)
            .canDrop(false)
            .canTrade(true)
            .canSaveToEnderChest(true)
            .name("&b钓鱼者的钻石胸甲")
            .buildWithUnbreakable(), "fishing_dia_armor"),

    FISHING_DIA_BOOTS(new ItemBuilder(Material.DIAMOND_BOOTS)
            .internalName("fishing_dia_armor")
            .removeOnJoin(false)
            .deathDrop(true)
            .canDrop(false)
            .canTrade(true)
            .canSaveToEnderChest(true)
            .name("&b钓鱼者的钻石鞋子")
            .buildWithUnbreakable(), "fishing_dia_armor"),

    FISHING_DIA_LEGGINGS(new ItemBuilder(Material.DIAMOND_LEGGINGS)
            .internalName("fishing_dia_armor")
            .removeOnJoin(false)
            .deathDrop(true)
            .canDrop(false)
            .canTrade(true)
            .canSaveToEnderChest(true)
            .name("&b钓鱼者的钻石护腿")
            .buildWithUnbreakable(), "fishing_dia_armor"),

    GOLDEN_APPLE(new ItemBuilder(Material.GOLDEN_APPLE)
            .internalName("golden_apple")
            .canDrop(false)
            .canSaveToEnderChest(false)
            .deathDrop(true)
            .removeOnJoin(true)
            .canSaveToEnderChest(false)
            .canDrop(false)
            .isHealingItem(true)
            .canTrade(false).build(), "golden_apple"),
    GOLDEN_HEAD(new ItemBuilder(Material.PLAYER_HEAD)
            .setSkullProperty("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDUyZGNhNjhjOGY4YWY1MzNmYjczN2ZhZWVhY2JlNzE3Yjk2ODc2N2ZjMTg4MjRkYzJkMzdhYzc4OWZjNzcifX19")
            .name("&6金头")
            .deathDrop(true)
            .removeOnJoin(true)
            .canSaveToEnderChest(false)
            .canDrop(false)
            .canTrade(false)
            .isHealingItem(true)
            .internalName("golden_head")
            .build(), "golden_head"),
    TURTLE_HELMET(TurtleHelmet.class),
    FANCY_HAT_HELMET(FancyHatHelmet.class),
    MYTHIC_EXP_BOTTLE(MythicExpBottle.class),
    MUSHROOMS(Mushrooms.class),
    COMMEMORATIVE_COIN(CommemorativeCoin.class),
    BEAST_MODE_HELMET(new ItemBuilder(Material.DIAMOND_HELMET).removeOnJoin(true).deathDrop(true).canTrade(false).lore("&7超级连杀物品", "&c激活连杀后死亡保留", "&c使用指令回城或重新进入房间后消失").internalName("beast_mode_helmet").buildWithUnbreakable(), "beast_mode_helmet"),
    BEAST_MODE_HELMET_AWARD(new ItemBuilder(Material.DIAMOND_HELMET)
            .name("&a野兽之甲")
            .canSaveToEnderChest(true)
            .deathDrop(true)
            .removeOnJoin(false)
            .canTrade(true)
            .internalName("beast_mode_helmet_award")
            .buildWithUnbreakable(), "beast_mode_helmet_award"),
    PERK_BARBARIAN_IRONAXE(new ItemBuilder(Material.IRON_AXE)
            .canDrop(false)
            .deathDrop(false)
            .canSaveToEnderChest(false)
            .internalName("perk_barbarian")
            .lore("&7天赋物品")
            .itemDamage(7)
            .buildWithUnbreakable(), "perk_barbarian"),
    COBBLESTONE(new ItemBuilder(Material.COBBLESTONE)
            .deathDrop(true)
            .canDrop(false)
            .canSaveToEnderChest(false)
            .internalName("perk_miner")
            .build(), "perk_miner"),
    AIR(new ItemStack(Material.AIR), "AIR"),
    DEFAULT_ARROW(new ItemBuilder(Material.ARROW)
            .internalName("default_arrow")
            .defaultItem()
            .canDrop(false)
            .canSaveToEnderChest(false)
            .build(), "default_arrow"),
    OAK_PLANKS_CREATIVE(new ItemBuilder(Material.OAK_PLANKS).canDrop(false).canSaveToEnderChest(false).deathDrop(true).canTrade(false).removeOnJoin(true).internalName("creative_enchantment_item").build(), "creative_enchantment_item"),
    LUCKY_STONE(LuckyStone.class),
    PERK_TASTY_SOUP_KILL(perk_tasty_soup_kill.class),
    PERK_TASTY_SOUP_ASSIST(perk_tasty_soup_assist.class),
    ETERNAL_HEART(eternalHeart.class),
    CAREER_SWORDSMAN_DEFAULT_ONE(DefaultSwordOne.class),
    EXORCISM_ARMOR(ExorcismArmor.class),
    MYSTERY_BOX_ONE(MysteryBoxOne.class),
    MYSTERY_BOX_TWO(MysteryBoxTwo.class),
    MYSTERY_BOX_THREE(MysteryBoxThree.class),
    MYSTERIOUS_FRAGMENTS(MysteriousFragments.class),
    PHASE_BOOK(phaseBook.class),
    BLACK_STONE(BlackStone.class),
    BOUNTY_QUEST(BountyQuest.class),
    GLEAM_STONE(gleamStone.class),
    GREED_GOLD(greedGold.class),
    NEW_YEAR_LUCKY_BAG(NewYearLuckyBag.class),
    FLY_BOOTS(FlyBoots.class),
    KINGQUEST_REFRESH_COUPON(kingQuestRefreshCoupon.class),
    BOW_BUNDLE_EMPTY(bowBundleEmpty.class),
    BOW_BUNDLE_FULL(bowBundleFull.class),
    SWORD_BUNDLE_EMPTY(swordBundleEmpty.class),
    SWORD_BUNDLE_FULL(swordBundleFull.class),
    PANTS_BUNDLE_FULL(pantsBundleFull.class),
    PANTS_BUNDLE_EMPTY(pantsBundleEmpty.class),
    BONE_MEAL(BoneMeal.class),
    MYSTERY_BOX_FRAGMENT(MysteryBoxFragment.class),
    GODEN_BRIC(GoldenBric.class),
    EXPERIENCE_BOTTLE(ExperienceBottle.class)
    ;

    private final Supplier<IMythicItem> factory;     // 仅 PROTOTYPE

    @Getter
    private final AbstractPitItem singletonItem;         // 仅 SINGLETON

    @Getter
    private final Scope scope;
    private final String internalName;
    // 为 SINGLETON 缓存一个模板栈；对外总是 clone，避免外部修改污染模板
    private ItemStack cachedItemStack;

    @Getter
    private IActionDisplayItem displayItem;

    @Getter
    private boolean playerInteracts;

    PitItems(Class<?> clazz) {
        boolean isMythic = IMythicItem.class.isAssignableFrom(clazz);
        AbstractPitItem abstractPitItem;
        if (isMythic) {
            Class<? extends IMythicItem> mythicClass = clazz.asSubclass(IMythicItem.class);
            this.factory = factoryOf(mythicClass);
            this.singletonItem = null;
            this.scope = Scope.PROTOTYPE;
            abstractPitItem = factory.get();
            this.internalName = abstractPitItem.getInternalName();
            ThePit.getInstance().getLogger().info("正在注册神话类物品 - " + clazz.getName() + ", " + abstractPitItem.getInternalName() + ", " + abstractPitItem.getItemDisplayName());
        } else {
            Class<? extends AbstractPitItem> pitClass = clazz.asSubclass(AbstractPitItem.class);
            this.singletonItem = instanceOf(pitClass);
            this.factory = null;
            this.scope = Scope.SINGLETON;
            this.internalName = this.singletonItem.getInternalName();
            abstractPitItem = this.singletonItem;
            ThePit.getInstance().getLogger().info("正在注册天坑物品 - " + clazz.getName() + ", " + abstractPitItem.getInternalName() + ", " + abstractPitItem.getItemDisplayName());
        }

        ItemSettings.registerUuidDefault(internalName, abstractPitItem.isUuidEnabledByDefault());
        playerInteracts = IPlayerInteract.class.isAssignableFrom(clazz);

        if (abstractPitItem instanceof Listener listener) {
            ThePit.getInstance().getLogger().info("  正在注册事件监听 - " + clazz.getName() + ", " + abstractPitItem.getInternalName());
            if (!abstractPitItem.getClass().isAnnotationPresent(AutoRegister.class))
                Bukkit.getPluginManager().registerEvents(listener, ThePit.getInstance());
            ThePit.getInstance().getItemFactor().registerItem(abstractPitItem);
        }

        if (IActionDisplayItem.class.isAssignableFrom(clazz)) {
            try {
                this.displayItem = (IActionDisplayItem) clazz.newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                ThePit.getInstance().getLogger().warning(String.format("%s", e));
            }
        }
    }

    PitItems(Class<?> clazz, Material material) {
        Class<? extends AbstractPitItem> pitClass = clazz.asSubclass(AbstractPitItem.class);
        this.singletonItem = instanceOf(pitClass, material);
        this.factory = null;
        this.scope = Scope.SINGLETON;
        this.internalName = this.singletonItem.getInternalName();

        ItemSettings.registerUuidDefault(internalName, singletonItem.isUuidEnabledByDefault());
        ThePit.getInstance().getLogger().info("正在注册尖塔物品 - " + singletonItem.getInternalName() + ", " + singletonItem.getItemDisplayName());
        if (singletonItem instanceof Listener listener) {
            ThePit.getInstance().getLogger().info("  正在注册事件监听 - " + singletonItem.getInternalName());
            if (!singletonItem.getClass().isAnnotationPresent(AutoRegister.class))
                Bukkit.getPluginManager().registerEvents(listener, ThePit.getInstance());
            ThePit.getInstance().getItemFactor().registerItem(singletonItem);
        }
        playerInteracts = singletonItem.getClass().isAssignableFrom(IPlayerInteract.class);
        if (IActionDisplayItem.class.isAssignableFrom(clazz)) {
            try {
                this.displayItem = (IActionDisplayItem) clazz.newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                ThePit.getInstance().getLogger().warning(String.format("%s", e));
            }
        }
    }

    PitItems(ItemStack itemStack, String internalName) {
        this.factory = null;
        this.displayItem = null;
        this.singletonItem = null;
        this.scope = Scope.BASED;
        this.internalName = internalName;
        this.cachedItemStack = itemStack;
        ThePit.getInstance().getLogger().info("正在注册基础物品 - " + internalName);
    }

    /**
     * 从候选枚举中随机挑一个（便于随机生成需求）
     */
    public static PitItems randomOf(PitItems... candidates) {
        return candidates[ThreadLocalRandom.current().nextInt(candidates.length)];
    }

    public static Supplier<IMythicItem> factoryOf(Class<? extends IMythicItem> type) {
        try {
            var ctor = type.getDeclaredConstructor(); // 无参构造
            if (!ctor.canAccess(null)) ctor.setAccessible(true);
            return () -> {
                try {
                    return ctor.newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("创建 IMythicItem 实例失败: " + type.getName(), e);
                }
            };
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("类缺少无参构造: " + type.getName(), e);
        }
    }

    public static AbstractPitItem instanceOf(Class<? extends AbstractPitItem> type) {
        try {
            var ctor = type.getDeclaredConstructor(); // 无参构造
            if (!ctor.canAccess(null)) ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("创建 AbstractPitItem 实例失败: " + type.getName(), e);
        }
    }

    public static AbstractPitItem instanceOf(Class<? extends AbstractPitItem> type, Material material) {
        try {
            var ctor = type.getDeclaredConstructor(Material.class);
            if (!ctor.canAccess(null)) ctor.setAccessible(true);
            return ctor.newInstance(material);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("用 Material 构造实例失败: " + type.getName(), e);
        }
    }

    public String getInternalName() {
        return internalName;
    }

    /**
     * 对外统一拿 ItemStack。
     * PROTOTYPE：每次调用都会 new 一个 AbstractPitItem 再转成 ItemStack。
     * SINGLETON：懒加载一个模板 ItemStack，之后总是 clone 模板发放。
     */
    public ItemStack toItemStack() {
        if (cachedItemStack == null) {
            if (this.equals(SPIRE_WOODEN_SWORD) || this.equals(SPIRE_IRON_SWORD) || this.equals(SPIRE_DIAMOND_SWORD)) {
                this.singletonItem.enchantments.put(ThePit.getInstance().getEnchantmentFactor().getEnchantmentMap().get("soul_ripper_enchant"), 1);
            }
            if (scope == Scope.PROTOTYPE) {
                ItemStack generated = factory.get().toItemStack();
                AdvancedItemIdentity.ensureUuid(generated);
                return generated;
            } else {
                cachedItemStack = singletonItem.toItemStack();
            }
        }
        ItemStack generated = cachedItemStack.clone();
        AdvancedItemIdentity.regenerateUuid(generated);
        return generated;
    }

    public ItemStack toSingletonItemStack() {
        return scope == Scope.SINGLETON ? singletonItem.toItemStack() : toItemStack();
    }

    public ItemStack toManyItem(int amount) {
        ItemStack itemStack = toItemStack();
        if (amount == 1) return itemStack;
        itemStack.setAmount(amount);
        return itemStack;
    }

    public IMythicItem toIMythicLeggingsItem(MythicColor color) {
        IMythicItem item = MythicUtil.getMythicItem(PitItems.MYTHIC_LEGGING.toItemStack());
        item.setColor(color);
        return item;
    }

    /**
     * 需要拿“物品对象本身”时使用：
     * - PROTOTYPE: 返回全新实例（推荐用于需要重新初始化的物品，例如神话类）
     * - SINGLETON: 返回内部持有的实例（请勿修改其内部可变状态）
     * - BASE: 快速注册物品
     */
    public IMythicItem newMythicItem() {
        if (scope == Scope.PROTOTYPE) {
            return factory.get();
        }
        return null; // 单例场景不建议外部改状态
    }

    public boolean isInternalNameEquals(ItemStack itemStack) {
        String string = ItemUtil.getInternalName(itemStack);
        if (string == null) return false;
        return this.internalName.equals(string);
    }

    public boolean isInternalNameEquals(String internalName) {
        if (internalName == null) return false;
        return this.internalName.equals(internalName);
    }

    public enum Scope {PROTOTYPE, SINGLETON, BASED}
}
