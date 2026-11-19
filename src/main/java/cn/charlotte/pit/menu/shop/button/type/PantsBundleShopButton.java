package cn.charlotte.pit.menu.shop.button.type;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.MythicColor;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.menu.shop.button.AbstractShopButton;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.perk.PerksShopEnum;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.item.MythicOwnershipUtil;
import cn.charlotte.pit.util.log.ItemOperationJson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/3/25 18:32
 */
@AutoRegister
@Slf4j
public class PantsBundleShopButton extends AbstractShopButton implements Listener {


    public PantsBundleShopButton(boolean b) {
        super(b);
    }

    @Override
    public String getInternalName() {
        return "pants_bundle";
    }

    @Override
    public ItemStack getDisplayButtonItem(Player player) {
        List<String> lines = new ArrayList<>();
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        lines.add("&7死亡后保留");
        lines.add(" ");
        lines.add("&7手持右键即可将背包内");
        lines.add("&710件未附魔的神话之甲打包.");
        lines.add(" ");
        lines.add("&7价格: &6" + getDiscountPrice(player, getPrice(player)) + " 硬币");

        if (profile.getCoins() >= getDiscountPrice(player, getPrice(player))) {
            lines.add("&e点击购买!");
        } else {
            lines.add("&c硬币不足!");
        }

        int data = profile.getLevel(PerksShopEnum.PANTS_BUNDLE);
        if (data > 0) {
            return new ItemBuilder(Material.MINECART)
                    .name("&b神话之甲收纳箱")
                    .lore(lines)
                    .build();
        }
        return new ItemBuilder(Material.AIR)
                .build();
    }

    @Override
    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return true;
    }

    @Override
    public ItemStack[] getResultItem(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7死亡后保留");
        lines.add(" ");
        lines.add("&7手持右键即可将背包内");
        lines.add("&710件未附魔的神话之甲打包.");
        return new ItemStack[]{
                new ItemBuilder(Material.MINECART)
                        .name("&b神话之甲收纳箱")
                        .lore(lines)
                        .canSaveToEnderChest(true)
                        .internalName("pants_bundle_empty")
                        .build(),
        };
    }

    @Override
    public int getPrice(Player player) {
        return 50;
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() == Action.PHYSICAL) {
            e.setCancelled(true);
            return;
        }
        Player player = e.getPlayer();
        if (e.getClickedBlock() != null && (e.getClickedBlock().getType() == Material.CRAFTING_TABLE
                || e.getClickedBlock().getType() == Material.ANVIL || e.getClickedBlock().getType() == Material.CHEST || e.getClickedBlock().getType() == Material.TRAPPED_CHEST)) {
            e.setCancelled(true);
            return;
        }
        ItemStack item = player.getItemInHand();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        if (ItemUtil.getInternalName(item) != null && "pants_bundle_full".equals(ItemUtil.getInternalName(item))) {
            int emptySlots = 0;
            for (int i = 0; i < 36; i++) {
                if (player.getInventory().getItem(i) == null || player.getInventory().getItem(i).getType() == Material.AIR) {
                    emptySlots++;
                    if (emptySlots >= 10) break;
                }
            }
            if (emptySlots < 10) {
                player.sendMessage(CC.translate("&c背包空间不足以取出全部神话之甲!"));
                return;
            }
            player.setItemInHand(new ItemBuilder(Material.AIR).build());
            List<ItemStack> rewards = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                ItemStack itemStack = new MythicLeggingsItem().toItemStack();
                itemStack = new ItemBuilder(itemStack).changeNbt("mythic_color", ItemUtil.getItemStringData(item, "pants_" + i)).build();
                AbstractPitItem mythicItem = new MythicLeggingsItem();
                mythicItem.loadFromItemStack(itemStack);
                ItemStack reward = mythicItem.toItemStack();
                rewards.add(reward);
                player.getInventory().addItem(reward);
            }
            JsonObject operationLog = ItemOperationJson.operation("OPEN_BUNDLE", player);
            operationLog.addProperty("bundle", "pants_bundle_full");
            operationLog.add("items", ItemOperationJson.grantedItems(player, rewards));
            log.info("{}", ItemOperationJson.toJson(operationLog));
        }
        if ("pants_bundle_empty".equals(ItemUtil.getInternalName(item))) {
            int mythicCount = 0;
            for (int i = 0; i < 36; i++) {
                ItemStack itemStack = player.getInventory().getItem(i);
                if (itemStack != null && "mythic_leggings".equalsIgnoreCase(ItemUtil.getInternalName(itemStack))) {
                    AbstractPitItem pitItem = new MythicLeggingsItem();
                    pitItem.loadFromItemStack(itemStack);
                    if (!pitItem.isEnchanted() && !MythicOwnershipUtil.isTraded(itemStack)) {
                        mythicCount++;
                        if (mythicCount >= 10) break;
                    }
                }
            }
            if (mythicCount < 10) {
                player.sendMessage(CC.translate("&c你的背包内需要有10条及以上未附魔且从未交易的神话之甲才能使用此物品!"));
            } else {
                int count = 0;
                List<ItemStack> packedItems = new ArrayList<>();
                ItemBuilder itemBuilder = new ItemBuilder(Material.CHEST_MINECART)
                        .name("&b神话之甲收纳箱")
                        .canSaveToEnderChest(true)
                        .canTrade(false)
                        .internalName("pants_bundle_full");
                HashMap<MythicColor, Integer> colorCount = new HashMap<>();
                try {
                    for (int i = 0; i < 36; i++) {
                        ItemStack itemStack = player.getInventory().getItem(i);
                        if (itemStack != null && "mythic_leggings".equalsIgnoreCase(ItemUtil.getInternalName(itemStack))) {
                            AbstractPitItem pitItem = new MythicLeggingsItem();
                            pitItem.loadFromItemStack(itemStack);
                            if (!pitItem.isEnchanted() && !MythicOwnershipUtil.isTraded(itemStack)) {
                                packedItems.add(itemStack.clone());
                                String internalColor = ItemUtil.getItemStringData(itemStack, "mythic_color");
                                itemBuilder.changeNbt("pants_" + count, ItemUtil.getItemStringData(itemStack, "mythic_color"));
                                player.getInventory().setItem(i, new ItemBuilder(Material.AIR).build());
                                colorCount.put(MythicColor.valueOfInternalName(internalColor), colorCount.getOrDefault(MythicColor.valueOfInternalName(internalColor), 0) + 1);
                                count++;
                                if (count >= 10) break;
                            }
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                    return;
                }
                List<String> lines = new ArrayList<>();
                lines.add("&7死亡后保留");
                lines.add("&7这个收纳箱里装有:");
                lines.add(" ");
                for (MythicColor color : colorCount.keySet()) {
                    lines.add("&f" + colorCount.get(color) + "x " + color.getChatColor() + color.getDisplayName() + "色神话之甲");
                }
                lines.add(" ");
                lines.add("&7手持并右键以将其取出!");

                itemBuilder.lore(lines);
                itemBuilder.internalName("pants_bundle_full");
                player.setItemInHand(itemBuilder.build());

                JsonObject operationLog = ItemOperationJson.operation("PACK_BUNDLE", player);
                operationLog.addProperty("bundle", "pants_bundle_full");
                operationLog.add("items", ItemOperationJson.items(packedItems));
                log.info("{}", ItemOperationJson.toJson(operationLog));
            }
        }


    }
}
