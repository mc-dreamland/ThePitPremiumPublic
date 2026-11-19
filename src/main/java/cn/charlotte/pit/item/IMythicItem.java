package cn.charlotte.pit.item;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.sub.EnchantmentRecord;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.item.type.ExorcismArmor;
import cn.charlotte.pit.item.type.mythic.*;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.chat.RomanUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.random.RandomUtil;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import de.tr7zw.nbtapi.iface.ReadableNBTList;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

@Setter
@Getter
public abstract class IMythicItem extends AbstractPitItem {

    private final static UUID defUUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    public int maxLive;
    public int live;
    public int tier;
    public MythicColor color;
    public DyeColor dyeColor;
    public String prefix;
    public boolean setCustomName = false;
    public boolean boostedByGem = false;
    public boolean boostedByGlobalGem = false;
    public boolean boostedByBook = false;
    public boolean eternal = false;
    public boolean boostedByPhaseBook = false;
    //    public Map<String,Boolean> boostmap = new HashMap<>(8);
    public int killCounterGem = -1;
    public String customName = null;
    public UUID uuid;
    public String creator;
    public String creatorName;
    public String owner;
    public String ownerName;
    public boolean traded = false;
    public long tradeTime = 0L;

    public IMythicItem() {
    }

    @Override
    protected boolean isUuidEnabledByDefault() {
        return true;
    }

    public void resetUUID() {
        this.uuid = defUUID;
    }

    @Override
    public ItemStack toItemStack() {
        List<String> lore = new ArrayList<>();
        if (this instanceof IMythicItemStack IMythicItem) {
            lore.add("");
            if (IMythicItem.hasLive()) {
                double ratio = live / (maxLive * 1.0);
                lore.add(String.format("&7生命: %s%d&7/%d", ratio <= 0.3 ? "&c" : (ratio <= 0.6 ? "&e" : "&a"), live, maxLive));
                lore.add("");
            }

            lore.addAll(IMythicItem.getLore());
            lore.add("");
            return new ItemBuilder(this.toItemStack()).lore(lore).buildWithUnbreakable();
        }
        if (this.color == null) {
            this.color = RandomUtil.helpMeToChooseOne(
                    MythicColor.RED, MythicColor.ORANGE, MythicColor.BLUE, MythicColor.GREEN, MythicColor.YELLOW
            );
        }

        if (isEnchanted()) {
            if (tier == 0) {
                tier = 3;
            }
        }

        applyPrefixAndNameRulesAfterCounting();

        String name = buildInitialNameByType(getItemDisplayName());
        name = applyFinalNameAdjustments(name);

        if (maxLive != 0) {
            double ratio = live / (maxLive * 1.0);
            String colorCode = ratio <= 0.3 ? "&c" : (ratio <= 0.6 ? "&e" : "&a");
            lore.add(String.format("&7生命: %s%d&7/%d", colorCode, live, maxLive)
                    + (isBoostedByGem() ? "&a ♦" : "")
                    + (isBoostedByGlobalGem() ? "&b ♦" : "")
                    + (isEternal() ? "&3 ♦" : "")
                    + (boostedByBook ? "&6 ᥀" : "")
                    + (boostedByPhaseBook ? "&b ᥀" : ""));
            lore.add("");
        }

        if (isEnchanted()) {
            final AbstractEnchantment somber = ThePit.getInstance().getEnchantmentFactor().getEnchantmentMap().get("somber_enchant");
            if (color == MythicColor.DARK && !enchantments.containsKey(somber)) {
                enchantments.put(somber, 1);
            }
            boolean genesisFound = false;
            for (Map.Entry<AbstractEnchantment, Integer> entry : enchantments.entrySet()) {
                getEnchantLore(lore, entry, enchantments.entrySet());
                if (entry.getKey().getRarity() == EnchantmentRarity.GENESIS) {
                    genesisFound = true;
                }
            }

            if (this instanceof MythicLeggingsItem) {
                if (color != MythicColor.DARK) {
                    lore.add((dyeColor == null ? color.getChatColor() : dyeColor.getChatColor())
                            + "穿着时提供与铁护腿相同的伤害减免效果 &7");
                } else {
                    lore.add((dyeColor == null ? color.getChatColor() : dyeColor.getChatColor())
                            + "穿着时提供与皮革护腿相同的伤害减免效果 &7");
                }
            } else {
                lore.add(ThePit.getApi().getWaterMark());
            }

            if (genesisFound) {
                lore.add(color.getChatColor() + "阵营活动奖励");
            }

        } else {
            lore.add("&7死亡后保留");
            lore.add("");
            if (this instanceof MythicLeggingsItem) {
                lore.add((dyeColor == null ? color.getChatColor() : dyeColor.getChatColor()) + "在神话之井中附魔");
                lore.add((dyeColor == null ? color.getChatColor() : dyeColor.getChatColor()) + "同时,也是一种潮流的象征");
            } else {
                lore.add("&7在神话之井中附魔");
                if (this instanceof MythicShovelItem) lore.add("&7不同寻常之物?");
            }
            this.tier = 0;
        }

        if (dyeColor != null && this instanceof MythicLeggingsItem) {
            lore.add("&7原: " + color.getChatColor() + color.getDisplayName() + "色神话之甲");
        }

        ItemBuilder builder = new ItemBuilder(this.getItemDisplayMaterial());
        if (name != null) {
            builder.composedName(name);
        }
        if (customName != null) {
            builder.customName(customName);
        }

        if (killCounterGem != -1) {
            lore.add("&c记录的击杀: " + killCounterGem);
        }

//        if (this.boostmap == null) {
//            this.boostmap = new HashMap<>(8);
//        }
//
//        if (!this.boostmap.isEmpty()) {
//            builder.putNbt("boostMap", this.boostmap);
//        }

        if (this instanceof IMythicSword mythicSword) {
            builder
                    .lore(lore)
                    .internalName(getInternalName())
                    .deathDrop(false)
                    .canSaveToEnderChest(true)
                    .removeOnJoin(false)
                    .canDrop(false)
                    .canTrade(true)
                    .enchant(enchantments)
                    .itemDamage(mythicSword.getItemDamage())
                    .maxLive(this.maxLive)
                    .tier(this.tier)
                    .makeBoostedByGem(this.boostedByGem)
                    .makeBoostedByEternal(this.eternal)
                    .makeBoostedByGlobalGem(this.boostedByGlobalGem)
                    .makeBoostedByBook(boostedByBook)
                    .makeBoostedByPhaseBook(boostedByPhaseBook)
                    .killCounterGem(killCounterGem)
                    .live(this.live)
//                    .putNbt("boostMap",boostmap)
                    .recordEnchantments(enchantmentRecords);
        } else if (this instanceof IMythicShovel shovel) {
            builder
                    .lore(lore)
                    .internalName(getInternalName())
                    .deathDrop(false)
                    .canSaveToEnderChest(true)
                    .removeOnJoin(false)
                    .canDrop(false)
                    .canTrade(true)
                    .enchant(enchantments)
                    .itemDamage(shovel.getItemDamage())
                    .maxLive(this.maxLive)
                    .tier(this.tier)
                    .makeBoostedByGem(this.boostedByGem)
                    .makeBoostedByGlobalGem(this.boostedByGlobalGem)
                    .makeBoostedByBook(boostedByBook)
                    .makeBoostedByEternal(this.eternal)
                    .makeBoostedByPhaseBook(boostedByPhaseBook)
                    .killCounterGem(killCounterGem)
                    .live(this.live)
//                    .putNbt("boostMap",boostmap)
                    .recordEnchantments(enchantmentRecords);
        } else {
            builder
                    .lore(lore)
                    .internalName(getInternalName())
                    .deathDrop(false)
                    .canDrop(false)
                    .canTrade(true)
                    .canSaveToEnderChest(true)
                    .removeOnJoin(false)
                    .enchant(enchantments)
                    .maxLive(this.maxLive)
                    .live(this.live)
                    .makeBoostedByGem(this.boostedByGem)
                    .makeBoostedByGlobalGem(this.boostedByGlobalGem)
                    .makeBoostedByBook(boostedByBook)
                    .makeBoostedByEternal(this.eternal)
                    .makeBoostedByPhaseBook(boostedByPhaseBook)
                    .killCounterGem(killCounterGem)
                    .tier(this.tier)
//                    .putNbt("boostMap",boostmap)
                    .recordEnchantments(enchantmentRecords);
        }

        if (this instanceof MythicLeggingsItem) {
            if (color != MythicColor.DARK) {
                builder.itemArmor(5.0);
            }
        }

        if (this instanceof GoldenHelmet) {
            builder.itemArmor(3.0);
            builder.ArmorToughness(2.0);
        }

        if (this instanceof ExorcismArmor) {
            builder.itemArmor(9.0);
            builder.ArmorToughness(2.0);
        }

        if (this instanceof FlyBoots) {
            builder.ArmorToughness(0.5);
            builder.speed(0.01);
        }

        if (dyeColor != null) {
            builder.dyeColor(dyeColor.name());
        }

        if (isEnchanted()) {
            builder.shiny();
        }

        this.setMythicColor(builder, color);

        if (this instanceof MythicLeggingsItem || this instanceof ArmageddonBoots) {
            builder.setLetherColor(dyeColor == null ? color.getLeatherColor() : dyeColor.getColor());
        }

        if (this.prefix != null) {
            builder.prefix(prefix);
        }

        applyOwnershipData(builder);

        return builder.buildWithUnbreakable();
    }

    protected ItemBuilder applyOwnershipData(ItemBuilder builder) {
        if (isUuidEnabled()) builder.uuid(ensureUuid());
        if (creator != null && !creator.isBlank()) {
            builder.changeNbt("creator", creator);
        }
        if (creatorName != null && !creatorName.isBlank()) {
            builder.changeNbt("creatorName", creatorName);
        }
        if (owner != null && !owner.isBlank()) {
            builder.changeNbt("owner", owner);
        }
        if (ownerName != null && !ownerName.isBlank()) {
            builder.changeNbt("ownerName", ownerName);
        }
        builder.changeNbt("traded", traded);
        if (tradeTime > 0L) {
            builder.changeNbt("tradeTime", tradeTime);
        }
        return builder;
    }

    private UUID ensureUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        return uuid;
    }

    protected void loadOwnershipData(ReadableNBT extra) {
        uuid = null;
        if (extra.hasTag("uuid")) {
            String uuidValue = extra.getString("uuid");
            if (uuidValue != null && !uuidValue.isBlank()) {
                try {
                    uuid = UUID.fromString(uuidValue);
                } catch (IllegalArgumentException ignored) {
                    // 遗留的非标准 UUID 会在物品下次构建时自动替换。
                }
            }
        }
        creator = extra.hasTag("creator") ? extra.getString("creator") : null;
        creatorName = extra.hasTag("creatorName") ? extra.getString("creatorName") : null;
        owner = extra.hasTag("owner") ? extra.getString("owner") : null;
        ownerName = extra.hasTag("ownerName") ? extra.getString("ownerName") : null;
        tradeTime = extra.hasTag("tradeTime") ? extra.getLong("tradeTime") : 0L;
        traded = (extra.hasTag("traded") && extra.getBoolean("traded")) || tradeTime > 0L;
    }

    private String buildInitialNameByType(String base) {
        String name = base;
        if (this instanceof MythicLeggingsItem) {
            name = color.getChatColor() + (tier > 0 ? RomanUtil.convert(tier) + " 阶" : "") + color.getDisplayName() + "色" + base;
            if (!base.equals(getDefaultItemDisplayName())) return name;
            if (dyeColor != null) {
                name = dyeColor.getChatColor() + (tier > 0 ? RomanUtil.convert(tier) + " 阶" : "") + "染色神话之甲";
            }
            if (color == MythicColor.DARK) {
                name = color.getChatColor() + (tier > 0 ? RomanUtil.convert(tier) + " 阶" : "") + "暗黑之甲";
            }
            if (color == MythicColor.RAGE) {
                name = color.getChatColor() + (tier > 0 ? RomanUtil.convert(tier) + " 阶" : "") + "暴怒之甲";
            }
            if (color == MythicColor.DARK_GREEN) {
                name = color.getChatColor() + (tier > 0 ? RomanUtil.convert(tier) + " 阶" : "") + "下水道之甲";
            }
        } else if (this instanceof MythicSwordItem) {
            name = (tier >= 3 ? "&c" : "&e") + (tier > 0 ? RomanUtil.convert(tier) + " 阶" : "") + base;
        } else if (this instanceof MythicBowItem) {
            name = (tier >= 3 ? "&c" : "&b") + (tier > 0 ? RomanUtil.convert(tier) + " 阶" : "") + base;
        } else if (this instanceof MagicFishingRod) {
            name = "&e" + base;
        }
        return name;
    }

    private void applyPrefixAndNameRulesAfterCounting() {
        int rareAmount = 0;
        int enchantTotalLevel = 0;
        boolean anyRareAtMaxOnRage = false;

        for (Map.Entry<AbstractEnchantment, Integer> e : enchantments.entrySet()) {
            AbstractEnchantment ench = e.getKey();
            int lvl = e.getValue();

            if (ench.getRarity() == EnchantmentRarity.RARE) {
                rareAmount++;
                if (color == MythicColor.RAGE && lvl == ench.getMaxEnchantLevel()) {
                    anyRareAtMaxOnRage = true;
                }
            }
            enchantTotalLevel += lvl;
        }

        java.util.LinkedHashSet<String> tags = new java.util.LinkedHashSet<>();

        if (enchantTotalLevel >= 8) {
            if (color == MythicColor.RAGE) {
                tags.add("§c狂躁");
            } else {
                tags.add("§6传奇");
            }
        }

        if (rareAmount == 1) {
            tags.add("§b出众");
        } else if (rareAmount == 2) {
            tags.add("§d非凡");
        } else if (rareAmount >= 3) {
            tags.add("§5稀世");
        }

        if (this.maxLive >= 100) {
            if (color != MythicColor.DARK) {
                tags.add("§3精铸");
            }
        }

        if (anyRareAtMaxOnRage) {
            tags.add("§b※§d神器§c※");
        }

        java.util.List<String> order = java.util.Arrays.asList("§b※§d神器§c※", "§c狂躁", "§6传奇", "§b出众", "§d非凡", "§5稀世", "§3精铸");
        java.util.List<String> sorted = tags.stream()
                .sorted(java.util.Comparator.comparingInt(s -> {
                    int idx = order.indexOf(s);
                    return idx < 0 ? Integer.MAX_VALUE : idx;
                }))
                .toList();

        this.prefix = sorted.isEmpty() ? "" : String.join(" ", sorted);
    }

    private String applyFinalNameAdjustments(String rawName) {
        String name = rawName;

        if (this.maxLive >= 100 && color == MythicColor.DARK && this instanceof MythicLeggingsItem
                && getItemDisplayName().equals(getDefaultItemDisplayName())) {
            name = color.getChatColor() + (tier > 0 ? RomanUtil.convert(tier) + " 阶" : "") + "恶魔之甲";
        }

        if (this.prefix != null && !this.prefix.isEmpty()) {
            if (name != null && name.length() >= 2 && (name.startsWith("&") || name.startsWith("§"))) {
                name = name.substring(0, 2) + this.prefix + " " + name.substring(2);
            } else if (name != null) {
                name = this.prefix + " " + name;
            }
        }

        if (this.customName != null) {
            name = customName;
        }
        return name;
    }

    @Override
    public void loadFromItemStack(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        ReadableNBT tag = NBT.readNbt(item);
        ReadableNBT extra = tag.getCompound("extra");
        if (extra == null) {
            return;
        }

        loadOwnershipData(extra);

        if (extra.hasTag("prefix")) {
            this.prefix = extra.getString("prefix");
        }

        if (extra.hasTag("boostedByGem")) {
            this.boostedByGem = extra.getBoolean("boostedByGem");
        } else {
            this.boostedByGem = false;
        }
        if (extra.hasTag("boostedByGlobalGem")) {
            this.boostedByGlobalGem = extra.getBoolean("boostedByGlobalGem");
        } else {
            this.boostedByGlobalGem = false;
        }
        if (extra.hasTag("customName")) {
            this.customName = extra.getString("customName");
        }

        if (extra.hasTag("boostedByBook")) {
            this.boostedByBook = extra.getBoolean("boostedByBook");
        } else {
            this.boostedByBook = false;
        }

        if (extra.hasTag("boostedByEternal")) this.eternal = extra.getBoolean("boostedByEternal");
        else this.eternal = false;

        if (extra.hasTag("boostedByPhaseBook")) this.boostedByPhaseBook = extra.getBoolean("boostedByPhaseBook");
        else this.boostedByPhaseBook = false;

        if (extra.hasTag("killCounterGem")) {
            this.killCounterGem = extra.getInteger("killCounterGem");
        } else {
            this.killCounterGem = -1;
        }

        if (extra.hasTag("dyeColor")) {
            this.dyeColor = DyeColor.valueOf(extra.getString("dyeColor"));
        }

//        if (extra.hasTag("boostMap")) {
//            ReadableNBT boostMapCompound = extra.getCompound("boostMap");
//            if (boostMapCompound != null) {
//                this.boostmap.clear();
//                for (String key : boostMapCompound.getKeys()) {
//                    if (boostMapCompound.hasTag(key) &&
//                            boostMapCompound.getType(key) == NBTType.NBTTagByte) {
//                        boolean value = boostMapCompound.getBoolean(key);
//                        this.boostmap.put(key, value);
//                    }
//                }
//            }
//        } else {
//            this.boostmap = new HashMap<>(8);
//        }

        if (!extra.hasTag("mythic_color")) {
            this.color = RandomUtil.helpMeToChooseOne(MythicColor.RED, MythicColor.ORANGE, MythicColor.BLUE, MythicColor.GREEN, MythicColor.YELLOW);
        } else {
            String internalColor = extra.getString("mythic_color");
            boolean colorFound = false;
            for (MythicColor value : MythicColor.values()) {
                if (value.getInternalName().equals(internalColor)) {
                    this.color = value;
                    colorFound = true;
                    break;
                }
            }
            if (!colorFound) {
                this.color = RandomUtil.helpMeToChooseOne(MythicColor.RED, MythicColor.ORANGE, MythicColor.BLUE, MythicColor.GREEN, MythicColor.YELLOW);
            }
        }

        this.maxLive = extra.getInteger("maxLive");
        this.live = extra.getInteger("live");

        this.enchantments = new HashMap<>();

        if (!extra.hasTag("ench")) {
            return;
        }

        final String recordsString = extra.getString("records");
        if (recordsString != null) {
            for (String recordString : Utils.splitByCharAt(recordsString, ';')) {
                final String[] split = Utils.splitByCharAt(recordString, '|');
                if (split.length >= 3) {
                    enchantmentRecords.add(
                            new EnchantmentRecord(
                                    split[0],
                                    split[1],
                                    Long.parseLong(split[2])
                            )
                    );
                }
            }
        }

        ReadableNBTList<String> ench = extra.getStringList("ench");
        Utils.readEnchantments(enchantments, ench);
        if (!extra.hasTag("tier") && isEnchanted()) {
            if (color == MythicColor.DARK) {
                this.tier = 2;
            } else {
                this.tier = 3;
            }
        } else {
            if (extra.hasTag("tier")) {
                if (!isEnchanted()) {
                    this.tier = 0;
                } else {
                    this.tier = extra.getInteger("tier");
                }
            }
        }
    }

    protected void setMythicColor(ItemBuilder builder, MythicColor color) {
        builder.changeNbt("mythic_color", color.getInternalName());
    }
}
