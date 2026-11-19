package cn.charlotte.pit.util.inventory;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.perk.PerksBoostsEnum;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.util.MythicUtil;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.item.MythicOwnershipUtil;
import cn.charlotte.pit.util.random.RandomUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

/**
 * @Author: EmptyIrony
 * @Date: 2020/12/30 23:40
 */
public class InventoryUtil {
    private static final Random random = new Random();

    private static Field handle;

    static {
        try {
            handle = CraftItemStack.class.getDeclaredField("handle");
            handle.setAccessible(true);
        } catch (NoSuchFieldException ignored) {
        }
    }

    public static int getInventoryEmptySlots(ItemStack[] itemStacks) {
        int slot = 0;
        for (ItemStack itemStack : itemStacks) {
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                slot++;
            }
        }
        return slot;
    }

    public static int getInventoryFilledSlots(ItemStack[] itemStacks) {
        int slot = 0;
        for (ItemStack itemStack : itemStacks) {
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                slot++;
            }
        }
        return slot;
    }

    public static int getAmountOfItem(Player player, ItemStack item) {
        int amount = 0;
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getItem(i) != null && player.getInventory().getItem(i).isSimilar(item)) {
                amount += player.getInventory().getItem(i).getAmount();
            }
        }
        return amount;
    }

    public static int getAmountOfItem(Player player, String internalName) {
        int amount = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = player.getInventory().getItem(i);
            if (itemStack != null && ItemUtil.getInternalName(itemStack) != null && ItemUtil.getInternalName(itemStack).equals(internalName)) {
                amount += player.getInventory().getItem(i).getAmount();
            }
        }
        return amount;
    }

    public static void addItemOfAmount(Player player, ItemStack item, int count) {
        int amount = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = player.getInventory().getItem(i);
            if (itemStack != null && itemStack.isSimilar(item)) amount += player.getInventory().getItem(i).getAmount();
        }

        if (amount >= count) return;
        item.setAmount(item.getAmount() + amount > count ? count - amount : item.getAmount());
        player.getInventory().addItem(item);

    }

    public static boolean removeItem(Player player, ItemStack item, Integer amount) {
        if (getAmountOfItem(player, item) < amount) return false;
        int requirement = amount;
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getItem(i) != null && player.getInventory().getItem(i).isSimilar(item)) {
                if (player.getInventory().getItem(i).getAmount() <= requirement) {
                    requirement -= player.getInventory().getItem(i).getAmount();
                    player.getInventory().setItem(i, PitItems.AIR.toItemStack());
                } else {
                    player.getInventory().setItem(i, new ItemBuilder(item).amount(player.getInventory().getItem(i).getAmount() - requirement).build());
                    return true;
                }
            }
        }
        return requirement <= 0;
    }

    public static boolean removeItem(Player player, String internalName, Integer amount) {
        if (getAmountOfItem(player, internalName) < amount) return false;
        int requirement = amount;
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getItem(i) != null && internalName.equals(ItemUtil.getInternalName(player.getInventory().getItem(i)))) {
                if (player.getInventory().getItem(i).getAmount() <= requirement) {
                    requirement -= player.getInventory().getItem(i).getAmount();
                    player.getInventory().setItem(i, PitItems.AIR.toItemStack());
                } else {
                    player.getInventory().setItem(i, new ItemBuilder(player.getInventory().getItem(i)).amount(player.getInventory().getItem(i).getAmount() - requirement).build());
                    return true;
                }
            }
        }
        return requirement <= 0;
    }

    public static boolean removeItemWithUUID(Player player, UUID uuid) {
        if (uuid == null) {
            return false;
        }

        ItemStack cursorItem = player.getItemOnCursor();
        if (isMatchingMythicItem(cursorItem, uuid)) {
            player.setItemOnCursor(PitItems.AIR.toItemStack());
            return true;
        }

        ItemStack helmet = player.getInventory().getHelmet();
        if (isMatchingMythicItem(helmet, uuid)) {
            player.getInventory().setHelmet(PitItems.AIR.toItemStack());
            return true;
        }

        ItemStack chestplate = player.getInventory().getChestplate();
        if (isMatchingMythicItem(chestplate, uuid)) {
            player.getInventory().setChestplate(PitItems.AIR.toItemStack());
            return true;
        }

        ItemStack leggings = player.getInventory().getLeggings();
        if (isMatchingMythicItem(leggings, uuid)) {
            player.getInventory().setLeggings(PitItems.AIR.toItemStack());
            return true;
        }

        ItemStack boots = player.getInventory().getBoots();
        if (isMatchingMythicItem(boots, uuid)) {
            player.getInventory().setBoots(PitItems.AIR.toItemStack());
            return true;
        }


        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        if (isMatchingMythicItem(offHandItem, uuid)) {
            player.getInventory().setItemInOffHand(PitItems.AIR.toItemStack());
            return true;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack itemStack = contents[i];
            if (isMatchingMythicItem(itemStack, uuid)) {
                inventory.setItem(i, PitItems.AIR.toItemStack());
                return true;
            }
        }

        return false;
    }

    private static boolean isMatchingMythicItem(ItemStack itemStack, UUID uuid) {
        if (itemStack == null || itemStack.getType().equals(Material.AIR)) {
            return false;
        }

        IMythicItem mythicItem = MythicUtil.getMythicItem(itemStack);
        return mythicItem != null && uuid.equals(mythicItem.getUuid());
    }

    public static void removeItemWithInternalName(Player player, String name) {
        if (name == null) {
            return;
        }
        if (name.equals(ItemUtil.getInternalName(player.getItemOnCursor()))) {
            player.setItemOnCursor(PitItems.AIR.toItemStack());
        }

        if (name.equals(ItemUtil.getInternalName(player.getInventory().getHelmet()))) {
            player.getInventory().setHelmet(PitItems.AIR.toItemStack());
        }
        if (name.equals(ItemUtil.getInternalName(player.getInventory().getChestplate()))) {
            player.getInventory().setChestplate(PitItems.AIR.toItemStack());
        }
        if (name.equals(ItemUtil.getInternalName(player.getInventory().getLeggings()))) {
            player.getInventory().setLeggings(PitItems.AIR.toItemStack());
        }
        if (name.equals(ItemUtil.getInternalName(player.getInventory().getBoots()))) {
            player.getInventory().setBoots(PitItems.AIR.toItemStack());
        }
        if (name.equals(ItemUtil.getInternalName(player.getInventory().getItemInOffHand()))) {
            player.getInventory().setItemInOffHand(PitItems.AIR.toItemStack());
        }

        int index = 0;
        final PlayerInventory inventory = player.getInventory();
        final List<ItemStack> contents = new ArrayList<>(Arrays.asList(inventory.getContents()));
        for (ItemStack itemStack : contents) {
            if (name.equals(ItemUtil.getInternalName(itemStack))) {
                inventory.setItem(index, null);
            }

            index++;
        }
    }

    public static boolean hasItemWithInternalName(Player player, String name) {
        for (ItemStack itemStack : player.getInventory()) {
            String str = ItemUtil.getInternalName(itemStack);
            if (str != null && str.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public static IMythicItem getItemWithUUID(Player player, UUID uuid) {
        if (uuid == null) {
            return null;
        }
        List<ItemStack> itemStackList = new ArrayList<>();
        PlayerInventory inventory = player.getInventory();

        itemStackList.addAll(Arrays.asList(inventory.getContents()));

        itemStackList.addAll(Arrays.asList(inventory.getArmorContents()));

        itemStackList.add(inventory.getItemInOffHand());
        for (ItemStack item : itemStackList) {
            IMythicItem mythicItem = MythicUtil.getMythicItem(item);
            if (mythicItem != null && mythicItem.getUuid() == uuid) {
                return mythicItem;
            }
        }
        return null;
    }

    public static ItemStack[] fixInventoryOrder(ItemStack[] source) {
        ItemStack[] fixed = new ItemStack[36];

        System.arraycopy(source, 0, fixed, 27, 9);
        System.arraycopy(source, 9, fixed, 0, 27);

        return fixed;
    }

    public static PlayerInv playerInventoryFromPlayer(Player player) {
        final PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (profile.isTempInvUsing()) {
            return profile.getInventory();
        }

        MythicOwnershipUtil.bindInventory(player);

        PlayerInv inv = new PlayerInv();

        inv.setContents(player.getInventory().getContents());
        inv.setArmorContents(player.getInventory().getArmorContents());

        return inv;
    }

    public static String itemsToString(ItemStack[] items) {
        if (items == null) return "null";

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < items.length; ++i) {
            builder.append(i).append("#").append(serializeItemStack(items[i]))
                    .append((i == items.length - 1) ? "" : ";");
        }

        return builder.toString();
    }

    public static ItemStack[] stringToItems(String in) {
        if (in == null || in.equals("unset") || in.equals("null") || in.equals("'null'")) return null;
        ItemStack[] contents = new ItemStack[in.split(";").length];

        for (String s : in.split(";")) {
            int slot = Integer.parseInt(s.split("#")[0]);

            if (s.split("#").length == 1) {
                contents[slot] = null;
            } else {
                contents[slot] = deserializeItemStack(s.split("#")[1]);
            }
        }
        return contents;
    }

    public static String playerInvToString(PlayerInv inv) {
        if (inv == null) return "null";

        StringBuilder builder = new StringBuilder();
        ItemStack[] armor = inv.getArmorContents();
        if (armor == null) {
            armor = new ItemStack[4];
        }

        for (int i = 0; i < armor.length; i++) {
            if (i == 3) {
                if (armor[i] == null) {
                    builder.append(serializeItemStack(PitItems.AIR.toItemStack()));
                } else {
                    builder.append(serializeItemStack(armor[3]));
                }
            } else {
                if (armor[i] == null) {
                    builder.append(serializeItemStack(PitItems.AIR.toItemStack())).append(";");
                } else {
                    builder.append(serializeItemStack(armor[i])).append(";");
                }
            }
        }

        builder.append("|");

        if (inv.getContents() == null) {
            inv.setContents(new ItemStack[36]);
        }

        for (int i = 0; i < inv.getContents().length; ++i) {
            builder.append(i).append("#").append(serializeItemStack(inv.getContents()[i]))
                    .append((i == inv.getContents().length - 1) ? "" : ";");
        }

        return builder.toString();
    }

    public static String playerInventoryToString(org.bukkit.inventory.PlayerInventory inv) {
        StringBuilder builder = new StringBuilder();
        ItemStack[] armor = inv.getArmorContents();

        for (int i = 0; i < armor.length; i++) {
            if (i == 3) {
                if (armor[i] == null) {
                    builder.append(serializeItemStack(PitItems.AIR.toItemStack()));
                } else {
                    builder.append(serializeItemStack(armor[3]));
                }
            } else {
                if (armor[i] == null) {
                    builder.append(serializeItemStack(PitItems.AIR.toItemStack())).append(";");
                } else {
                    builder.append(serializeItemStack(armor[i])).append(";");
                }
            }
        }

        builder.append("|");

        for (int i = 0; i < inv.getContents().length; ++i) {
            builder.append(i).append("#").append(serializeItemStack(inv.getContents()[i]))
                    .append((i == inv.getContents().length - 1) ? "" : ";");
        }

        return builder.toString();
    }

    public static PlayerInv playerInventoryFromString(String in) {
        if (in == null || in.equals("unset") || in.equals("null") || in.equals("'null'")) return null;

        PlayerInv inv = new PlayerInv();

        String[] data = in.split("\\|");
        ItemStack[] armor = new ItemStack[data[0].split(";").length];

        for (int i = 0; i < data[0].split(";").length; ++i) {
            armor[i] = deserializeItemStack(data[0].split(";")[i]);
        }

        inv.setArmorContents(armor);
        ItemStack[] contents = new ItemStack[data[1].split(";").length];

        for (String s : data[1].split(";")) {
            int slot = Integer.parseInt(s.split("#")[0]);

            if (s.split("#").length == 1) {
                contents[slot] = null;
            } else {
                contents[slot] = deserializeItemStack(s.split("#")[1]);
            }
        }

        inv.setContents(contents);
        return inv;
    }

    public static String inventoryToString(Inventory inv) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < inv.getContents().length; ++i) {
            builder.append(i).append("#").append(serializeItemStack(inv.getContents()[i]));
            if (i != inv.getContents().length - 1) {
                builder.append(";");
            }
        }

        return builder.toString();
    }

    public static Inventory inventoryFromString(String in) {
        Inventory inv = Bukkit.createInventory(null, 54);
        String[] split;
        @SuppressWarnings("unused")
        String[] data = split = in.split(";");

        for (String s : split) {
            String[] info = s.split("#");
            inv.setItem(Integer.parseInt(info[0]), (info.length > 1) ? deserializeItemStack(info[1]) : null);
        }

        return inv;
    }

    public static String serializeItemStack(ItemStack input) {
        if (input == null || input.getType().equals(Material.AIR)) {
            return "";
        }
        ByteBuf buffer = Unpooled.buffer();
        MinecraftServer minecraftServer = ((CraftServer) Bukkit.getServer()).getServer();
        // 获取RegistryAccess实例
        RegistryAccess registryAccess = minecraftServer.registryAccess();
        RegistryFriendlyByteBuf registryFriendlyByteBuf = new RegistryFriendlyByteBuf(buffer, registryAccess);

        CraftItemStack item = input instanceof CraftItemStack ? ((CraftItemStack) input) : CraftItemStack.asCraftCopy(input);
        net.minecraft.world.item.ItemStack nms = null;
        try {
            nms = (net.minecraft.world.item.ItemStack) handle.get(item);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode(registryFriendlyByteBuf, nms);
        return java.util.Base64.getMimeEncoder().encodeToString(registryFriendlyByteBuf.array());

//        try {
//            return 2.getDefault().serializeItemStack(input);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }

    public static @Nullable ItemStack deserializeItemStack(String input) {
        if (input == null || input.isEmpty() || input.isBlank()) {
            return null;
        }
        byte[] decode = java.util.Base64.getMimeDecoder().decode(input);
        ByteBuf buf = Unpooled.wrappedBuffer(decode);

        MinecraftServer minecraftServer = ((CraftServer) Bukkit.getServer()).getServer();
        // 获取RegistryAccess实例
        RegistryAccess registryAccess = minecraftServer.registryAccess();
        return CraftItemStack.asCraftMirror(net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.decode(new RegistryFriendlyByteBuf(buf, registryAccess)));
    }

    /**
     * 检查玩家的背包（包括快捷栏）是否已完全占满
     */
    public static boolean isInvFull(@NotNull Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                return false;
            }
        }
        return true;
    }


    /**
     * 重构后的 supplyItems 方法：一次遍历玩家所有普通槽位 + 副手，
     * 单独再遍历盔甲槽，合并各种“移除”、“统计”、“记录是否有默认剑/默认弓”等逻辑。
     *
     * @param player 玩家对象
     */

    public static void supplyItems(Player player) {
        PlayerInventory inv = player.getInventory();
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        boolean hasBeastHelmet = false;
        boolean hasMinerPerk = profile.hasEquipPerk(PerksBoostsEnum.MINE_MAN.name());
        boolean hasBarbarianPerk = profile.hasEquipPerk(PerksBoostsEnum.BARBARIAN.name());
        int illegalCount = 0;

        illegalCount += sanitizeItem(inv::setItemInOffHand, inv.getItemInOffHand());

        boolean hasDefaultSword = false;
        boolean hasDefaultBow = false;
        List<ItemStack> toRemove = new ArrayList<>();

        for (ItemStack stack : inv.getContents()) {
            if (stack == null) continue;

            if ("beast_mode_helmet".equals(ItemUtil.getInternalName(stack))) {
                hasBeastHelmet = true;
            }

            if (mustDelete(stack)) {
                toRemove.add(stack);
                illegalCount += ItemUtil.isIllegalItem(stack) ? 1 : 0;
                continue;
            }

            switch (ItemUtil.getInternalName(stack)) {
                case "default_sword" -> hasDefaultSword = true;
                case "default_bow" -> hasDefaultBow = true;
                case null, default -> {
                }
            }
        }
        toRemove.forEach(inv::removeItemAnySlot);

        // remove cobblestone and arrows once
        inv.remove(Material.COBBLESTONE);
        inv.remove(Material.ARROW);

        if (hasMinerPerk) {
            inv.addItem(PitItems.COBBLESTONE.toManyItem(32));
        }
        if (hasBarbarianPerk) {
            InventoryUtil.removeItemWithInternalName(player, "perk_barbarian");
            player.getInventory().addItem(PitItems.PERK_BARBARIAN_IRONAXE.toItemStack());
        }
        illegalCount += sanitizeItem(inv::setHelmet, inv.getHelmet());
        illegalCount += sanitizeItem(inv::setChestplate, inv.getChestplate());
        illegalCount += sanitizeItem(inv::setLeggings, inv.getLeggings());
        illegalCount += sanitizeItem(inv::setBoots, inv.getBoots());

        if (illegalCount > 0) {
            player.sendMessage(CC.translate("&c我们从您的背包中找到 &e" + illegalCount + "&c 个异常物品，已从中移除，抱歉!"));
        }

        if (profile.getPlayerOption().isOutfit()) {
            clearDefaultArmor(inv);
            giveMissingDefaultWeapons(inv, hasDefaultSword, hasDefaultBow);
            giveDefaultArmor(inv);
        }

        int maxArrow = 32 + Math.max(0, PlayerUtil.getPlayerUnlockedPerkLevel(player, PerksPrestigeEnum.ARMORY) * 8);
        InventoryUtil.addInvReverse(inv, PitItems.DEFAULT_ARROW.toManyItem(maxArrow));

        if (hasBeastHelmet && !hasItemWithInternalName(player, PitItems.BEAST_MODE_HELMET_AWARD.getInternalName())) {
            if (inv.getHelmet() == null || inv.getHelmet().getType().equals(Material.AIR)) {
                inv.setHelmet(PitItems.BEAST_MODE_HELMET_AWARD.toItemStack());
            } else {
                inv.addItem(PitItems.BEAST_MODE_HELMET_AWARD.toItemStack());
            }
        }
    }

    private static int sanitizeItem(Consumer<ItemStack> slotSetter, @Nullable ItemStack stack) {
        if (stack == null) return 0;
        if (mustDelete(stack)) {
            slotSetter.accept(null);
            return ItemUtil.isIllegalItem(stack) ? 1 : 0;
        }
        return 0;
    }

    private static boolean mustDelete(ItemStack stack) {
        return ItemUtil.isIllegalItem(stack)
                || ItemUtil.isRemovedOnJoin(stack)
                || ItemUtil.isDefaultItem(stack)
                || stack.getType() == Material.ARROW
                || stack.getType() == Material.COBBLESTONE;
    }

    private static void clearDefaultArmor(PlayerInventory inv) {
        if (ItemUtil.isDefaultItem(inv.getHelmet())) inv.setHelmet(null);
        if (ItemUtil.isDefaultItem(inv.getChestplate())) inv.setChestplate(null);
        if (ItemUtil.isDefaultItem(inv.getLeggings())) inv.setLeggings(null);
        if (ItemUtil.isDefaultItem(inv.getBoots())) inv.setBoots(null);
    }

    private static void giveMissingDefaultWeapons(PlayerInventory inv, boolean hasSword, boolean hasBow) {
        if (!hasSword) {
            inv.addItem(new ItemBuilder(Material.IRON_SWORD)
                    .internalName("default_sword")
                    .defaultItem()
                    .canDrop(false)
                    .canSaveToEnderChest(false)
                    .buildWithUnbreakable());
        }
        if (!hasBow) {
            inv.addItem(new ItemBuilder(Material.BOW)
                    .internalName("default_bow")
                    .defaultItem()
                    .canDrop(false)
                    .canSaveToEnderChest(false)
                    .buildWithUnbreakable());
        }
    }

    private static void giveDefaultArmor(PlayerInventory inv) {
        int ironSlot = RandomUtil.random.nextInt(3);
        if (inv.getChestplate() == null) {
            inv.setChestplate(new ItemBuilder(ironSlot == 0 ? Material.IRON_CHESTPLATE : Material.CHAINMAIL_CHESTPLATE)
                    .defaultItem()
                    .internalName("default_armor")
                    .canDrop(false)
                    .canSaveToEnderChest(true)
                    .buildWithUnbreakable());
        }
        if (inv.getLeggings() == null) {
            inv.setLeggings(new ItemBuilder(ironSlot == 1 ? Material.IRON_LEGGINGS : Material.CHAINMAIL_LEGGINGS)
                    .defaultItem()
                    .internalName("default_armor")
                    .canDrop(false)
                    .canSaveToEnderChest(true)
                    .buildWithUnbreakable());
        }
        if (inv.getBoots() == null) {
            inv.setBoots(new ItemBuilder(ironSlot == 2 ? Material.IRON_BOOTS : Material.CHAINMAIL_BOOTS)
                    .defaultItem()
                    .internalName("default_armor")
                    .canDrop(false)
                    .canSaveToEnderChest(true)
                    .buildWithUnbreakable());
        }
    }

    public static EquipmentSlot getArmorSlot(Material material) {
        String name = material.name();
        if (name.contains("HELMET")) {
            return EquipmentSlot.HEAD;
        }
        if (name.contains("CHESTPLATE")) {
            return EquipmentSlot.CHEST;
        }
        if (name.contains("LEGGINGS")) {
            return EquipmentSlot.LEGS;
        }
        if (name.contains("BOOTS")) {
            return EquipmentSlot.FEET;
        }
        return null;
    }

    private static int firstPartial(PlayerInventory playerInv, ItemStack item) {
        ItemStack[] inventory = playerInv.getContents();
        ItemStack filteredItem = CraftItemStack.asCraftCopy(item);
        for (int i = 35; i > -1; i--) {
            ItemStack cItem = inventory[i];
            if (cItem != null && cItem.getAmount() < cItem.getMaxStackSize() && cItem.isSimilar(filteredItem)) {
                return i;
            }
        }
        return -1;
    }

    private static int firstEmpty(PlayerInventory playerInv) {
        ItemStack[] inventory = playerInv.getContents();
        for (int i = 35; i > -1; i--) {
            if (inventory[i] == null) {
                return i;
            }
        }
        return -1;
    }

    public static void addInvReverse(PlayerInventory inventory, ItemStack item) {
        while (true) {
            // Do we already have a stack of it?
            int firstPartial = firstPartial(inventory, item);

            // Drat! no partial stack
            if (firstPartial == -1) {
                // Find a free spot!
                int firstFree = firstEmpty(inventory);

                if (firstFree == -1) {
                    break;
                } else {
                    // More than a single stack!
                    if (item.getAmount() > inventory.getMaxStackSize()) {
                        CraftItemStack stack = CraftItemStack.asCraftCopy(item);
                        stack.setAmount(inventory.getMaxStackSize());
                        inventory.setItem(firstFree, stack);
                        item.setAmount(item.getAmount() - inventory.getMaxStackSize());
                    } else {
                        // Just store it
                        inventory.setItem(firstFree, item);
                        break;
                    }
                }
            } else {
                // So, apparently it might only partially fit, well lets do just that
                ItemStack partialItem = inventory.getItem(firstPartial);

                int amount = item.getAmount();
                int partialAmount = partialItem.getAmount();
                int maxAmount = partialItem.getMaxStackSize();

                // Check if it fully fits
                if (amount + partialAmount <= maxAmount) {
                    partialItem.setAmount(amount + partialAmount);
                    // To make sure the packet is sent to the client
                    inventory.setItem(firstPartial, partialItem);
                    break;
                }

                // It fits partially
                partialItem.setAmount(maxAmount);
                // To make sure the packet is sent to the client
                inventory.setItem(firstPartial, partialItem);
                item.setAmount(amount + partialAmount - maxAmount);
            }
        }
    }

    //去掉装备栏和副手的判断
    public static boolean isInvFull(PlayerInventory inventory) {
        for (int i = 0; i < 36; i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                return false;
            }
        }
        return true;
    }

    public static int getInventoryEmptySlots(PlayerInventory inventory) {
        int slot = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null || stack.getType() == Material.AIR) {
                slot++;
            }
        }
        return slot;
    }

    public static int matchByInternalName(Player player,String... internalName){
        int slot = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if(stack == null) continue;
            String str = ItemUtil.getInternalNameNotNull(stack);
            if(str.isBlank() || Util.matchesAny(str,internalName)) continue;
            slot++;
        }
        return slot;
    }
}
