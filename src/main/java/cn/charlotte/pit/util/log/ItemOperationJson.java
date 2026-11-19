package cn.charlotte.pit.util.log;

import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.util.MythicUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.item.MythicOwnershipUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public final class ItemOperationJson {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private ItemOperationJson() {
    }

    public static JsonObject operation(String action, Player player) {
        JsonObject json = new JsonObject();
        json.addProperty("action", action);
        json.addProperty("playerUuid", player.getUniqueId().toString());
        json.addProperty("playerName", player.getName());
        return json;
    }

    public static JsonObject mythicGranted(Player player, String source, ItemStack... itemStacks) {
        MythicOwnershipUtil.bindInitial(java.util.Arrays.asList(itemStacks), player);
        JsonObject json = operation("GET_MYTHIC", player);
        json.addProperty("source", source);
        json.add("items", items(java.util.Arrays.asList(itemStacks)));
        return json;
    }

    public static JsonObject item(ItemStack itemStack) {
        JsonObject json = new JsonObject();
        if (itemStack == null || itemStack.getType().isAir()) {
            return json;
        }

        json.addProperty("name", getDisplayName(itemStack));
        json.addProperty("material", itemStack.getType().name());
        json.addProperty("amount", itemStack.getAmount());

        try {
            json.addProperty("uuid", ItemUtil.getUUID(itemStack));
            String internalName = ItemUtil.getInternalName(itemStack);
            json.addProperty("internalName", internalName);

            if ("phase_book".equals(internalName)) {
                json.addProperty("targetEnchant", ItemUtil.getItemStringData(itemStack, "phase_book"));
                String probability = ItemUtil.getItemStringData(itemStack, "probability");
                if (probability != null && !probability.isBlank()) {
                    try {
                        json.addProperty("probability", Double.parseDouble(probability));
                    } catch (NumberFormatException ignored) {
                        json.addProperty("probability", probability);
                    }
                }
            } else if ("lucky_stone".equals(internalName)) {
                json.addProperty("luckyBonus", ItemUtil.readItemDoubleData(itemStack, "lucky"));
            }

            if (MythicOwnershipUtil.isEquipment(itemStack)) {
                json.addProperty("creator", MythicOwnershipUtil.getCreator(itemStack));
                json.addProperty("creatorName", MythicOwnershipUtil.getCreatorName(itemStack));
                json.addProperty("owner", MythicOwnershipUtil.getOwner(itemStack));
                json.addProperty("ownerName", MythicOwnershipUtil.getOwnerName(itemStack));
                json.addProperty("traded", MythicOwnershipUtil.isTraded(itemStack));
                long tradeTime = MythicOwnershipUtil.getTradeTime(itemStack);
                if (tradeTime > 0L) {
                    json.addProperty("tradeTime", tradeTime);
                }
            }

            IMythicItem mythicItem = MythicUtil.getMythicItem(itemStack);
            if (mythicItem == null) {
                return json;
            }

            json.addProperty("color", mythicItem.getColor() == null ? null : mythicItem.getColor().name());
            json.addProperty("tier", mythicItem.getTier());
            json.addProperty("lives", mythicItem.getLive());
            json.addProperty("maxLives", mythicItem.getMaxLive());
            json.add("enchants", enchantments(mythicItem));
        } catch (Exception ignored) {
            return json;
        }
        return json;
    }

    public static JsonArray items(Collection<ItemStack> itemStacks) {
        JsonArray json = new JsonArray();
        if (itemStacks == null) {
            return json;
        }
        for (ItemStack itemStack : itemStacks) {
            if (itemStack != null && !itemStack.getType().isAir()) {
                json.add(item(itemStack));
            }
        }
        return json;
    }

    public static JsonArray grantedItems(Player player, Collection<ItemStack> itemStacks) {
        MythicOwnershipUtil.bindInitial(itemStacks, player);
        return items(itemStacks);
    }

    public static JsonArray textValues(Collection<String> values) {
        JsonArray json = new JsonArray();
        if (values == null) {
            return json;
        }
        for (String value : values) {
            if (value != null) {
                json.add(CC.stripColor(CC.translate(value)));
            }
        }
        return json;
    }

    public static JsonObject enchantResult(int tierBefore,
                                           Map<AbstractEnchantment, Integer> beforeEnchantments,
                                           IMythicItem afterItem) {
        JsonObject result = new JsonObject();
        result.addProperty("tierBefore", tierBefore);
        result.addProperty("tierAfter", afterItem.getTier());

        Map<String, Integer> before = enchantmentLevels(beforeEnchantments);
        Map<String, Integer> after = enchantmentLevels(afterItem);
        JsonObject added = new JsonObject();
        JsonObject upgraded = new JsonObject();

        for (Map.Entry<String, Integer> entry : after.entrySet()) {
            Integer oldLevel = before.get(entry.getKey());
            if (oldLevel == null) {
                added.addProperty(entry.getKey(), entry.getValue());
            } else if (!oldLevel.equals(entry.getValue())) {
                upgraded.add(entry.getKey(), levelChange(oldLevel, entry.getValue()));
            }
        }
        for (Map.Entry<String, Integer> entry : before.entrySet()) {
            if (!after.containsKey(entry.getKey())) {
                upgraded.add(entry.getKey(), levelChange(entry.getValue(), 0));
            }
        }

        result.add("added", added);
        result.add("upgraded", upgraded);
        return result;
    }

    public static boolean isBlankMythic(ItemStack itemStack) {
        try {
            IMythicItem mythicItem = MythicUtil.getMythicItem(itemStack);
            return mythicItem != null && mythicItem.getTier() == 0 && mythicItem.getEnchantments().isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }

    public static String toJson(JsonObject json) {
        return GSON.toJson(json);
    }

    private static JsonObject enchantments(IMythicItem mythicItem) {
        JsonObject json = new JsonObject();
        enchantmentLevels(mythicItem).forEach(json::addProperty);
        return json;
    }

    private static Map<String, Integer> enchantmentLevels(IMythicItem mythicItem) {
        return enchantmentLevels(mythicItem.getEnchantments());
    }

    private static Map<String, Integer> enchantmentLevels(Map<AbstractEnchantment, Integer> enchantments) {
        Map<String, Integer> levels = new TreeMap<>();
        for (Map.Entry<AbstractEnchantment, Integer> entry : enchantments.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                levels.put(entry.getKey().getNbtName(), entry.getValue());
            }
        }
        return levels;
    }

    private static JsonObject levelChange(int before, int after) {
        JsonObject json = new JsonObject();
        json.addProperty("before", before);
        json.addProperty("after", after);
        return json;
    }

    private static String getDisplayName(ItemStack itemStack) {
        if (!itemStack.hasItemMeta() || !itemStack.getItemMeta().hasDisplayName()) {
            return itemStack.getType().name();
        }
        return CC.stripColor(CC.translate(itemStack.getItemMeta().getDisplayName()));
    }
}
