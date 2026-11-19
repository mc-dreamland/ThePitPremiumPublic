package cn.charlotte.pit.item.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.item.type.mythic.MythicBowItem;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.item.type.mythic.MythicSwordItem;
import cn.charlotte.pit.lambda.playerLambda;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.RandomList;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.log.ItemOperationJson;
import cn.charlotte.pit.util.random.RandomUtil;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

@AutoRegister
@Slf4j
public class ActionGiftBox extends AbstractPitItem implements Listener {

    private final RandomList<playerLambda> randomList = new RandomList<>(
            Map.entry(player -> {
                int amount = RandomUtil.random.nextInt(1500) + 500;
                PlayerProfile profile = Util.getPitProfile(player);
                profile.setExperience(profile.getExperience() + amount);
                profile.applyExperienceToPlayer(player);
                player.sendMessage(CC.translate(String.format("&e行动礼盒! &7你通过行动礼盒开出了. &b%d经验", amount)));
                logGift(player, "EXPERIENCE", amount);
            }, 300),

            Map.entry(player -> {
                int amount = RandomUtil.random.nextInt(1500) + 500;
                PlayerProfile profile = Util.getPitProfile(player);
                profile.setCoins(profile.getCoins() + amount);
                profile.grindCoins(amount);
                player.sendMessage(CC.translate(String.format("&e行动礼盒! &7你通过行动礼盒开出了. &e%d金币", amount)));
                logGift(player, "COINS", amount);
            }, 300),

            Map.entry(player -> {
                ItemStack reward = new ItemBuilder(Material.DIAMOND_LEGGINGS)
                                .deathDrop(true)
                                .canSaveToEnderChest(true)
                                .canTrade(true)
                                .internalName("shopItem")
                                .buildWithUnbreakable();
                player.getInventory().addItem(reward);
                player.sendMessage(CC.translate("&e行动礼盒! &7你通过行动礼盒开出了. &f钻石裤子"));
                logGift(player, "ITEM", 1, reward);
            }, 100),

            Map.entry(player -> {
                ItemStack boots = new ItemBuilder(Material.DIAMOND_BOOTS)
                                .deathDrop(true)
                                .canSaveToEnderChest(true)
                                .canTrade(true)
                                .internalName("shopItem")
                                .buildWithUnbreakable();
                ItemStack chestplate = new ItemBuilder(Material.DIAMOND_CHESTPLATE)
                                .deathDrop(true)
                                .canSaveToEnderChest(true)
                                .canTrade(true)
                                .internalName("shopItem")
                                .buildWithUnbreakable();
                player.getInventory().addItem(boots, chestplate);
                player.sendMessage(CC.translate("&e行动礼盒! &7你通过行动礼盒开出了. &f半套钻石套装"));
                logGift(player, "ITEM", 2, boots, chestplate);
            }, 200),

            Map.entry(player -> {
                ItemStack reward = new ItemBuilder(Material.DIAMOND_HELMET)
                                .canTrade(true)
                                .canSaveToEnderChest(true)
                                .deathDrop(true)
                                .lore("&e行动礼盒获得")
                                .internalName("dice_award")
                                .buildWithUnbreakable();
                player.getInventory().addItem(reward);
                player.sendMessage(CC.translate("&e行动礼盒! &7你通过行动礼盒开出了. &e行动头盔"));
                logGift(player, "ITEM", 1, reward);
            }, 45),

            Map.entry(player -> {
                ItemStack mythic = RandomUtil.helpMeToChooseOne(
                        new MythicSwordItem().toItemStack(),
                        new MythicBowItem().toItemStack(),
                        new MythicLeggingsItem().toItemStack()
                );
                if (mythic != null) {
                    player.getInventory().addItem(mythic);
                    logGift(player, "RANDOM_MYTHIC", 1, mythic);
                }
                player.sendMessage(CC.translate("&e行动礼盒! &7你通过行动礼盒开出了. &d随机的神话物品"));
            }, 45),

            Map.entry(player -> {
                ItemStack reward = PitItems.CHUNK_OF_VILE.toItemStack();
                player.getInventory().addItem(reward);
                CC.boardCast(String.format("&e恭喜 %s &f从行动礼盒中开出&5暗聚块", Util.getPitProfile(player).getFormattedName()));
                logGift(player, "ITEM", 1, reward);
            }, 10)
    );

    private static void logGift(Player player, String reward, Number rewardAmount, ItemStack... items) {
        JsonObject operationLog = ItemOperationJson.operation("OPEN_GIFT", player);
        operationLog.addProperty("gift", "Xing_Dong_Lihe");
        operationLog.addProperty("reward", reward);
        operationLog.addProperty("rewardAmount", rewardAmount);
        operationLog.add("items", ItemOperationJson.grantedItems(player, java.util.Arrays.asList(items)));
        log.info("{}", ItemOperationJson.toJson(operationLog));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(BlockPlaceEvent event) {
        ItemStack itemInHand = event.getItemInHand();
        EquipmentSlot hand = event.getHand();
        if (!getInternalName().equals(ItemUtil.getInternalName(itemInHand))) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (InventoryUtil.isInvFull(player.getInventory())) {
            player.sendMessage(CC.translate("&c你的背包是满的, 无法使用物品"));
            return;
        }

        PlayerUtil.takeOneItem(player, hand);

        final float[] ticks = {0.3f};
        new BukkitRunnable() {
            @Override
            public void run() {
                player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1f, ticks[0]);
                ticks[0] += 0.1f;
                if (ticks[0] >= 2.0f) cancel();
            }
        }.runTaskTimer(ThePit.getInstance(), 0L, 2L);

        try {
            randomList.random().execute(player);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    public String getInternalName() {
        return "Xing_Dong_Lihe";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&e行动礼盒";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.CHEST;
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(getItemDisplayMaterial())
                .internalName("Xing_Dong_Lihe")
                .name(getItemDisplayName())
                .lore(
                        "&7死亡时保留",
                        "",
                        "&e拿着并右键获得物品!"
                )
                .dontStack()
                .canTrade(true)
                .canSaveToEnderChest(true)
                .build();
    }

    @Override
    public void loadFromItemStack(ItemStack item) {
        // no-op
    }
}
