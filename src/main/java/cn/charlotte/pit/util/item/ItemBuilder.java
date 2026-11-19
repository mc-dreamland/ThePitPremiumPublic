package cn.charlotte.pit.util.item;

import cn.charlotte.pit.config.ItemSettings;
import com.google.common.collect.Multimap;
import cn.charlotte.pit.data.sub.EnchantmentRecord;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.util.chat.CC;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBTList;
import de.tr7zw.nbtapi.iface.ReadableNBTList;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class ItemBuilder {

    private final List<Consumer<ReadWriteNBT>> nbtModifiers = Lists.newArrayList();
    private final List<Consumer<ItemMeta>> metaModifiers = Lists.newArrayList();
    private final Map<String, List<Consumer<ReadWriteNBT>>> nbtMapModifiers = new HashMap<>();
    private ItemStack is;
    private String internalName;
    private boolean composedName;
    private UUID requestedUuid;

    public ItemBuilder(Material mat) {
        this.is = new ItemStack(mat);
    }

    public ItemBuilder(ItemStack is) {
        this.is = is;
    }

    public ItemBuilder material(Material mat) {
        this.is = new ItemStack(mat);
        return this;
    }

    public ItemBuilder amount(int amount) {
        this.is.setAmount(amount);
        return this;
    }

    public ItemBuilder name(String name) {
        composedName = false;
        metaModifiers.add(meta -> meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name)));
        return this;
    }

    /** The caller has already composed the configured base name with per-item data. */
    public ItemBuilder composedName(String name) {
        name(name);
        composedName = true;
        return this;
    }

    public ItemBuilder dontStack() {
        return uuid(UUID.randomUUID());
    }

    public ItemBuilder setLetherColor(Color color) {
        metaModifiers.add(meta -> ((LeatherArmorMeta) meta).setColor(color));
        return this;
    }

    public ItemBuilder setSkullOwner(String owner) {
        metaModifiers.add(meta -> ((SkullMeta) meta).setOwner(owner));
        return this;
    }

    public ItemBuilder setSkullProperty(Player player) {
        try {
            metaModifiers.add(l -> {
                if (l instanceof SkullMeta meta) meta.setOwningPlayer(player);
            });
        } catch (Exception e) {
            log.error("", e);
        }
        return this;
    }

    public ItemBuilder setSkullProperty(String texture) {
        metaModifiers.add(l -> {
            SkullMeta meta = (SkullMeta) l;
            GameProfile gp = new GameProfile(UUID.randomUUID(), "pit-" + UUID.randomUUID().toString().split("-")[0]);
            gp.getProperties().put("textures", new Property("textures", texture));
            try {
                Field field = meta.getClass().getDeclaredField("profile");
                field.setAccessible(true);
                field.set(meta, gp);
            } catch (Exception ignored) {
            }
        });
        return this;
    }

    public ItemBuilder lore(String name) {
        metaModifiers.add(meta -> {
            List<String> lore = meta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.add(ChatColor.translateAlternateColorCodes('&', name));
            meta.setLore(lore);
        });
        return this;
    }

    public ItemBuilder lore(String... lore) {
        return lore(Arrays.asList(lore));
    }

    public ItemBuilder enchant(Enchantment enchantment, int i) {
        metaModifiers.add(meta -> meta.addEnchant(enchantment, i, true));
        return this;
    }

    public ItemBuilder customName(String customName) {
        return this.changeNbt("customName", customName);
    }

    public ItemBuilder prefix(String prefix) {
        return this.changeNbt("prefix", prefix);
    }

    public ItemBuilder lore(List<String> lore) {
        metaModifiers.add(meta -> {
            List<String> toSet = CC.translate(lore);
            meta.setLore(toSet);
        });
        return this;
    }

    public ItemBuilder durability(int durability) {
        metaModifiers.add(meta -> ((Damageable) meta).setDamage(durability));
        return this;
    }

    public ItemBuilder jewelSwordKills(int kills) {
        this.changeNbt("killed", kills);
        return this;
    }

    public ItemBuilder makeBoostedByGlobalGem(boolean boosted) {
        this.changeNbt("boostedByGlobalGem", boosted);
        return this;
    }

    public ItemBuilder makeBoostedByGem(boolean boosted) {
        this.changeNbt("boostedByGem", boosted);
        return this;
    }

    public ItemBuilder makeBoostedByEternal(boolean boosted) {
        this.changeNbt("boostedByEternal", boosted);
        return this;
    }

    public ItemBuilder makeBoostedByBook(boolean boosted) {
        this.changeNbt("boostedByBook", boosted);
        return this;
    }

    public ItemBuilder makeBoostedByPhaseBook(boolean boosted) {
        this.changeNbt("boostedByPhaseBook", boosted);
        return this;
    }

    public ItemBuilder recordEnchantments(List<EnchantmentRecord> records) {
        StringBuilder builder = new StringBuilder();

        if (records.size() > 5) {
            records = records.subList(records.size() - 5, records.size());
        }

        for (EnchantmentRecord record : records) {
            if (!builder.isEmpty()) {
                builder.append(";");
            }
            builder.append(record.getEnchanter())
                    .append("|")
                    .append(record.getDescription())
                    .append("|")
                    .append(record.getTimestamp());
        }

        this.changeNbt("records", builder.toString());
        return this;
    }

    public ItemBuilder enchantment(Enchantment enchantment, int level) {
        return enchant(enchantment, level);
    }

    public ItemBuilder enchantment(Enchantment enchantment) {
        return enchant(enchantment, 1);
    }

    public ItemBuilder shiny() {
        return this.enchant(Enchantment.UNBREAKING, 1).flags(ItemFlag.values());
    }

    public ItemBuilder shiny(boolean b) {
        if (b) return this.enchant(Enchantment.UNBREAKING, 1).flags(ItemFlag.values());
        metaModifiers.add(meta -> meta.removeEnchant(Enchantment.UNBREAKING));
        metaModifiers.add(meta -> meta.removeItemFlags(ItemFlag.values()));
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        metaModifiers.add(meta -> meta.addItemFlags(flags));
        return this;
    }

    public ItemBuilder type(Material material) {
        this.is.setType(material);
        return this;
    }

    public ItemBuilder clearLore() {
        metaModifiers.add(meta -> meta.setLore(new ArrayList<>()));
        return this;
    }

    public ItemBuilder clearEnchantments() {
        metaModifiers.add(ItemMeta::removeEnchantments);
        return this;
    }

    public ItemBuilder canDrop(boolean allow) {
        this.changeNbt("tradeAllow", allow);
        return this;
    }

    public ItemBuilder canTrash(boolean allow) {
        this.changeNbt("canTrash", allow);
        return this;
    }

    public ItemBuilder isHealingItem(boolean isHealingItem) {
        this.changeNbt("isHealingItem", isHealingItem);
        return this;
    }

    public ItemBuilder canTrade(boolean allow) {
        this.changeNbt("canTrade", allow);
        return this;
    }

    public ItemBuilder defaultItem() {
        this.changeNbt("defaultItem", true);
        return this;
    }

    public ItemBuilder defaultItem(boolean b) {
        this.changeNbt("defaultItem", b);
        return this;
    }

    public ItemBuilder canSaveToEnderChest(boolean allow) {
        this.changeNbt("enderChest", allow);
        return this;
    }

    public ItemBuilder changeNbt(String key, String value) {
        nbtModifiers.add(l -> l.setString(key, value));
        return this;
    }

    public ItemBuilder changeNbt(String key, boolean value) {
        nbtModifiers.add(l -> l.setBoolean(key, value));
        return this;
    }

    public ItemBuilder changeNbt(String key, Long vaule) {
        nbtModifiers.add(l -> l.setLong(key, vaule));
        return this;
    }

    public ItemBuilder changeNbt(String key, int value) {
        nbtModifiers.add(l -> l.setInteger(key, value));
        return this;
    }

    public ItemBuilder changeNbt(String key, double value) {
        nbtModifiers.add(l -> l.setDouble(key, value));
        return this;
    }

    public ItemStack buildWithUnbreakable() {
        metaModifiers.add(meta -> meta.setUnbreakable(true));
        return build().clone();
    }

    public ItemBuilder internalName(String name) {
        this.internalName = name;
        this.changeNbt("internal", name);
        metaModifiers.add(l -> l.getPersistentDataContainer().set(ItemUtil.KEY_INTERNAL_NAME, PersistentDataType.STRING, name));
        return this;
    }

    public ItemBuilder removeOnJoin(boolean remove) {
        this.changeNbt("removeOnJoin", remove);
        return this;
    }

    public ItemBuilder uuid(UUID uuid) {
        requestedUuid = uuid;
        return this;
    }

    public ItemStack build() {
        if (!nbtModifiers.isEmpty()) {
            NBT.modify(is, nbt -> {
                ReadWriteNBT extra = nbt.getOrCreateCompound("extra");
                nbtModifiers.forEach(modifier -> modifier.accept(extra));
                nbtMapModifiers.forEach((str, value) -> value.forEach(modifier -> modifier.accept(nbt.getOrCreateCompound("extra").getOrCreateCompound(str))));
            });
        }
        applyMeta(is);
        if (useRequestedUuid(is)) {
            AdvancedItemIdentity.writeUuid(is, requestedUuid);
        } else {
            AdvancedItemIdentity.ensureUuid(is);
        }
        return is;
    }

    private boolean useRequestedUuid(ItemStack stack) {
        if (requestedUuid == null || stack.getType().isAir()) return false;
        String id = ItemUtil.getInternalName(stack);
        return id == null || ItemSettings.uuidEnabled(id);
    }

    private void applyMeta(ItemStack stack) {
        if (metaModifiers.isEmpty()) return;
        stack.editMeta(meta -> {
            metaModifiers.forEach(modifier -> modifier.accept(meta));
            String override = composedName ? null : ItemSettings.displayName(internalName, null);
            if (override != null) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', override));
        });
    }

    public ItemBuilder deathDrop(boolean drop) {
        this.changeNbt("deathDrop", drop);
        return this;
    }

    public ItemBuilder enchant(Map<AbstractEnchantment, Integer> enchant) {
        nbtModifiers.add(l -> {
            ReadWriteNBTList<String> ench = l.getStringList("ench");
            enchant.forEach((it, level) -> {
                ench.add(it.getNbtName() + ":" + level);
                it.onBuild(this, level);
            });
        });
        return this;
    }

    public ItemBuilder maxLive(int live) {
        this.changeNbt("maxLive", live);
        return this;
    }

    public ItemBuilder dyeColor(String color) {
        this.changeNbt("dyeColor", color);
        return this;
    }

    public ItemBuilder tier(int tier) {
        this.changeNbt("tier", tier);
        return this;
    }

    public ItemBuilder live(int live) {
        this.changeNbt("live", live);
        return this;
    }

    public ItemBuilder itemDamage(double damageValue) {
        metaModifiers.add(meta -> {
            AttributeModifier modifier = new AttributeModifier(
                    UUID.randomUUID(),
                    "generic.attack_damage",
                    damageValue - 1.0,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlot.HAND
            );
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, modifier);
        });
        return this;
    }

    public ItemBuilder itemArmor(double armorValue) {
        metaModifiers.add(meta -> {
            meta.removeAttributeModifier(Attribute.GENERIC_ARMOR);
            EquipmentSlot slot;
            Material type = is.getType();
            if (type.name().endsWith("_HELMET")) {
                slot = EquipmentSlot.HEAD;
            } else if (type.name().endsWith("_CHESTPLATE")) {
                slot = EquipmentSlot.CHEST;
            } else if (type.name().endsWith("_LEGGINGS")) {
                slot = EquipmentSlot.LEGS;
            } else if (type.name().endsWith("_BOOTS")) {
                slot = EquipmentSlot.FEET;
            } else {
                slot = EquipmentSlot.LEGS;
            }
            AttributeModifier modifier = new AttributeModifier(
                    UUID.randomUUID(),
                    "generic.armor",
                    armorValue,
                    AttributeModifier.Operation.ADD_NUMBER,
                    slot
            );
            meta.addAttributeModifier(Attribute.GENERIC_ARMOR, modifier);
        });
        return this;
    }

    public ItemBuilder ArmorToughness(double toughnessValue) {
        metaModifiers.add(meta -> {
            meta.removeAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS);
            EquipmentSlot slot = getEquipmentSlot();
            AttributeModifier modifier = new AttributeModifier(
                    UUID.randomUUID(),
                    "generic.armor_toughness",
                    toughnessValue,
                    AttributeModifier.Operation.ADD_NUMBER,
                    slot
            );
            meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, modifier);
        });
        return this;
    }

    @NotNull
    private EquipmentSlot getEquipmentSlot() {
        Material type = is.getType();
        if (type.name().endsWith("_HELMET")) {
            return EquipmentSlot.HEAD;
        } else if (type.name().endsWith("_CHESTPLATE")) {
            return EquipmentSlot.CHEST;
        } else if (type.name().endsWith("_LEGGINGS")) {
            return EquipmentSlot.LEGS;
        } else if (type.name().endsWith("_BOOTS")) {
            return EquipmentSlot.FEET;
        } else {
            return EquipmentSlot.LEGS;
        }
    }

    public ItemBuilder addPotionEffect(PotionEffect effect, boolean b) {
        metaModifiers.add(meta -> {
            if (meta instanceof PotionMeta potionMeta) {
                potionMeta.addCustomEffect(effect, b);
            }
        });
        return this;
    }

    public ItemBuilder killCounterGem(int killCounterGem) {
        if (killCounterGem != -1) {
            this.changeNbt("killCounterGem", killCounterGem);
        }
        return this;
    }

    public void addAttributeModifier(Attribute attribute, AttributeModifier modifier) {
        metaModifiers.add(let -> let.addAttributeModifier(attribute, modifier));
    }

    public ItemBuilder newList(String name) {
        nbtMapModifiers.put(name, new ArrayList<>());
        return this;
    }

    public ItemBuilder putNbt(String mapName, String key, boolean value) {
        List<Consumer<ReadWriteNBT>> nbtList = nbtMapModifiers.get(mapName);
        if (nbtList == null) newList(mapName);
        nbtList = nbtMapModifiers.get(mapName);
        nbtList.add(nbt1 -> nbt1.setBoolean(key, value));
        return this;
    }

    public ItemBuilder putNbt(String mapName, Map<String, Boolean> map) {
        List<Consumer<ReadWriteNBT>> nbtList = nbtMapModifiers.get(mapName);
        if (nbtList == null) newList(mapName);
        nbtList = nbtMapModifiers.get(mapName);
        List<Consumer<ReadWriteNBT>> finalNbtList = nbtList;
        map.forEach((key, value) -> finalNbtList.add(nbt1 -> nbt1.setBoolean(key, value)));
        return this;
    }

    public ItemBuilder speed(double value) {
        metaModifiers.add(meta -> {
            meta.removeAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED);
            EquipmentSlot slot = getEquipmentSlot();
            AttributeModifier modifier = new AttributeModifier(
                    UUID.randomUUID(),
                    "generic.movement_speed",
                    value,
                    AttributeModifier.Operation.ADD_NUMBER,
                    slot
            );
            meta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, modifier);
        });
        return this;
    }

    public String getName() {
        ItemStack copy = is.clone();
        applyMeta(copy);
        return copy.getItemMeta().getDisplayName();
    }

    public List<String> getLore() {
        ItemStack copy = is.clone();
        copy.editMeta(meta -> metaModifiers.forEach(modifier -> modifier.accept(meta)));
        return copy.getItemMeta().getLore();
    }

    public boolean isShiny() {
        ItemStack copy = is.clone();
        copy.editMeta(meta -> metaModifiers.forEach(modifier -> modifier.accept(meta)));
        ItemMeta meta = copy.getItemMeta();
        return meta != null && meta.hasEnchant(Enchantment.UNBREAKING) && meta.getEnchantLevel(Enchantment.UNBREAKING) >= 1;
    }

    private ItemStack preview() {
        ItemStack copy = is.clone();
        if (!nbtModifiers.isEmpty()) {
            NBT.modify(copy, nbt -> {
                ReadWriteNBT extra = nbt.getOrCreateCompound("extra");
                nbtModifiers.forEach(modifier -> modifier.accept(extra));
                nbtMapModifiers.forEach((str, value) -> value.forEach(modifier -> modifier.accept(nbt.getOrCreateCompound("extra").getOrCreateCompound(str))));
            });
        }
        applyMeta(copy);
        return copy;
    }

    public Material getType() {
        return preview().getType();
    }

    public int getAmount() {
        return preview().getAmount();
    }

    public int getDurability() {
        ItemStack preview = preview();
        if (preview.getItemMeta() instanceof Damageable damageable) {
            return damageable.getDamage();
        }
        return 0;
    }

    public Map<Enchantment, Integer> getEnchantments() {
        ItemStack preview = preview();
        ItemMeta meta = preview.getItemMeta();
        return meta != null ? meta.getEnchants() : Collections.emptyMap();
    }

    public Set<ItemFlag> getItemFlags() {
        ItemStack preview = preview();
        ItemMeta meta = preview.getItemMeta();
        return meta != null ? meta.getItemFlags() : Collections.emptySet();
    }

    public boolean isUnbreakable() {
        ItemStack preview = preview();
        ItemMeta meta = preview.getItemMeta();
        return meta != null && meta.isUnbreakable();
    }

    public Color getLeatherColor() {
        ItemStack preview = preview();
        ItemMeta meta = preview.getItemMeta();
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            return leatherMeta.getColor();
        }
        return null;
    }

    public String getSkullOwner() {
        ItemStack preview = preview();
        ItemMeta meta = preview.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            return skullMeta.getOwner();
        }
        return null;
    }

    public Collection<PotionEffect> getPotionEffects() {
        ItemStack preview = preview();
        ItemMeta meta = preview.getItemMeta();
        if (meta instanceof PotionMeta potionMeta) {
            return potionMeta.getCustomEffects();
        }
        return Collections.emptyList();
    }

    public UUID getUuid() {
        ItemStack preview = preview();
        if (useRequestedUuid(preview)) return requestedUuid;
        return AdvancedItemIdentity.readUuid(preview);
    }

    public boolean isCanDrop() {
        try {
            ItemStack preview = preview();
            return NBT.get(preview, nbt -> {
                return nbt.getCompound("extra").getBoolean("tradeAllow");
            });
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

    public boolean isCanTrash() {
        try {
            ItemStack preview = preview();
            return NBT.get(preview, nbt -> {
                return nbt.getCompound("extra").getBoolean("canTrash");
            });
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

    public boolean isHealingItem() {
        try {
            ItemStack preview = preview();
            return NBT.get(preview, nbt -> {
                return nbt.getCompound("extra").getBoolean("isHealingItem");
            });
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

    public boolean isCanTrade() {
        try {
            ItemStack preview = preview();
            return NBT.get(preview, nbt -> {
                return nbt.getCompound("extra").getBoolean("canTrade");
            });
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

    public boolean isDefaultItem() {
        try {
            ItemStack preview = preview();
            return NBT.get(preview, nbt -> {
                return nbt.getCompound("extra").getBoolean("defaultItem");
            });
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

    public boolean isCanSaveToEnderChest() {
        try {
            ItemStack preview = preview();
            return NBT.get(preview, nbt -> {
                return nbt.getCompound("extra").getBoolean("enderChest");
            });
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

    public boolean isRemoveOnJoin() {
        try {
            ItemStack preview = preview();
            return NBT.get(preview, nbt -> {
                return nbt.getCompound("extra").getBoolean("removeOnJoin");
            });
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

    public boolean isDeathDrop() {
        try {
            ItemStack preview = preview();
            return NBT.get(preview, nbt -> {
                return nbt.getCompound("extra").getBoolean("deathDrop");
            });
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

    public int getMaxLive() {
        try {
            ItemStack preview = preview();
            return NBT.get(preview, nbt -> {
                return nbt.getCompound("extra").getInteger("maxLive");
            });
        } catch (Exception e) {
            log.error("", e);
            return 0;
        }
    }

    public String getDyeColor() {
        try {
            ItemStack preview = preview();
            return NBT.get(preview, nbt -> {
                return nbt.getCompound("extra").getString("dyeColor");
            });
        } catch (Exception e) {
            log.error("", e);
            return null;
        }
    }

    public int getTier() {
        try {
            ItemStack preview = preview();
            return NBT.get(preview, nbt -> {
                return nbt.getCompound("extra").getInteger("tier");
            });
        } catch (Exception e) {
            log.error("", e);
            return 0;
        }
    }

    public int getLive() {
        try {
            ItemStack preview = preview();
            return NBT.get(preview, nbt -> {
                return nbt.getCompound("extra").getInteger("live");
            });
        } catch (Exception e) {
            log.error("", e);
            return 0;
        }
    }

    public int getKillCounterGem() {
        try {
            ItemStack preview = preview();
            return NBT.get(preview, nbt -> {
                return nbt.getCompound("extra").getInteger("killCounterGem");
            });
        } catch (Exception e) {
            log.error("", e);
            return -1;
        }
    }

    public boolean isBoostedByGlobalGem() {
        try {
            ItemStack preview = preview();
            return NBT.get(preview, nbt -> {
                return nbt.getCompound("extra").getBoolean("boostedByGlobalGem");
            });
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

    public boolean isBoostedByGem() {
        try {
            ItemStack preview = preview();
            return NBT.get(preview, nbt -> {
                return nbt.getCompound("extra").getBoolean("boostedByGem");
            });
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

    public boolean isBoostedByEternal() {
        try {
            ItemStack preview = preview();
            return NBT.get(preview, nbt -> {
                return nbt.getCompound("extra").getBoolean("boostedByEternal");
            });
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

    public boolean isBoostedByBook() {
        try {
            ItemStack preview = preview();
            return NBT.get(preview, nbt -> {
                return nbt.getCompound("extra").getBoolean("boostedByBook");
            });
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

    public boolean isBoostedByPhaseBook() {
        try {
            ItemStack preview = preview();
            return NBT.get(preview, nbt -> {
                return nbt.getCompound("extra").getBoolean("boostedByPhaseBook");
            });
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

    public String getRecords() {
        try {
            ItemStack preview = preview();
            return NBT.get(preview, nbt -> {
                return nbt.getCompound("extra").getString("records");
            });
        } catch (Exception e) {
            log.error("", e);
            return null;
        }
    }


    public Multimap<Attribute, AttributeModifier> getAttributeModifiers() {
        ItemStack preview = preview();
        ItemMeta meta = preview.getItemMeta();
        return meta != null ? meta.getAttributeModifiers() : null;
    }

    public Collection<AttributeModifier> getAttributeModifiers(Attribute attribute) {
        ItemStack preview = preview();
        ItemMeta meta = preview.getItemMeta();
        return meta != null ? meta.getAttributeModifiers(attribute) : null;
    }
}
