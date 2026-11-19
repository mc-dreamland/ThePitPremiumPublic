package cn.charlotte.pit.menu.mystery.button;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitToMysteryBuildEvent;
import cn.charlotte.pit.menu.mystery.MysteryBuildData;
import cn.charlotte.pit.menu.mystery.MysteryMenu;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.log.ItemOperationJson;
import cn.charlotte.pit.util.menu.Button;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MysteryBuildButton extends Button {

    private static final double BUILD_COST = 4000.0;
    private static final Set<UUID> PROCESSING_PLAYERS = ConcurrentHashMap.newKeySet();

    private static final ItemStack NO = new ItemBuilder(Material.ENDER_PEARL)
            .name("&e进行铸造")
            .lore(
                    "",
                    "&f将物品与物品进行铸造",
                    "&c请确保你拥有足够的材料或者足够的硬币",
                    ""
            )
            .build();

    private static final ItemStack YES = new ItemBuilder(Material.ENDER_EYE)
            .name("&e进行铸造")
            .lore(
                    "",
                    "&f将物品与物品进行铸造",
                    "&7价格:&6 4000 硬币",
                    "&7(部分物品要求消耗额外的物品)",
                    ""
            )
            .build();


    @Override
    public ItemStack getButtonItem(Player player) {
        PlayerProfile profile = Util.getPitProfile(player);
        if (profile.getMysteryMainItem() == null || profile.getMysteryOffItem() == null || profile.getMysteryMainItem().isEmpty() || profile.getMysteryOffItem().isEmpty() || profile.getCoins() < BUILD_COST)
            return NO;
        return YES;
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        if (!PROCESSING_PLAYERS.add(player.getUniqueId())) {
            CC.send(MessageType.MISC, player, "&c上一次造物操作仍在处理中，请稍候。");
            return;
        }

        try {
            build(player);
        } finally {
            PROCESSING_PLAYERS.remove(player.getUniqueId());
        }
    }

    private void build(Player player) {
        PlayerProfile profile = Util.getPitProfile(player);
        if (!profile.isLoaded()) {
            CC.send(MessageType.MISC, player, "&c玩家数据尚未加载完成，请稍后重试。");
            return;
        }
        String originalMain = profile.getMysteryMainItem();
        String originalOff = profile.getMysteryOffItem();
        if (Util.isEmpty(originalMain) || Util.isEmpty(originalOff)) return;
        if (profile.getCoins() < BUILD_COST) {
            CC.send(player, "&c金币不足");
            return;
        }

        ItemStack mainItem = InventoryUtil.deserializeItemStack(originalMain);
        ItemStack offItem = InventoryUtil.deserializeItemStack(originalOff);
        if (mainItem == null || mainItem.getType().isAir() || offItem == null || offItem.getType().isAir()) {
            CC.send(MessageType.MISC, player, "&c造物台中的物品数据无效，请将物品取出后重试。");
            return;
        }

        MysteryBuildData buildData = new MysteryBuildData();
        PitToMysteryBuildEvent event = new PitToMysteryBuildEvent(player, profile, mainItem, offItem, buildData);
        event.callEvent();
        MysteryBuildData data = event.getData();
        if (!data.isHandled()) {
            CC.send(MessageType.MISC, player, "&c所选的物品无法进行造物。");
            return;
        }
        if (data.isCanceled()) {
            CC.send(MessageType.MISC, player, data.getCancelMessage());
            return;
        }

        int remainingMaterial = data.getDelCount();
        if (remainingMaterial < 0 || remainingMaterial > offItem.getAmount()) {
            log.error("Invalid mystery build material result: operationId={}, player={}, before={}, remaining={}",
                    data.getOperationId(), player.getUniqueId(), offItem.getAmount(), remainingMaterial);
            CC.send(MessageType.MISC, player, "&c造物结果异常，操作已取消。");
            return;
        }

        profile.setMysteryMainItem(serializeNullable(data.getReturnItemStack()));
        profile.setMysteryOffItem(serializeRemaining(offItem, remainingMaterial));
        profile.takeCoins(BUILD_COST);

        JsonObject operationLog = ItemOperationJson.operation("MYSTERY_BUILD", player);
        operationLog.addProperty("operationId", data.getOperationId());
        operationLog.add("input", ItemOperationJson.item(mainItem));
        operationLog.add("material", ItemOperationJson.item(offItem));
        operationLog.addProperty("materialConsumed", offItem.getAmount() - remainingMaterial);
        operationLog.addProperty("coinsConsumed", BUILD_COST);
        operationLog.add("result", ItemOperationJson.item(data.getReturnItemStack()));

        try {
            data.runPostCommitAction();
        } catch (Exception exception) {
            operationLog.addProperty("status", "REWARD_FAILED_IN_MEMORY");
            operationLog.addProperty("delivery", data.getDelivery());
            log.info("{}", ItemOperationJson.toJson(operationLog));
            log.error("Failed to grant mystery build reward {}", data.getOperationId(), exception);
            CC.send(MessageType.MISC, player, "&c造物奖励发放失败，请联系管理员并提供操作编号: &f" + data.getOperationId());
            new MysteryMenu().openMenu(player);
            return;
        }

        operationLog.addProperty("status", "SUCCESS");
        operationLog.addProperty("delivery", data.getDelivery());
        log.info("{}", ItemOperationJson.toJson(operationLog));
        CC.send(MessageType.MISC, player, data.getReturnMessage());
        new MysteryMenu().openMenu(player);
    }

    private String serializeNullable(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return null;
        }
        String serialized = InventoryUtil.serializeItemStack(itemStack);
        return serialized == null || serialized.isEmpty() ? null : serialized;
    }

    private String serializeRemaining(ItemStack itemStack, int amount) {
        if (amount <= 0) {
            return null;
        }
        return InventoryUtil.serializeItemStack(new ItemBuilder(itemStack.clone()).amount(amount).build());
    }
}
